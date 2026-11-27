import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { once } from "node:events";
import { readFile, mkdtemp, rm } from "node:fs/promises";
import { createServer } from "node:http";
import path from "node:path";
import { tmpdir } from "node:os";

const root = path.resolve(process.argv[2] || "dist");


const password = "fixture-only";
const email = "fixture@example.test";
const trace = [];
let completed = false;
let rejectNext = true;
let holdNext = false;
let pending;
let saved;
let controlIbge = true;
const consultas = [];
const municipios = { SP: [{nome:"São Paulo"}], RJ: [{nome:"Rio de Janeiro"}] };
const server = createServer(async (req, res) => {
    const pathname = new URL(req.url, "http://localhost").pathname;
    const reply = (status, payload) => {
        res.writeHead(status, {"content-type":"application/json","cache-control":"no-store"});
        res.end(JSON.stringify(payload));
    };
    if (pathname.startsWith("/ibge/")) {
        const uf = pathname.split("/").at(-1);
        const headers = {"content-type":"application/json","access-control-allow-origin":"*","cache-control":"no-store"};
        const responder = (status, body) => {
            if (res.destroyed) return false;
            res.writeHead(status,headers).end(body);
            return true;
        };
        if (!controlIbge) return responder(200,JSON.stringify(municipios[uf]));
        consultas.push({
            uf, res, inicio: performance.now(), responder,
            iniciarCorpo() {
                res.writeHead(200,headers);
                res.write("[");
            }
        });
        return;
    }
    if (pathname.startsWith("/api/v1/")) {
        trace.push({pathname, method:req.method});
        if (pathname.endsWith("/auth/login")) return reply(200,{token:"fixture",userId:1,nome:"Conta teste",role:"USER"});
        if (pathname.endsWith("/recommendation-profiles/1/status")) return reply(200,completed);
        if (pathname.endsWith("/objectives")) return reply(200,[{id:1,nome:"Objetivo de teste"}]);
        if (pathname.endsWith("/interests")) return reply(200,[{id:1,nome:"Interesse de teste"}]);
        if (pathname.endsWith("/recommendation-profiles/1")) {
            if (req.method === "GET") return reply(200,saved);
            const chunks = [];
            for await (const chunk of req) chunks.push(chunk);
            const data=JSON.parse(Buffer.concat(chunks));
            if (holdNext) {
                pending={reply,response:res};
                return;
            }
            if (rejectNext) {
                rejectNext=false;
                return reply(400,{error:"Falha de teste"});
            }
            saved={...data,questionarioConcluido:true};
            completed=true;
            return reply(201,saved);
        }
        if (pathname.endsWith("/profile/me")) return reply(200,{nome:"Conta teste"});
        return reply(200,[]);
    }
    try {
        const asset = pathname.startsWith("/assets/") ? pathname : "/index.html";
        const body = await readFile(path.join(root, asset));
        res.writeHead(200, { "content-type": asset.endsWith(".js") ? "text/javascript" : asset.endsWith(".css") ? "text/css" : "text/html" });
        res.end(body);
    } catch {
        res.writeHead(404).end();
    }
});
const poll = async (read) => {
    for (let i = 0; i < 350; i++) {
        const result = await read();
        if (result) return result;
        await new Promise(resolve => setTimeout(resolve, 40));
    }
    throw new Error("Tempo limite");
};
await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
const origin = "http://127.0.0.1:" + server.address().port;
const profileDir = await mkdtemp(path.join(tmpdir(), "recommenda-10210-regression-edge-"));
const browser = spawn(process.env.RECOMMENDA_EDGE_PATH || "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe", [
    "--headless=new", "--remote-debugging-port=0", "--disable-background-networking",
    "--no-first-run", "--no-default-browser-check", "--no-proxy-server",
    "--host-resolver-rules=MAP * ~NOTFOUND, EXCLUDE 127.0.0.1",
    "--user-data-dir=" + profileDir, "about:blank"
], { stdio: "ignore", windowsHide: true });
let socket;
const browserExited = once(browser, "close").catch(() => {});
try {
    const port = (await poll(() => readFile(path.join(profileDir, "DevToolsActivePort"), "utf8").catch(() => null))).split("\n")[0];
    const page = await (await fetch("http://127.0.0.1:" + port + "/json/new?about:blank", { method: "PUT" })).json();
    socket = new WebSocket(page.webSocketDebuggerUrl);
    await once(socket, "open");
    let id = 0;
    const calls = new Map();
    socket.addEventListener("message", event => {
        const message = JSON.parse(event.data);
        if (message.method === "Runtime.exceptionThrown") console.error(message.params.exceptionDetails.exception?.description);
        calls.get(message.id)?.(message);
        calls.delete(message.id);
    });
    const cdp = (method, params = {}) => new Promise(resolve => {
        calls.set(++id, resolve);
        socket.send(JSON.stringify({ id, method, params }));
    });
    const evaluate = async (expression, awaitPromise = false) => {
        const { result } = await cdp("Runtime.evaluate", { expression, returnByValue: true, awaitPromise });
        if (result.exceptionDetails) throw new Error(result.result.description);
        return result.result.value;
    };
    const go = async (url) => {
        await evaluate(`history.pushState({}, "", ${JSON.stringify(url)}); dispatchEvent(new PopStateEvent("popstate"));`);
        await poll(() => evaluate(`location.pathname + location.search === ${JSON.stringify(url)}`));
    };
    const flush = () => evaluate("new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve(true))))", true);
    const click = async label => {
        const query = `[...document.querySelectorAll("button")].find(e => e.textContent.trim() === ${JSON.stringify(label)})`;
        await poll(() => evaluate(`Boolean(${query})`));
        await evaluate(`${query}.click()`);
    };
    const login = async (email, destination) => {
        await poll(() => evaluate('!!document.querySelector("#senha")'));
        await evaluate(`for (const [selector, value] of [["#email", ${JSON.stringify(email)}], ["#senha", ${JSON.stringify(password)}]]) {
            const input = document.querySelector(selector);
            Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value").set.call(input, value);
            input.dispatchEvent(new Event("input", { bubbles: true }));
        }`);
        await click("Entrar");
        await poll(() => evaluate(`location.pathname === ${JSON.stringify(destination)}`));
    };
    const shown = expr => poll(() => evaluate(expr));

    await cdp("Network.enable");
    await cdp("Runtime.enable");
    await cdp("Page.enable");
    const falhasInterceptacao = [];
    let falharTransporte = false;
    socket.addEventListener("message", async event => {
        const {method,params} = JSON.parse(event.data);
        if (method !== "Fetch.requestPaused") return;
        const uf = new URL(params.request.url).pathname.split("/").at(-2);
        if (falharTransporte) {
            falharTransporte = false;
            consultas.push({uf, encerrar: () => cdp("Fetch.failRequest", {
                requestId:params.requestId, errorReason:"Failed"
            })});
            return;
        }
        const result = await cdp("Fetch.continueRequest", {requestId:params.requestId,url:origin+"/ibge/"+uf});
        if (result.error) falhasInterceptacao.push(result.error);
    });
    const interception = await cdp("Fetch.enable", {patterns:[{urlPattern:"https://servicodados.ibge.gov.br/api/v1/localidades/estados/*/municipios"}]});
    assert.equal(interception.error,undefined);

    const select = async (selector,value) => {
        await shown("!!document.querySelector("+JSON.stringify(selector)+") && !document.querySelector("+JSON.stringify(selector)+").disabled");
        const selected = await evaluate("((s,v)=>{const e=document.querySelector(s);if(![...e.options].some(o=>o.value===v))return false;e.value=v;e.dispatchEvent(new Event('change',{bubbles:true}));return true})("+JSON.stringify(selector)+","+JSON.stringify(value)+")");
        assert.equal(selected,true);
        await flush();
    };
    const uf=".onboarding-location label:first-child select";
    const city=".onboarding-location label:nth-child(2) select";
    const valor = selector => evaluate("document.querySelector("+JSON.stringify(selector)+").value");
    const cidadeDisponivel = nome => shown("(()=>{const s=document.querySelector("+JSON.stringify(city)+");return s&&!s.disabled&&[...s.options].some(o=>o.value==="+JSON.stringify(nome)+")})()");
    const consulta = (indice, estado) => poll(() => {
        if (!consultas[indice]) return false;
        assert.equal(consultas[indice].uf,estado);
        return consultas[indice];
    });
    const retry = async (repetir=false) => {
        const botao = "[...document.querySelectorAll('button')].find(e=>e.textContent.trim()==='Tentar novamente')";
        await shown("Boolean("+botao+")");
        if (repetir) await evaluate("(()=>{const b="+botao+";b.click();b.click();b.click()})()");
        else await click("Tentar novamente");
    };
    const bloqueio = async estado => {
        assert.equal(await valor(uf),estado);
        assert.equal(await evaluate("document.querySelector("+JSON.stringify(city)+").disabled"),true);
        assert.equal(await evaluate("[...document.querySelectorAll('button')].find(e=>e.textContent.trim()==='Continuar').disabled"),true);
    };
    const localizacao = async () => {
        for (const label of ["De 2 a 5 horas","Até R$ 100","Prefiro atividades individuais","Baixa","Em casa"]) {
            await click(label);
            await flush();
        }
    };
    const verificarIbge = async () => {
        await bloqueio("");
        await select(uf,"SP");
        (await consulta(0,"SP")).responder(503,'{"error":"fixture"}');
        await shown("document.body.innerText.includes('Não foi possível carregar as cidades')");
        await bloqueio("SP");
        assert.equal(consultas.length,1);

        falharTransporte=true;
        await retry(true);
        const transporte = await consulta(1,"SP");
        await flush();
        assert.equal(consultas.length,2);
        assert.equal((await transporte.encerrar()).error,undefined);
        await shown("document.body.innerText.includes('Não foi possível carregar as cidades')");
        await bloqueio("SP");

        await retry();
        (await consulta(2,"SP")).responder(200,"{inválido");
        await shown("document.body.innerText.includes('Não foi possível carregar as cidades')");
        await bloqueio("SP");

        await retry();
        (await consulta(3,"SP")).responder(200,JSON.stringify(municipios.SP));
        await cidadeDisponivel("São Paulo");
        await select(city,"São Paulo");
        await select(uf,"RJ");
        const antiga = await consulta(4,"RJ");
        await bloqueio("RJ");
        assert.equal(await valor(city),"");

        await select(uf,"SP");
        const incompleta = await consulta(5,"SP");
        await poll(()=>antiga.res.destroyed);
        await bloqueio("SP");
        incompleta.iniciarCorpo();
        await shown("document.body.innerText.includes('A consulta de municípios demorou demais')");
        const ms = Math.round(performance.now()-incompleta.inicio);
        assert.ok(ms>=9500 && ms<=11500,"Timeout observado: "+ms+" ms");
        await poll(()=>incompleta.res.destroyed);
        await bloqueio("SP");
        assert.equal(consultas.length,6);

        await retry(true);
        const nova = await consulta(6,"SP");
        nova.responder(200,'[{"nome":"Campinas"}]');
        await cidadeDisponivel("Campinas");
        assert.equal(antiga.responder(200,JSON.stringify(municipios.RJ)),false);
        incompleta.res.end('{"nome":"Município obsoleto"}]');
        await flush();
        assert.equal(await evaluate("[...document.querySelector("+JSON.stringify(city)+").options].map(o=>o.value).join('|')"),"|Campinas");
        assert.equal(consultas.length,7);
        assert.deepEqual(falhasInterceptacao,[]);
        console.log("IBGE: 503, transporte, JSON inválido, recuperação na mesma UF, cliques repetidos, troca e respostas obsoletas.");
        console.log("IBGE: timeout durante a leitura do corpo em "+ms+" ms, com cancelamento e recuperação manual.");
        controlIbge=false;
        await select(uf,"RJ");
        await cidadeDisponivel("Rio de Janeiro");
        await select(uf,"SP");
        await cidadeDisponivel("São Paulo");
    };

    const answer = async editing => {
        await localizacao();
        if (!editing) await verificarIbge();
        await select(city,"São Paulo");
        await click("Continuar");
        await shown("document.body.innerText.includes('Quais assuntos')");
        if (!editing) await click("Interesse de teste");
        await click("Continuar");
        await shown("document.body.innerText.includes('O que você busca')");
        if (!editing) await click("Objetivo de teste");
    };
    const posts=()=>trace.filter(x=>x.method==="POST" && x.pathname.endsWith("/recommendation-profiles/1")).length;
    const recommendations=()=>trace.filter(x=>x.method==="GET" && x.pathname.endsWith("/recommendations/1")).length;
    await cdp("Page.navigate",{url:origin+"/login"});
    await login(email,"/onboarding");
    await answer(false);
    await click("Continuar");
    await shown("document.body.innerText.includes('Algumas informações são inválidas')");
    assert.equal(await evaluate("location.pathname"),"/onboarding");
    assert.equal(recommendations(),0);
    assert.equal(posts(),1);
    await click("Continuar");
    await shown("location.pathname === '/recommendations'");
    await shown("document.body.innerText.includes('Nenhuma recomendação encontrada')");
    assert.equal(posts(),2);
    assert.equal(recommendations(),1);
    console.log("Primeira conclusão: erro manteve o formulário; novo envio abriu recomendações e fez GET.");

    await click("Atualizar preferências");
    await shown("location.search === '?editar=true'");
    await answer(true);
    holdNext=true;
    await click("Continuar");
    await poll(()=>pending);
    assert.equal(await evaluate("document.querySelector('.onboarding-continue').disabled"),true);
    await go("/myProfile");
    await poll(()=>pending.response.destroyed);
    pending.reply(201,saved);
    await flush();
    assert.equal(await evaluate("location.pathname"),"/myProfile");
    assert.equal(recommendations(),1);
    console.log("Saída do formulário abortou a gravação pendente e impediu navegação tardia.");

    holdNext=false;
    await go("/onboarding?editar=true");
    await answer(true);
    await click("Continuar");
    await shown("location.pathname === '/MyProfile'");
    assert.equal(posts(),4);
    assert.equal(recommendations(),1);
    console.log("Edição válida voltou ao perfil e preservou o destino anterior.");
    await go("/onboarding?editar=true");
    await localizacao();
    await cidadeDisponivel("São Paulo");
    controlIbge=true;
    await select(uf,"RJ");
    const aoSair=await consulta(7,"RJ");
    await go("/myProfile");
    await poll(()=>aoSair.res.destroyed);
    assert.deepEqual(falhasInterceptacao,[]);
    console.log("IBGE: saída da tela cancelou a consulta pendente.");
} finally {
    socket?.close();
    browser.kill();
    await browserExited;
    server.closeAllConnections();
    await new Promise(resolve => server.close(resolve));
    await rm(profileDir, { recursive: true, force: true, maxRetries: 6, retryDelay: 250 });
}












