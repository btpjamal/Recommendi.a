export function mergeNotifications(current, incoming) {
    const rows = new Map(current.map(row => [row.id, row]));
    for (const row of incoming) {
        const previous = rows.get(row.id);
        rows.set(row.id, { ...row, lida: Boolean(row.lida || previous?.lida), lidaEm: previous?.lidaEm || row.lidaEm });
    }
    return [...rows.values()].sort((a, b) => b.id - a.id);
}
export function safeNotificationDestination(value) {
    if (typeof value !== "string") return null;
    return /^(?:\/community\?aba=conversas&caixa=RECEBIDAS|\/community\/conversations\/[1-9]\d*|\/community\/communities\/[1-9]\d*\?aba=posts&publicacao=[1-9]\d*&comentario=[1-9]\d*)$/.test(value) ? value : null;
}
