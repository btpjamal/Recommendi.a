import test from "node:test";
import assert from "node:assert/strict";
import { mergePosts, createPostAttempt } from "./communityPostState.mjs";

test("publicações sobrepostas são deduplicadas e ordenadas por ID decrescente", () => {
    assert.deepEqual(mergePosts([{ id: 3, versao: 0 }, { id: 2, versao: 0 }], [{ id: 2, versao: 0 }, { id: 4, versao: 0 }]).map(p => p.id), [4, 3, 2]);
});
test("página atrasada não substitui edição mais recente", () => {
    const current = { id: 1, versao: 2, conteudo: "Editado" };
    assert.deepEqual(mergePosts([current], [{ id: 1, versao: 1, conteudo: "Antigo" }]), [current]);
});
test("repetir envio com texto e arquivo iguais reutiliza UUID", () => {
    const image = {};const first = createPostAttempt(null, "Texto", image, () => "a");
    assert.equal(createPostAttempt(first, "Texto", image, () => "b"), first);
});
test("alterar texto ou imagem cria outra intenção de publicação", () => {
    const first = createPostAttempt(null, "Texto", null, () => "a");
    assert.equal(createPostAttempt(first, "Outro", null, () => "b").clientId, "b");
    assert.equal(createPostAttempt(first, "Texto", {}, () => "c").clientId, "c");
});
