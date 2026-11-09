export function pendingSend(anterior, conteudo, uuid = () => crypto.randomUUID()) {
    return anterior?.conteudo === conteudo ? anterior : { conteudo, clientMessageId: uuid() };
}

export function shouldMarkRead({ visible, following, received, confirmed }) {
    return visible && following && received > confirmed;
}
