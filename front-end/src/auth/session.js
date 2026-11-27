import { API_BASE_URL } from "../api/config.js";

// Credentials live only in this page's memory. Reloading requires a new login.
const values = new Map();
const tokenListeners = new Set();

export function subscribeSessionToken(listener) {
    tokenListeners.add(listener);
    return () => tokenListeners.delete(listener);
}

let generation = 0;

export function sessionSnapshot() {
    return { generation, token: session.getItem("token") };
}

export function isCurrentSession(expected) {
    return generation === expected;
}

function renewSession() {
    const next = ++generation;
    tokenListeners.forEach(listener => listener());
    return next;
}

export function clearSessionIfCurrent(expected) {
    if (!isCurrentSession(expected)) return false;
    values.clear();
    return isCurrentSession(renewSession());
}

export const session = Object.freeze({
    getItem: key => values.get(key) ?? null,
    start(credentials) {
        values.clear();
        for (const [key, value] of Object.entries(credentials)) values.set(key, String(value));
        return renewSession();
    },
    setItem(key, value) {
        const result = values.set(key, String(value));
        if (key === "token" || key === "userId") renewSession();
        return result;
    },
    removeItem(key) {
        const result = values.delete(key);
        if (result && (key === "token" || key === "userId")) renewSession();
        return result;
    },
    clear() {
        values.clear();
        return renewSession();
    },
});

// Remove sessions persisted by older releases without migrating their tokens.
try {
    for (const key of ["token", "userId", "nome", "email", "role"]) {
        globalThis.localStorage?.removeItem(key);
        globalThis.sessionStorage?.removeItem(key);
    }
} catch { /* Storage can be unavailable in restricted browsers. */ }

export async function revokeSession(onComplete) {
    const token = session.getItem("token");
    const ended = session.clear();
    try {
        if (token) {
            const response = await fetch(API_BASE_URL + "/auth/logout", {
                method: "POST", headers: { Authorization: "Bearer " + token },
                signal: AbortSignal.timeout(5000),
            });
            if (!response.ok && response.status !== 401) throw new Error("logout");
        }
    } catch {
        if (isCurrentSession(ended)) globalThis.alert?.("A sessão local foi encerrada. Não foi possível confirmar o encerramento no servidor; entre novamente para encerrar as outras sessões.");
    }
    if (isCurrentSession(ended)) onComplete?.();
}
