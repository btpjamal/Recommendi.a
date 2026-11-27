import assert from "node:assert/strict";
import { beforeEach, test, mock } from "node:test";
import axios from "axios";
import { session } from "../../auth/session.js";
import { invalidarMiniaturas, solicitarMiniatura, solicitarFoto } from "./socialAvatarRequest.js";

const first = new Blob(["first"]);
const second = new Blob(["second"]);
let pending;

beforeEach(() => {
    mock.restoreAll();
    session.clear();
    session.setItem("token", "session-a");
    pending = [];
    mock.method(axios, "get", (url, options) => new Promise((resolve, reject) => {
        pending.push({ url, options, resolve, reject });
    }));
});

function respond(call, data = first, etag = '"first"', status = 200) {
    call.resolve({ data, headers: { etag }, status });
}

async function load(id = 1, data = first) {
    const request = solicitarMiniatura(id, "session-a");
    respond(pending.at(-1), data);
    return request.promise;
}

test("compartilha a requisição pendente e só reutiliza o Blob após 304", async () => {
    const a = solicitarMiniatura(1, "session-a");
    const b = solicitarMiniatura("1", "session-a");
    assert.equal(a.promise, b.promise);
    assert.equal(pending.length, 1);
    a.liberar();
    assert.equal(pending[0].options.signal.aborted, false);
    respond(pending[0]);
    assert.equal(await b.promise, first);

    const next = solicitarMiniatura(1, "session-a");
    assert.notEqual(next.promise, b.promise);
    assert.equal(pending[1].options.headers["If-None-Match"], '"first"');
    assert.equal(pending[1].options.validateStatus(304), true);
    respond(pending[1], undefined, '"first"', 304);
    assert.equal(await next.promise, first);

    const changed = solicitarMiniatura(1, "session-a");
    respond(pending[2], second, '"second"');
    assert.equal(await changed.promise, second);
    const confirmed = solicitarMiniatura(1, "session-a");
    assert.equal(pending[3].options.headers["If-None-Match"], '"second"');
    respond(pending[3], undefined, '"second"', 304);
    assert.equal(await confirmed.promise, second);
});

for (const status of [401, 403, 404, undefined]) {
    test(`descarta o cache depois de falha ${status ?? "de rede"}`, async () => {
        await load();
        const request = solicitarMiniatura(1, "session-a");
        pending.at(-1).reject(Object.assign(new Error("Falha"), { response: { status } }));
        await assert.rejects(request.promise);
        const next = solicitarMiniatura(1, "session-a");
        assert.equal(pending.at(-1).options.headers["If-None-Match"], undefined);
        respond(pending.at(-1));
        await next.promise;
    });
}

for (const invalidate of [
    request => request.liberar(),
    () => invalidarMiniaturas(),
    () => session.clear(),
    () => session.setItem("token", "session-b")
]) {
    test(`ignora respostas atrasadas após ${invalidate.toString()}`, async () => {
        const old = solicitarMiniatura(1, "session-a");
        const oldCall = pending[0];
        invalidate(old);
        assert.equal(oldCall.options.signal.aborted, true);
        if (!session.getItem("token")) session.setItem("token", "session-b");
        const token = session.getItem("token");
        const fresh = solicitarMiniatura(1, token);
        respond(pending[1], second, '"second"');
        assert.equal(await fresh.promise, second);
        respond(oldCall);
        await assert.rejects(old.promise);
        const next = solicitarMiniatura(1, token);
        assert.equal(pending[2].options.headers["If-None-Match"], '"second"');
        respond(pending[2], undefined, '"second"', 304);
        assert.equal(await next.promise, second);
    });
}

test("descarta entradas antigas ao atingir o limite de quantidade", async () => {
    for (let id = 1; id <= 129; id++) await load(id);
    const next = solicitarMiniatura(1, "session-a");
    assert.equal(pending.at(-1).options.headers["If-None-Match"], undefined);
    respond(pending.at(-1));
    await next.promise;
});

test("limita a soma dos Blobs e não armazena uma entrada maior que o orçamento", async () => {
    await load(1, new Blob([new Uint8Array(1024 * 1024)]));
    await load(2, new Blob([new Uint8Array(1024 * 1024 + 1)]));
    await load(3, new Blob([new Uint8Array(2 * 1024 * 1024 + 1)]));
    for (const id of [1, 3]) {
        const next = solicitarMiniatura(id, "session-a");
        assert.equal(pending.at(-1).options.headers["If-None-Match"], undefined);
        respond(pending.at(-1));
        await next.promise;
    }
});

test("original compartilha apenas a consulta pendente e preserva caches separados", async () => {
    await load();
    const a = solicitarFoto(1, "session-a");
    const b = solicitarFoto("1", "session-a");
    assert.equal(a.promise, b.promise);
    assert.equal(pending.length, 2);
    assert.match(pending[1].url, /\/photo$/);
    assert.equal(pending[1].options.headers["If-None-Match"], undefined);
    a.liberar();
    assert.equal(pending[1].options.signal.aborted, false);
    respond(pending[1], second, '"photo-version"');
    assert.equal(await b.promise, second);
    b.liberar();

    for (let i = 0; i < 3; i++) {
        const reopened = solicitarFoto(1, "session-a");
        assert.equal(pending.at(-1).options.headers["If-None-Match"], '"photo-version"');
        respond(pending.at(-1), undefined, '"photo-version"', 304);
        assert.equal(await reopened.promise, second);
        reopened.liberar();
    }
    const thumb = solicitarMiniatura(1, "session-a");
    assert.equal(pending.at(-1).options.headers["If-None-Match"], '"first"');
    respond(pending.at(-1), undefined, '"first"', 304);
    assert.equal(await thumb.promise, first);
});

test("negação elimina o original e logout descarta respostas atrasadas", async () => {
    const saved = solicitarFoto(7, "session-a");
    respond(pending.at(-1), first, '"photo-first"');
    await saved.promise;
    const denied = solicitarFoto(7, "session-a");
    pending.at(-1).reject(Object.assign(new Error("Privado"), {response:{status:404}}));
    await assert.rejects(denied.promise);
    const late = solicitarFoto(7, "session-a");
    const old = pending.at(-1);
    assert.equal(old.options.headers["If-None-Match"], undefined);
    session.clear();
    assert.equal(old.options.signal.aborted, true);
    session.setItem("token", "session-a");
    const fresh = solicitarFoto(7, "session-a");
    assert.equal(pending.at(-1).options.headers["If-None-Match"], undefined);
    respond(old, first, '"obsolete"');
    await assert.rejects(late.promise);
    respond(pending.at(-1), second, '"replacement"');
    assert.equal(await fresh.promise, second);
});

test("cache de originais respeita oito entradas e quatro MiB", async () => {
    for (let id = 1; id <= 9; id++) {
        const request = solicitarFoto(id, "session-a");
        respond(pending.at(-1), first);
        await request.promise;
    }
    const oldest = solicitarFoto(1, "session-a");
    assert.equal(pending.at(-1).options.headers["If-None-Match"], undefined);
    oldest.liberar();
    invalidarMiniaturas();
    const large = new Blob([new Uint8Array(1024 * 1024 + 1)]);
    for (let id = 1; id <= 4; id++) {
        const request = solicitarFoto(id, "session-a");
        respond(pending.at(-1), large);
        await request.promise;
    }
    const evicted = solicitarFoto(1, "session-a");
    assert.equal(pending.at(-1).options.headers["If-None-Match"], undefined);
    evicted.liberar();
    const retained = solicitarFoto(4, "session-a");
    assert.equal(pending.at(-1).options.headers["If-None-Match"], '"first"');
    respond(pending.at(-1), undefined, '"first"', 304);
    assert.equal(await retained.promise, large);
});

test("invalidação local limpa as duas imagens e cancela um original em andamento", async () => {
    await load(12);
    const firstPhoto = solicitarFoto(12, "session-a");
    respond(pending.at(-1), second, '"photo-old"');
    await firstPhoto.promise;
    const old = solicitarFoto(12, "session-a");
    const request = pending.at(-1);
    assert.equal(request.options.headers["If-None-Match"], '"photo-old"');

    invalidarMiniaturas();
    assert.equal(request.options.signal.aborted, true);
    const nextPhoto = solicitarFoto(12, "session-a");
    assert.equal(pending.at(-1).options.headers["If-None-Match"], undefined);
    respond(pending.at(-1), first, '"photo-new"');
    assert.equal(await nextPhoto.promise, first);
    const nextThumb = solicitarMiniatura(12, "session-a");
    assert.equal(pending.at(-1).options.headers["If-None-Match"], undefined);
    respond(pending.at(-1), first);
    assert.equal(await nextThumb.promise, first);
    respond(request, second, '"photo-old"', 304);
    await assert.rejects(old.promise);
});
