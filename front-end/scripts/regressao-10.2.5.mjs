import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { once } from "node:events";
import { readFile, mkdtemp, rm } from "node:fs/promises";
import { createServer } from "node:http";
import path from "node:path";
import { tmpdir } from "node:os";

const root = path.resolve(process.argv[2] || "dist");
const reproduce = process.argv.includes("--reproduce");
const profileDir = await mkdtemp(path.join(tmpdir(), "recommenda-edge-"));
const pending = [];
const seen = [];
const server = createServer(async (req, res) => {
    const pathname = new URL(req.url, "http://localhost").pathname;
    const send = (data, status = 200) => {
        res.writeHead(status, { "content-type": "application/json", "cache-control": "no-store" });
        res.end(JSON.stringify(data));
    };
    if (pathname.startsWith("/api/v1/")) {
        seen.push({ pathname, method: req.method, token: req.headers.authorization });
        if (pathname.endsWith("/auth/login")) {
            const chunks = [];
            for await (const chunk of req) chunks.push(chunk);
            const email = JSON.parse(Buffer.concat(chunks)).email;
            const userId = email.startsWith("a@") ? 101 : 202;
            return send({ token: "fixture-" + userId, userId, nome: "Conta " + userId, role: "USER" });
        }
        if (pathname.endsWith("/social/me/disable") ||
            (pathname.endsWith("/community/contacts/requests") && req.method === "POST") ||
            (pathname.endsWith("/social/me/profile") && req.method === "PUT") ||
            pathname.endsWith("/social/me/profile/photo") ||
            (/\/users\/\d+\/profile$/.test(pathname) && req.method === "PUT") ||
            (/\/recommendation-profiles\/\d+$/.test(pathname) && req.method === "GET")) {
            const chunks = [];
            for await (const chunk of req) chunks.push(chunk);
            pending.push({ pathname, method: req.method, headers: req.headers, body: Buffer.concat(chunks), send,
                get closed() { return res.destroyed; } });
            return;
        }
        if (pathname.endsWith("/community/users/303")) return send({ userId: 303, nome: "Outro participante" });
        if (pathname.endsWith("/social/me/status")) return send({ participa: true });
        if (pathname.endsWith("/social/me/profile/preview")) return send({ userId: 202, nome: "Perfil sintético", possuiFoto: true });
        if (pathname.endsWith("/thumbnail")) {
            res.writeHead(200, { "content-type": "image/png", etag: "\"fixture\"" });
            return res.end(Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a0e0AAAAASUVORK5CYII=", "base64"));
        }
        if (pathname.endsWith("/social/me/profile")) return send({ biografia: "Perfil sintético" });
        if (pathname.endsWith("/status")) return send(true);
        return send([]);
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
    for (let i = 0; i < 150; i++) {
        const result = await read();
        if (result) return result;
        await new Promise(resolve => setTimeout(resolve, 40));
    }
    throw new Error("Tempo limite");
};
await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
const origin = "http://127.0.0.1:" + server.address().port;
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
    const login = async email => {
        await poll(() => evaluate('!!document.querySelector("#senha")'));
        await evaluate(`for (const [selector, value] of [["#email", ${JSON.stringify(email)}], ["#senha", "fixture-password"]]) {
            const input = document.querySelector(selector);
            Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value").set.call(input, value);
            input.dispatchEvent(new Event("input", { bubbles: true }));
        }`);
        await click("Entrar");
        await poll(() => evaluate('location.pathname === "/recommendations"'));
    };
    const submitProfile = async () => {
        await go("/profile");
        for (const label of ["Continuar", "Continuar", "Sozinho", "Continuar", "Leve", "Continuar", "Em casa", "Continuar", "Iniciante", "Ver meus hobbies"]) await click(label);
        return poll(() => pending.shift());
    };
    await cdp("Network.enable");
    await cdp("Network.setCacheDisabled", { cacheDisabled: true });
    await cdp("Runtime.enable");
    await cdp("Page.enable");
    const navigation = await cdp("Page.navigate", { url: origin + "/login" });
    if (navigation.result?.errorText) throw new Error(JSON.stringify(navigation));
    await login("a@example.test");
    const abandoned = await submitProfile();
    await go("/onboarding");
    await poll(() => evaluate('!!document.querySelector(".onboarding-page")'));
    abandoned.send({});
    await poll(() => evaluate('performance.getEntriesByType("resource").some(e => e.name.endsWith("/users/101/profile"))'));
    assert.equal(await evaluate("location.pathname"), "/onboarding");
    console.log("PASS Profile: PUT tardio após desmontagem preservou /onboarding.");
    const older = await submitProfile();
    await go("/login");
    await login("b@example.test");
    await go("/onboarding");
    await poll(() => evaluate('!!document.querySelector(".onboarding-page")'));
    await poll(() => older.closed);
    older.send({}, 401);
    await flush();
    assert.equal(await evaluate("location.pathname"), "/onboarding");
    assert.ok(seen.some(({ token }) => token === "Bearer fixture-202"));
    console.log("PASS Profile A→B: PUT 401 antigo não encerrou B nem mudou a rota.");
    const editUrl = "/onboarding?editar=true";
    await go(editUrl);
    const earlierEdit = await poll(() => pending.shift());
    await go("/onboarding");
    await poll(() => seen.filter(({ pathname }) => pathname.endsWith("/objectives")).length >= 4);
    await go(editUrl);
    const currentEdit = await poll(() => pending.shift());
    currentEdit.send({ tempoDisponivelSemanal: 5 });
    await poll(() => evaluate('document.querySelector(".onboarding-option.selected")?.textContent.trim() === "De 2 a 5 horas"'));
    await poll(() => earlierEdit.closed);
    earlierEdit.send({ tempoDisponivelSemanal: 15 });
    await flush();
    assert.equal(await evaluate('document.querySelector(".onboarding-option.selected")?.textContent.trim()'), "De 2 a 5 horas");
    console.log("PASS Onboarding montado: GET anterior preservou a seleção da consulta vigente.");
    await go("/recommendations");
    await go(editUrl);
    const canceledEdit = await poll(() => pending.shift());
    await go("/onboarding");
    await poll(() => canceledEdit.closed);
    canceledEdit.send({}, 500);
    await flush();
    const loadError = '!!document.querySelector(".onboarding-error")?.textContent.includes("carregar as suas preferências")';
    assert.equal(await evaluate(loadError), false);
    await go(editUrl);
    const legitimate = await poll(() => pending.shift());
    legitimate.send({}, 500);
    await poll(() => evaluate(loadError));
    console.log("PASS Onboarding: consulta substituída silenciosa, HTTP 500 da consulta vigente visível.");
    const save = await submitProfile();
    save.send({ message: "Erro sintético da sessão vigente" }, 403);
    await poll(() => evaluate('document.body.innerText.includes("Erro sintético da sessão vigente")'));
    await go("/onboarding");
    const success = await submitProfile();
    success.send({});
    await poll(() => evaluate('location.pathname === "/recommendations"'));
    console.log("PASS Profile ativo: HTTP 403 visível e PUT 200 navegou normalmente.");

    const social = "/myProfile?aba=social";
    const leave = "Sair da comunidade";
    const saveLabel = "Salvar perfil e visibilidade";
    const next = (suffix, method) => poll(() => {
        const index = pending.findIndex(req => req.pathname.endsWith(suffix) && req.method === method);
        return index < 0 ? null : pending.splice(index, 1)[0];
    });
    await go("/community/users/303");
    await click("Solicitar conversa");
    await click("Enviar solicitação");
    const contact = await next("/community/contacts/requests", "POST");
    await go("/onboarding");
    await poll(() => evaluate('!!document.querySelector(".onboarding-page")'));
    if (reproduce) {
        contact.send({});
        await poll(() => evaluate('location.pathname === "/community" && location.search === "?aba=conversas"'));
        console.log("REPRO SocialProfile: solicitação de conversa tardia navegou após desmontagem, na mesma sessão.");
    } else {
        await poll(() => contact.closed);
        contact.send({});
        await flush();
        assert.equal(await evaluate("location.pathname"), "/onboarding");
        console.log("PASS SocialProfile: solicitação de conversa tardia foi cancelada ao desmontar, sem navegação.");
        await go("/community/users/303");
        await click("Solicitar conversa");
        await click("Enviar solicitação");
        (await next("/community/contacts/requests", "POST")).send({});
        await poll(() => evaluate('location.pathname === "/community" && location.search === "?aba=conversas"'));
        console.log("PASS SocialProfile: solicitação vigente abriu a aba de conversas.");
    }
    await evaluate('window.confirm = () => true; window.participationEvents = 0; addEventListener("social-participation-changed", () => window.participationEvents++)');
    await go(social);
    await click(leave);
    const leaving = await next("/social/me/disable", "POST");
    await go("/login");
    await login("b@example.test");
    await go("/onboarding");
    await poll(() => evaluate('!!document.querySelector(".onboarding-page")'));
    if (reproduce) {
        leaving.send({});
        await poll(() => evaluate('location.pathname === "/community" && window.participationEvents === 1'));
        console.log("REPRO SocialProfile: relogin com o mesmo token, POST antigo emitiu evento e redirecionou.");
    } else {
        await poll(() => leaving.closed);
        leaving.send({});
        await flush();
        assert.equal(await evaluate("location.pathname"), "/onboarding");
        assert.equal(await evaluate("window.participationEvents"), 0);
        console.log("PASS SocialProfile: relogin com o mesmo token cancelou POST antigo, sem evento ou navegação.");

        await go(social);
        await poll(() => evaluate('!!document.querySelector(".social-avatar-trigger")'));
        await click(saveLabel);
        const staleSave = await next("/social/me/profile", "PUT");
        await go("/recommendations");
        await poll(() => staleSave.closed);
        await go(social);
        await poll(() => evaluate('!!document.querySelector(".social-avatar-trigger")'));
        await evaluate(`const form = document.querySelector("form.social-profile-editor");
            for (let i = 0; i < 2; i++) form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));`);
        const liveSave = await next("/social/me/profile", "PUT");
        const thumbnailCount = () => seen.filter(req => req.pathname.endsWith("/thumbnail")).length;
        const before = thumbnailCount();
        staleSave.send({ biografia: "Obsoleta" });
        await flush();
        assert.equal(thumbnailCount(), before);
        assert.equal(await evaluate('document.querySelector(".social-profile-editor button.community-primary-button").disabled'), true);
        assert.equal(await evaluate('document.body.innerText.includes("Perfil e visibilidade salvos.")'), false);
        liveSave.send({ biografia: "Atual" });
        await poll(() => evaluate('document.body.innerText.includes("Perfil e visibilidade salvos.")'));
        await poll(() => thumbnailCount() > before);
        assert.equal(await evaluate('document.querySelector(".social-profile-editor textarea").value'), "Atual");
        assert.equal(seen.filter(req => req.method === "PUT" && req.pathname.endsWith("/social/me/profile")).length, 2);
        console.log("PASS SocialProfile: PUT desmontado preservou loading, mensagens e cache da operação seguinte; PUT atual atualizou miniatura e mostrou sucesso.");

        await click(saveLabel);
        const error = await next("/social/me/profile", "PUT");
        error.send({ detail: "Falha vigente de perfil social" }, 403);
        await poll(() => evaluate('document.querySelector(".social-profile-page [role=alert]")?.textContent === "Falha vigente de perfil social"'));
        assert.equal(await evaluate('document.querySelector(".social-profile-editor button.community-primary-button").disabled'), false);
        await evaluate(`const photo = document.querySelector('input[type=file]');
            const transfer = new DataTransfer();
            transfer.items.add(new File([new Uint8Array([137,80,78,71])], "fixture.png", { type: "image/png" }));
            photo.files = transfer.files;
            photo.dispatchEvent(new Event("change", { bubbles: true }));`);
        const upload = await next("/social/me/profile/photo", "PUT");
        assert.equal(upload.headers.authorization, "Bearer fixture-202");
        assert.match(upload.headers["content-type"], /^multipart\/form-data; boundary=/);
        assert.match(upload.body.toString("latin1"), /name="file"; filename="fixture.png"/);
        upload.send({});
        await poll(() => evaluate('document.body.innerText.includes("Foto atualizada.")'));
        console.log("PASS SocialProfile: upload preservou Bearer e multipart; prévia e confirmação atualizaram.");
        await click(leave);
        (await next("/social/me/disable", "POST")).send({});
        await poll(() => evaluate('location.pathname === "/community" && window.participationEvents === 1'));
        console.log("PASS SocialProfile: 403 vigente visível, loading liberado e POST vigente emitiu um evento e navegou.");
    }
} finally {
    socket?.close();
    browser.kill();
    await browserExited;
    server.closeAllConnections();
    await new Promise(resolve => server.close(resolve));
    await rm(profileDir, { recursive: true, force: true, maxRetries: 6, retryDelay: 250 });
}
