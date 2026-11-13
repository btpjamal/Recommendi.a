import { API_BASE_URL } from "../api/config.js";

// Credentials live only in this page's memory. Reloading requires a new login.
const values = new Map();
const tokenListeners = new Set();

export function subscribeSessionToken(listener) {
    tokenListeners.add(listener);
    return () => tokenListeners.delete(listener);
}

function notifyTokenChange(previous) {
    if (previous !== values.get("token")) {
        tokenListeners.forEach(listener => listener());
    }
}

export const session = Object.freeze({
    getItem: key => values.get(key) ?? null,
    setItem(key, value) {
        const previous = values.get("token");
        const result = values.set(key, String(value));
        notifyTokenChange(previous);
        return result;
    },
    removeItem(key) {
        const previous = values.get("token");
        const result = values.delete(key);
        notifyTokenChange(previous);
        return result;
    },
    clear() {
        const previous = values.get("token");
        values.clear();
        notifyTokenChange(previous);
    },
});

// Remove sessions persisted by older releases without migrating their tokens.
try {
    for (const key of ["token", "userId", "nome", "email", "role"]) {
        globalThis.localStorage?.removeItem(key);
        globalThis.sessionStorage?.removeItem(key);
    }
} catch { /* Storage can be unavailable in restricted browsers. */ }

export async function revokeSession() {
    const token = session.getItem("token");
    try {
        if (token) {
            const response = await fetch(API_BASE_URL + "/auth/logout", {
                method: "POST", headers: { Authorization: "Bearer " + token },
                signal: AbortSignal.timeout(5000),
            });
            if (!response.ok && response.status !== 401) throw new Error("logout");
        }
    } catch {
        globalThis.alert?.("A sessão local foi encerrada. Não foi possível confirmar o encerramento no servidor; entre novamente para encerrar as outras sessões.");
    } finally {
        session.clear();
    }
}
