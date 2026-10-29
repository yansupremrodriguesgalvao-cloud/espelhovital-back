// Apply only reviewed text files; refuse to overwrite any file edited since preparation.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const assert = require('assert/strict');
const manifestPath = path.resolve('target/frontend-changes.json');
const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
const source = path.resolve(manifest.source);
const stage = path.resolve(manifest.stage);
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
function files(directory) {
    return fs.readdirSync(directory, {withFileTypes: true}).flatMap(entry => {
        const file = path.join(directory, entry.name);
        return entry.isDirectory() ? files(file) : [file];
    });
}
const changed = files(stage).map(file => path.relative(stage, file)).filter(relative => {
    return manifest.originals[relative] !== hash(path.join(stage, relative));
});
// Validate every destination before performing any write.
for (const relative of changed) {
    const destination = path.resolve(source, relative);
    assert.ok(destination.startsWith(source + path.sep), 'Destination outside front-end directory');
    assert.ok(/\.(html|js|css)$/.test(relative), 'Only front-end text files are allowed');
    if (manifest.originals[relative]) assert.equal(hash(destination), manifest.originals[relative], 'File edited since review: ' + relative);
    else assert.ok(!fs.existsSync(destination), 'New file already exists: ' + relative);
}
const backup = path.resolve('target/frontend-backup-' + Date.now());
for (const relative of changed) {
    const destination = path.join(source, relative);
    if (fs.existsSync(destination)) {
        const saved = path.join(backup, relative);
        fs.mkdirSync(path.dirname(saved), {recursive: true});
        fs.copyFileSync(destination, saved);
    }
    fs.mkdirSync(path.dirname(destination), {recursive: true});
    fs.copyFileSync(path.join(stage, relative), destination);
    assert.equal(hash(destination), hash(path.join(stage, relative)));
}
fs.writeFileSync('target/frontend-applied.json', JSON.stringify({source, backup, changed, appliedAt: new Date().toISOString()}, null, 2));
console.log('Applied ' + changed.length + ' front-end files. Backup: ' + backup);
