import test from "node:test";
import assert from "node:assert/strict";
import { pendingSend, shouldMarkRead } from "./privateChatState.mjs";

test("nova tentativa do mesmo rascunho reutiliza o identificador", () => {
    const pedido = pendingSend(null, "Olá", () => "primeiro");
    assert.equal(pendingSend(pedido, "Olá", () => "outro").clientMessageId, "primeiro");
});
test("editar o texto gera um novo envio", () => {
    const pedido = pendingSend(null, "Olá", () => "primeiro");
    assert.equal(pendingSend(pedido, "Outro texto", () => "segundo").clientMessageId, "segundo");
});
test("aba oculta ou leitura do historico nao marca novidades como lidas", () => {
    assert.equal(shouldMarkRead({ visible: false, following: true, received: 10, confirmed: 0 }), false);
    assert.equal(shouldMarkRead({ visible: true, following: false, received: 10, confirmed: 0 }), false);
});
test("leitura avanca somente ate o ponto recebido e ainda nao confirmado", () => {
    assert.equal(shouldMarkRead({ visible: true, following: true, received: 10, confirmed: 9 }), true);
    assert.equal(shouldMarkRead({ visible: true, following: true, received: 10, confirmed: 10 }), false);
    assert.equal(shouldMarkRead({ visible: true, following: true, received: 9, confirmed: 10 }), false);
});
