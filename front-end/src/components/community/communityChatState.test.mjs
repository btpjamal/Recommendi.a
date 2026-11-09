import test from "node:test";
import assert from "node:assert/strict";
import { mergeMessages, createChatSession } from "./communityChatState.mjs";

test("polling seguido da resposta de envio nao duplica a mensagem", () => {
    const polling = mergeMessages([{ id: 1 }], [{ id: 2, conteudo: "Oi" }]);
    assert.deepEqual(mergeMessages(polling, [{ id: 2, conteudo: "Oi" }]).map((m) => m.id), [1, 2]);
});
test("envio antes do polling preserva mensagens intermediarias e ordena por ID", () => {
    const envio = mergeMessages([{ id: 1 }], [{ id: 3 }]);
    assert.deepEqual(mergeMessages(envio, [{ id: 2 }, { id: 3 }]).map((m) => m.id), [1, 2, 3]);
});
test("historico sobreposto nao duplica nem remove mensagens recentes", () => {
    assert.deepEqual(mergeMessages([{ id: 3 }, { id: 4 }], [{ id: 1 }, { id: 2 }, { id: 3 }]).map((m) => m.id), [1, 2, 3, 4]);
});
test("resposta atrasada da comunidade anterior nao altera a tela atual", async () => {
    const anterior = createChatSession();
    const atual = createChatSession();
    let resolver;
    const resposta = new Promise((resolve) => { resolver = resolve; });
    let tela = [];
    const requisicao = resposta.then((dados) => anterior.commit(() => { tela = dados; }));
    anterior.close();
    atual.commit(() => { tela = [{ id: 20 }]; });
    resolver([{ id: 1 }]);
    await requisicao;
    assert.deepEqual(tela, [{ id: 20 }]);
    assert.equal(anterior.signal.aborted, true);
    atual.close();
});
test("perda de acesso impede atualizacoes posteriores", () => {
    const sessao = createChatSession();
    let conteudo = "historico anterior";
    sessao.commit(() => { conteudo = ""; });
    sessao.close();
    sessao.commit(() => { conteudo = "resposta antiga"; });
    assert.equal(conteudo, "");
});
