import test from "node:test";
import assert from "node:assert/strict";
import { mergeNotifications, safeNotificationDestination } from "./notificationState.mjs";
test("páginas sobrepostas não duplicam notificações", () => {
    assert.deepEqual(mergeNotifications([{ id: 3, lida: false }, { id: 2, lida: false }], [{ id: 2, lida: false }, { id: 1, lida: false }]).map(n => n.id), [3, 2, 1]);
});
test("resposta antiga não desfaz leitura confirmada", () => {
    assert.equal(mergeNotifications([{ id: 1, lida: true }], [{ id: 1, lida: false }])[0].lida, true);
});
test("destinos de contato e comentário são aceitos", () => {
    for (const path of ["/community?aba=conversas&caixa=RECEBIDAS", "/community/conversations/5", "/community/communities/3?aba=posts&publicacao=10&comentario=20"]) assert.equal(safeNotificationDestination(path), path);
});
test("destinos externos, administrativos e malformados são recusados", () => {
    for (const path of ["https://example.com", "//example.com", "/admin", "/community/conversations/-1", "/community/conversations/5?next=https://example.com", null]) assert.equal(safeNotificationDestination(path), null);
});
