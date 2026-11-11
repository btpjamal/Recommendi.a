export function mergePosts(current, incoming) {
    const map = new Map(current.map(post => [post.id, post]));
    for (const post of incoming) {
        const previous = map.get(post.id);
        if (!previous || post.versao >= previous.versao) map.set(post.id, post);
    }
    return [...map.values()].sort((a, b) => b.id - a.id);
}

export function createPostAttempt(previous, text, image, makeId = () => crypto.randomUUID()) {
    if (previous && previous.text === text && previous.image === image) return previous;
    return { text, image, clientId: makeId() };
}
