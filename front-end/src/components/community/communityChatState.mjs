export function mergeMessages(atuais, recebidas) {
    if (!recebidas.length) return atuais;
    const porId = new Map(atuais.map((mensagem) => [mensagem.id, mensagem]));
    for (const mensagem of recebidas) porId.set(mensagem.id, mensagem);
    return [...porId.values()].sort((a, b) => a.id - b.id);
}

export function createChatSession() {
    const controller = new AbortController();
    return {
        signal: controller.signal,
        isActive: () => !controller.signal.aborted,
        commit(callback) { if (!controller.signal.aborted) callback(); },
        close() { controller.abort(); }
    };
}
