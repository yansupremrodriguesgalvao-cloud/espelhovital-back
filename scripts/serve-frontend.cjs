const fs = require("node:fs");
const http = require("node:http");
const path = require("node:path");

const root = fs.realpathSync(
  process.env.FRONTEND_DIR ||
    "C:/Users/Valdiney/Documents/workspace/EspelhoVital",
);
const port = Number(process.env.FRONTEND_PORT || 5500);
const types = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".svg": "image/svg+xml",
  ".webp": "image/webp",
  ".ico": "image/x-icon",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
  ".ttf": "font/ttf",
  ".bin": "application/octet-stream",
};

const server = http.createServer((request, response) => {
  if (!["GET", "HEAD"].includes(request.method)) {
    response.writeHead(405, { Allow: "GET, HEAD" });
    response.end();
    return;
  }
  try {
    const url = new URL(request.url, "http://localhost");
    const relative = decodeURIComponent(url.pathname).replace(/^[/\\]+/, "");
    const file = path.resolve(root, relative || "index.html");
    if (
      !file.startsWith(root + path.sep) ||
      relative.split(/[/\\]/).some((part) => part.startsWith(".")) ||
      !Object.hasOwn(types, path.extname(file).toLowerCase())
    ) {
      response.writeHead(403);
      response.end("Acesso não permitido.");
      return;
    }
    const realFile = fs.realpathSync(file);
    if (!realFile.startsWith(root + path.sep) || !fs.statSync(realFile).isFile()) {
      response.writeHead(403);
      response.end();
      return;
    }
    response.writeHead(200, {
      "Content-Type": types[path.extname(file).toLowerCase()],
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
    });
    if (request.method === "HEAD") response.end();
    else {
      const stream = fs.createReadStream(realFile);
      stream.on("error", () => response.destroy());
      stream.pipe(response);
    }
  } catch {
    response.writeHead(404);
    response.end("Arquivo não encontrado.");
  }
});

server.on("error", (error) => {
  console.error(
    error.code === "EADDRINUSE"
      ? `A porta ${port} já está em uso. Verifique se o front-end já está aberto.`
      : error.message,
  );
  process.exitCode = 1;
});
server.listen(port, "127.0.0.1", () => {
  console.log(`Espelho Vital: http://localhost:${port}/pages/login.html`);
});
