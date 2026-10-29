const fs=require('fs'),path=require('path'),http=require('http'),{spawn}=require('child_process'),assert=require('assert/strict');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const source=process.env.FRONTEND_DIR || 'C:/Users/Valdiney/Documents/workspace/EspelhoVital'; const stage=path.resolve(source);
const log=fs.openSync('target/browser-backend.log','w');
const classpath=[path.resolve('target/test-classes'),path.resolve('target/classes'),fs.readFileSync('target/test-classpath.txt','utf8').trim()].join(path.delimiter);
const backend=spawn(process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME,'bin','java.exe') : 'java',['-cp',classpath,'com.example.espelhovitalback.EspelhovitalBackApplication','--server.port=18081','--app.cors.origins=http://localhost:15500','--app.frontend-url=http://localhost:15500','--spring.datasource.url=jdbc:h2:mem:browser;MODE=MySQL;DB_CLOSE_DELAY=-1','--xai.api.key='],{windowsHide:true,stdio:['ignore',log,log]});
const mime={'.html':'text/html;charset=utf-8','.js':'text/javascript;charset=utf-8','.css':'text/css;charset=utf-8','.png':'image/png','.jpg':'image/jpeg','.svg':'image/svg+xml'};
const server=http.createServer((req,res)=>{
    let rel=decodeURIComponent(new URL(req.url,'http://localhost').pathname).replace(/^\//,'');if(!rel)rel='index.html';
    if(rel.includes('..')){res.writeHead(403);res.end();return;}
    if(rel==='js/config.js'){res.writeHead(200,{'Content-Type':mime['.js']});res.end("window.EV_CONFIG={apiBase:'http://localhost:18081'};");return;}
    let file=path.join(stage,rel);if(!fs.existsSync(file))file=path.join(source,rel);
    if(!fs.existsSync(file)){res.writeHead(404);res.end();return;}res.writeHead(200,{'Content-Type':mime[path.extname(file)]||'application/octet-stream'});fs.createReadStream(file).pipe(res);
});
let chrome,ws;let id=0;const waiting=new Map(),errors=[];
function call(method,params={}){return new Promise((resolve,reject)=>{const key=++id;const timer=setTimeout(()=>{waiting.delete(key);reject(new Error('CDP timeout: '+method));},15000);waiting.set(key,{resolve,reject,timer});ws.send(JSON.stringify({id:key,method,params}));});}
async function evaluate(expression){const result=await call('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(result.exceptionDetails)throw new Error(JSON.stringify(result.exceptionDetails));return result.result.value;}
async function until(expression,description){for(let i=0;i<120;i++){try{if(await evaluate(expression))return;}catch{}await sleep(150);}throw new Error('Timeout: '+description+'; '+await evaluate('document.body.innerText'));}
async function navigate(page){let nav;for(let attempt=0;attempt<3;attempt++){nav=await call('Page.navigate',{url:'http://localhost:15500/'+page});if(!nav.errorText)break;await sleep(400);}if(nav.errorText)throw new Error('Navigation failed: '+JSON.stringify(nav)+' '+await evaluate('JSON.stringify({url:location.href,html:document.documentElement.outerHTML})'));await until('location.pathname === '+JSON.stringify('/'+page)+' && typeof EV !== "undefined" && document.readyState !== "loading"','page '+page);}
async function screenshot(file,width=1280,height=1000){await call('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile:width<600});await sleep(400);const image=await call('Page.captureScreenshot',{format:'png',captureBeyondViewport:true});fs.writeFileSync('target/'+file,Buffer.from(image.data,'base64'));}
(async()=>{
 try {
    server.listen(15500,'127.0.0.1');
    for(let i=0;i<120;i++){try{const r=await fetch('http://localhost:18081/api/csrf');if(r.ok)break;}catch{}if(i===119)throw new Error('Isolated backend failed to start');await sleep(250);}
    const profile=path.resolve('target/chrome-review-'+Date.now());
    chrome=spawn(process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=0','--user-data-dir='+profile,'about:blank'],{windowsHide:true,stdio:'ignore'});
    let port;for(let i=0;i<80;i++){try{port=Number(fs.readFileSync(path.join(profile,'DevToolsActivePort'),'utf8').split('\n')[0]);if(port)break;}catch{}await sleep(150);}
    if(!port)throw new Error('Chrome remote debugger did not start');
    const tab=await (await fetch('http://127.0.0.1:'+port+'/json/new?about:blank',{method:'PUT'})).json();ws=new WebSocket(tab.webSocketDebuggerUrl);await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=reject;});
    ws.onmessage=event=>{const m=JSON.parse(event.data);if(m.id){const pending=waiting.get(m.id);if(pending){clearTimeout(pending.timer);waiting.delete(m.id);m.error?pending.reject(new Error(JSON.stringify(m.error))):pending.resolve(m.result);}}else if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails.text+': '+(m.params.exceptionDetails.exception?.description||''));};
    await call('Runtime.enable');await call('Page.enable');await sleep(500);
    // Create and authenticate through the real server, with the browser's real cookies/CSRF/CORS.
    await navigate('pages/login.html');
    const email='browser-'+Date.now()+'@example.com';
    await evaluate(`EV.json('/usuarios/cadastrar',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({nome:'Teste Calendário',email:${JSON.stringify(email)},senha:'password123'})})`);
    await evaluate(`document.getElementById('email').value=${JSON.stringify(email)};document.getElementById('senha').value='password123';document.getElementById('loginForm').requestSubmit()`);
    await until("location.pathname.endsWith('/perfil-pronto.html') && document.getElementById('cardTotalConsultas')?.textContent === '0'",'login and dashboard');
    await until("document.getElementById('conteudo')?.getAttribute('aria-busy')==='false'",'dashboard loaded');
    assert.equal(await evaluate("document.getElementById('cardMediaSono').textContent"),'Ainda sem registros');
    await screenshot('painel-vazio-desktop.png',1440,1100);
    await navigate('pages/ciclo.html');await until("document.getElementById('historicoCiclos')?.textContent.includes('Nenhuma')",'empty cycle state');
    await evaluate("document.getElementById('novoRegistro').click();document.getElementById('dataUltimaMenstruacao').value='2026-09-30';document.getElementById('cicloForm').requestSubmit()");
    await until("document.getElementById('totalCiclos')?.textContent==='1'",'save confirmed period');
    await evaluate("document.getElementById('visao').value='ano';document.getElementById('visao').dispatchEvent(new Event('change'))");
    assert.equal(await evaluate("document.querySelectorAll('.month-panel').length"),12);
    assert.ok(await evaluate("document.querySelectorAll('.dia.prevista').length")>=10);
    assert.equal(await evaluate("document.querySelectorAll('.dia.menstruacao').length"),5);
    await evaluate("document.getElementById('proximoMes').click()");
    assert.ok(await evaluate("document.querySelectorAll('.dia.prevista').length")>40);
    await screenshot('calendario-anual.png',1440,1100);
    await evaluate("document.getElementById('mesAnterior').click()");
    // Editing irregular preferences recalculates every forecast and removes fertility claims.
    await evaluate("document.querySelector('.history-row button').click();document.getElementById('irregular').value='true';document.getElementById('irregular').dispatchEvent(new Event('change'));document.getElementById('duracaoMinima').value='24';document.getElementById('duracaoMaxima').value='40';document.getElementById('cicloForm').requestSubmit()");
    await until("document.getElementById('avisoCiclo')?.textContent.startsWith('Ciclo irregular:')",'irregular cycle update');
    assert.equal(await evaluate("document.querySelectorAll('.dia.ovulacao,.dia.fertil').length"),0);
    assert.ok(await evaluate("document.querySelectorAll('.dia.incerta').length")>0);
    await screenshot('calendario-irregular.png',1440,1100);
    await evaluate("document.getElementById('visao').value='mes';document.getElementById('visao').dispatchEvent(new Event('change'))");
    await call('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});await sleep(400);
    assert.ok(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'),'mobile layout must fit viewport');
    assert.ok(await evaluate("document.getElementById('ev-sidebar').getBoundingClientRect().right<=1"),'mobile menu starts closed');
    await screenshot('calendario-mobile.png',390,844);
    // Reload restores server history; another recorded start retains the first one.
    await navigate('pages/ciclo.html');await until("document.getElementById('totalCiclos')?.textContent==='1'",'restore history');

    await evaluate("document.querySelector('.dia[data-date=\"2026-10-27\"]').click()");
    assert.ok(await evaluate("document.getElementById('detalheDia').open && document.getElementById('registroSection').hidden"),'quick start does not open full form');
    assert.equal(await evaluate("document.getElementById('registrarDia').textContent"),'Minha menstruação começou neste dia');
    await screenshot('ciclo-registro-rapido-mobile.png',390,844);
    await call('Network.enable');await call('Network.setBlockedURLs',{urls:['*18081/ciclo/inicio']});
    await evaluate("document.getElementById('registrarDia').click()");
    await until("document.getElementById('erroDia').textContent.length>0 && !document.getElementById('registrarDia').disabled",'quick start error recovery');
    await call('Network.setBlockedURLs',{urls:[]});await evaluate("document.getElementById('registrarDia').click()");
    await until("document.getElementById('totalCiclos')?.textContent==='2' && !document.getElementById('detalheDia').open",'quick start recalculates calendar');
    const quickCalendar=await evaluate("EV.json('/ciclo/calendario')");
    assert.equal(quickCalendar.registros[1].duracaoMenstruacao,5);assert.equal(quickCalendar.registros[1].irregular,true);
    assert.equal(quickCalendar.registros[1].duracaoMinima,24);assert.equal(quickCalendar.registros[1].duracaoMaxima,40);
    await evaluate("document.querySelector('.dia[data-date=\"2026-10-27\"]').click()");
    assert.ok(await evaluate("document.getElementById('registrarDia').hidden && !document.getElementById('editarPeriodoDia').hidden"),'existing period is not registered again');
    await evaluate("document.getElementById('fecharDetalhe').click();document.getElementById('desfazerInicio').click()");
    await until("document.getElementById('totalCiclos')?.textContent==='1' && document.getElementById('desfazerInicio').hidden",'undo only new start');
    const future=new Date();future.setDate(future.getDate()+1);
    if(future.getMonth()===new Date().getMonth()){
      const futureIso=[future.getFullYear(),String(future.getMonth()+1).padStart(2,'0'),String(future.getDate()).padStart(2,'0')].join('-');
      await evaluate(`document.querySelector('.dia[data-date="${futureIso}"]').click()`);
      assert.ok(await evaluate("document.getElementById('registrarDia').hidden"),'future date cannot be confirmed');
      await evaluate("document.getElementById('fecharDetalhe').click()");
    }
    await evaluate("document.querySelector('.dia[data-date=\"2026-10-27\"]').click();document.getElementById('registrarDia').click()");
    await until("document.getElementById('totalCiclos')?.textContent==='2' && !document.getElementById('detalheDia').open",'new start after undo');
    await screenshot('ciclo-readaptado.png',1440,1100);
    await evaluate("document.querySelector('.dia[data-date=\"2026-10-28\"]').click()");
    assert.ok(await evaluate("!document.getElementById('fimPeriodo').hidden"));
    await evaluate("document.getElementById('ultimoDiaSangramento').value='2026-10-28';document.getElementById('registrarFimDia').click()");
    await until("!document.getElementById('detalheDia').open",'period end saved');
    let endedCalendar=await evaluate("EV.json('/ciclo/calendario')");
    assert.equal(endedCalendar.registros[1].duracaoMenstruacao,2);
    assert.equal(endedCalendar.registros[1].fimMenstruacao,'2026-10-28');
    assert.equal(endedCalendar.irregular,true);
    await evaluate("document.querySelector('.dia[data-date=\"2026-10-29\"]').click()");
    await evaluate("document.getElementById('ultimoDiaSangramento').value='2026-10-29';document.getElementById('registrarFimDia').click()");
    await until("!document.getElementById('detalheDia').open",'end corrected beyond estimated or saved range');
    endedCalendar=await evaluate("EV.json('/ciclo/calendario')");
    assert.equal(endedCalendar.registros[1].duracaoMenstruacao,3);
    await screenshot('ciclo-fim-registrado.png',390,844);
    assert.ok(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'),'period end mobile');

    // Decimal sleep duration and automatic quality are accepted through the protected API.
    const sleepRecord=await evaluate("EV.json('/sono',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({horas:7.5,qualidade:''})})");
    assert.equal(sleepRecord.horas,7.5);assert.equal(sleepRecord.qualidade,'Boa');
    // Dashboard with real account records, empty state, partial failure and mobile layout.
    await evaluate(`EV.json('/humor',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({estado:'Tranquila, Animada',observacao:'Um dia de cada vez.'})})`);
    await evaluate(`EV.json('/consultas',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({tipo:'CONSULTA',titulo:'Consulta de acompanhamento',dataAgendada:new Date(Date.now()+86400000).toISOString().slice(0,10),horaAgendada:'14:30',status:'AGENDADA'})})`);
    await navigate('perfil-pronto.html');
    await until("document.getElementById('conteudo')?.getAttribute('aria-busy')==='false'",'populated dashboard');
    assert.equal(await evaluate("document.querySelectorAll('.check-state.complete').length"),2);
    assert.ok(await evaluate("document.querySelectorAll('.appointment').length")>0);
    assert.ok(await evaluate("document.getElementById('focoTitulo').textContent.includes('chegando')"));
    await screenshot('painel-desktop.png',1440,1100);
    await screenshot('painel-mobile.png',390,844);
    assert.ok(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'),'dashboard fits mobile viewport');
    await evaluate("document.getElementById('ev-sidebar-toggle').click()");
    assert.equal(await evaluate("document.getElementById('ev-sidebar-toggle').getAttribute('aria-expanded')"),'true');
    await call('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
    assert.equal(await evaluate("document.getElementById('ev-sidebar-toggle').getAttribute('aria-expanded')"),'false');
    await call('Network.enable');await call('Network.setBlockedURLs',{urls:['*18081/dashboard*']});
    await navigate('perfil-pronto.html');await until("!document.getElementById('dashboardErro')?.hidden",'dashboard error state');
    await call('Network.setBlockedURLs',{urls:[]});await evaluate("document.getElementById('tentarNovamente').click()");
    await until("document.getElementById('dashboardErro')?.hidden && document.getElementById('conteudo')?.getAttribute('aria-busy')==='false'",'dashboard recovery');
    for(const page of ['pages/sono.html','pages/humor.html']){
      await navigate(page);await sleep(400);assert.ok(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'),'mobile layout '+page);
    }

    // Integrated diary, reporting, opt-in reminders and privacy controls.
    await call('Browser.setDownloadBehavior',{behavior:'allow',downloadPath:path.resolve('target/feature-downloads')});
    await navigate('pages/diario.html');await until("document.getElementById('diarioForm') && !document.getElementById('diarioForm').inert && !document.getElementById('diarioMensagem').textContent.includes('Carregando')",'diary initial load');
    await evaluate("document.getElementById('fluxo').value='LEVE';document.getElementById('intensidadeDor').value='2';document.querySelector('[name=sintoma][value=COLICA]').checked=true;document.getElementById('horasSono').value='7.8';document.getElementById('observacoesDiario').value='Cólica leve. Observação da rotina.';document.getElementById('observacoesDiario').dispatchEvent(new Event('input',{bubbles:true}));document.getElementById('diarioForm').requestSubmit()");
    await until("!document.getElementById('salvarDia').disabled && document.getElementById('diarioMensagem').textContent.includes('foi salvo')",'journal saved');
    await evaluate("document.getElementById('observacoesDiario').value='Observação revisada, com atenção.';document.getElementById('observacoesDiario').dispatchEvent(new Event('input',{bubbles:true}));document.getElementById('diarioForm').requestSubmit()");
    await until("!document.getElementById('salvarDia').disabled && document.getElementById('diarioMensagem').textContent.includes('foi salvo')",'journal updated');
    assert.equal((await evaluate("EV.json('/sono/historico?pagina=0&tamanho=20')")).totalRegistros,1);
    assert.equal((await evaluate("EV.json('/diario/'+document.getElementById('dataDiario').value)")).observacoes,'Observação revisada, com atenção.');
    await screenshot('diario-desktop.png',1440,1100);await screenshot('diario-mobile.png',390,844);
    assert.ok(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'),'diary mobile');
    await navigate('pages/relatorio.html');await evaluate("document.getElementById('relatorioForm').requestSubmit()");
    await until("!document.getElementById('relatorioPreview').hidden",'report preview');
    await screenshot('resumo-consulta.png',1440,1100);await evaluate("document.getElementById('baixarPdf').click()");
    await until("document.getElementById('relatorioMensagem').textContent.includes('pronto para salvar')",'PDF generated');
    await navigate('pages/minha-conta.html');await until("!document.getElementById('lembretesForm').inert",'reminder preferences loaded');
    await evaluate("document.getElementById('lembretesAtivos').checked=true;document.getElementById('lembreteConsultas').checked=true;document.getElementById('horarioLembrete').value='00:00';document.getElementById('lembretesForm').requestSubmit()");
    await until("document.getElementById('lembretesMensagem').textContent.includes('Preferências salvas')",'preferences saved');
    await screenshot('preferencias-lembretes.png',1440,1100);
    await navigate('perfil-pronto.html');await until("document.getElementById('lembreteQuantidade')?.textContent==='1'",'in-app reminder');
    await evaluate("document.getElementById('lembreteBotao').click()");await until("!document.getElementById('lembretePainel').hidden",'reminder popover');
    await evaluate("document.querySelector('.reminder-item button').click()");await until("document.getElementById('lembreteQuantidade').hidden",'reminder dismissed');
    await navigate('pages/minha-conta.html');await until("document.getElementById('privacidadeContagens')?.children.length===5",'privacy counters');
    await evaluate("document.getElementById('tabPrivacidade').click();document.getElementById('exportarDados').click();document.getElementById('senhaPrivacidade').value='incorrect';document.getElementById('confirmacaoForm').requestSubmit()");
    await until("document.getElementById('confirmacaoMensagem').textContent.includes('inválida')",'incorrect password leaves session valid');
    await evaluate("document.getElementById('senhaPrivacidade').value='password123';document.getElementById('confirmacaoForm').requestSubmit()");
    await until("!document.getElementById('confirmarPrivacidade').open && document.getElementById('privacidadeMensagem').textContent.includes('pronta')",'JSON export downloaded');
    await screenshot('privacidade-desktop.png',1440,1100);await screenshot('privacidade-mobile.png',390,844);
    assert.ok(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'),'privacy mobile');
    await evaluate("document.getElementById('apagarRegistros').click();document.getElementById('senhaPrivacidade').value='password123';document.getElementById('frasePrivacidade').value='não confirmar';document.getElementById('confirmacaoForm').requestSubmit()");
    await until("document.getElementById('confirmacaoMensagem').textContent.includes('exatamente')",'destructive action confirmation');
    await evaluate("document.getElementById('frasePrivacidade').value='APAGAR MEUS REGISTROS';document.getElementById('confirmacaoForm').requestSubmit()");
    await until("!document.getElementById('confirmarPrivacidade').open && document.getElementById('privacidadeMensagem').textContent.includes('Registros apagados')",'health data clear');
    const counters=await evaluate("EV.json('/conta/privacidade')");assert.deepEqual(Object.values(counters.registros),[0,0,0,0,0]);
    assert.equal((await evaluate("EV.json('/lembretes/config')")).preferencias.ativo,false);
    // Stored markup is rendered as text, never executed.
    await evaluate(`EV.json('/consultas',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({tipo:'CONSULTA',titulo:'<img src=x onerror="window.injetado=true">',dataAgendada:'2026-12-01',status:'AGENDADA',observacao:'<script>window.injetado=true</script>'})})`);
    await navigate('pages/consultas.html');await until("[...document.querySelectorAll('.consulta-info h3')].some(e=>e.textContent.includes('<img'))",'consultation text rendering');
    assert.equal(await evaluate('Boolean(window.injetado)'),false);assert.equal(await evaluate("document.querySelectorAll('.consulta-info img,.consulta-info script').length"),0);
    // Session logout is enforced by the server.
    await evaluate("document.getElementById('ev-logout-btn').click()");await until("location.pathname.endsWith('/pages/login.html')",'logout');
    await call('Page.navigate',{url:'http://localhost:15500/pages/ciclo.html'});await until("location.pathname.endsWith('/pages/login.html')",'unauthenticated page redirect');

    // Account deletion is exercised only on a newly created temporary test account.
    const deleteEmail='delete-'+Date.now()+'@example.com';
    await evaluate(`EV.json('/usuarios/cadastrar',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({nome:'Teste Exclusão',email:${JSON.stringify(deleteEmail)},senha:'password123'})})`);
    await evaluate(`document.getElementById('email').value=${JSON.stringify(deleteEmail)};document.getElementById('senha').value='password123';document.getElementById('loginForm').requestSubmit()`);
    await until("location.pathname.endsWith('/perfil-pronto.html') && document.getElementById('conteudo')?.getAttribute('aria-busy')==='false'",'temporary account login');
    await navigate('pages/minha-conta.html');await until("document.getElementById('privacidadeContagens')?.children.length===5",'delete account settings');
    await evaluate("document.getElementById('tabPrivacidade').click();document.getElementById('excluirConta').click();document.getElementById('senhaPrivacidade').value='password123';document.getElementById('frasePrivacidade').value='EXCLUIR MINHA CONTA';document.getElementById('confirmacaoForm').requestSubmit()");
    await until("location.pathname.endsWith('/pages/login.html')",'account deletion invalidates session');
    const downloads=fs.readdirSync('target/feature-downloads');assert.ok(downloads.some(f=>f.endsWith('.pdf')),'PDF download exists');assert.ok(downloads.some(f=>f.endsWith('.json')),'JSON download exists');
    assert.deepEqual(errors,[],'unexpected JavaScript runtime errors');
    fs.writeFileSync('target/browser-validation.json',JSON.stringify({passed:true,checks:['real login/session/CSRF/CORS','12-month calendar','multiple-cycle forecasts','irregular date ranges without fertility claims','mobile layout','history persistence and new start','decimal sleep','stored HTML rendered as text','server logout and protected redirect','empty and populated dashboard','contextual dashboard priority','daily check-in from actual records','dashboard error and retry','mobile sidebar keyboard navigation','integrated diary create and edit','PDF and JSON downloads','opt-in reminders and dismissal','health data deletion confirmation','temporary account deletion','date-only cycle start','quick start network retry','duplicate and future-date guards','quick cycle undo'],screenshots:['calendario-anual.png','calendario-irregular.png','calendario-mobile.png','painel-desktop.png','painel-mobile.png','painel-vazio-desktop.png']},null,2));
    console.log('Browser integration checks passed (Chrome + isolated H2 backend).');
 } finally {try{ws?.close();}catch{}chrome?.kill();backend.kill();server.close();fs.closeSync(log);}
})().catch(error=>{console.error(error);process.exitCode=1;});
