import { API_BASE_URL } from "../api/config.js";
import { session, subscribeSessionToken } from "../auth/session.js";

const idle = Object.freeze({ loading: false, progress: "", attempt: 0, result: null, error: "" });
const searches = new Map();
const listeners = new Set();
let notices = [];
const notify = () => listeners.forEach(listener => listener());

export function subscribeLocationSearch(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
}

subscribeSessionToken(() => {
    for (const search of searches.values()) {
        search.controller.abort();
        clearTimeout(search.expiry);
    }
    searches.clear();
    notices = [];
    notify();
});

export function getLocationSearch(hobbyId) {
    return searches.get(String(hobbyId))?.state ?? idle;
}

export function getLocationSearchNotices() {
    return notices;
}

export function markLocationSearchRead(hobbyId) {
    notices = notices.map(item => item.hobbyId === hobbyId ? { ...item, read: true } : item);
    notify();
}

export async function startLocationSearch(hobbyId, hobbyNome, modoAdministrador = false) {
    const key = String(hobbyId);
    const previous = searches.get(key);
    if (previous?.state.loading) return;
    clearTimeout(previous?.expiry);
    notices = notices.filter(item => item.search !== previous);

    const controller = new AbortController();
    const search = { controller, state: { ...idle, loading: true, progress: "Iniciando busca de locais..." } };
    searches.set(key, search);
    notify();
    const update = changes => {
        if (searches.get(key) !== search) return;
        search.state = { ...search.state, ...changes };
        notify();
    };
    const timeout = setTimeout(() => controller.abort(), 135_000);

    try {
        const token = session.getItem("token");
        const response = await fetch(`${API_BASE_URL}/hobbies/${hobbyId}/locations/stream`, {
            headers: { Accept: "text/event-stream", ...(token && { Authorization: `Bearer ${token}` }) },
            signal: controller.signal,
        });
        if (response.status === 429) throw new Error("Já existe uma busca em andamento ou o serviço está ocupado. Tente novamente em instantes.");
        if (!response.ok) throw new Error(`Erro HTTP ${response.status}`);
        if (!response.body) throw new Error("O navegador não disponibilizou o streaming.");

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        try {
            while (true) {
                const { value, done } = await reader.read();
                buffer += done ? decoder.decode() : decoder.decode(value, { stream: true });
                const blocks = buffer.split(/\r?\n\r?\n/);
                buffer = blocks.pop();
                for (const block of blocks) {
                    const lines = block.split(/\r?\n/);
                    const type = lines.find(line => line.startsWith("event:"))?.slice(6).trim();
                    const data = lines.filter(line => line.startsWith("data:"))
                        .map(line => line.slice(5).replace(/^ /, "")).join("\n");
                    if (type === "resultado") {
                        update({ result: JSON.parse(data), progress: "Busca concluída!" });
                        return;
                    }
                    if (type === "erro") throw new Error(data);
                    if (type === "progresso") {
                        const attempt = data.includes("servidor secundário") ? 2
                            : data.includes("Consultando servidor principal") ? 1 : search.state.attempt;
                        update({ progress: data, attempt });
                    }
                }
                if (done) throw new Error("A conexão terminou sem retornar os locais.");
            }
        } finally {
            void reader.cancel().catch(() => {});
            reader.releaseLock();
        }
    } catch (error) {
        update({
            error: controller.signal.aborted
                ? "A busca demorou demais. Tente novamente."
                : error.message || "Não foi possível consultar os locais.",
        });
    } finally {
        clearTimeout(timeout);
        if (searches.get(key) === search) {
            search.expiry = setTimeout(() => {
                if (searches.get(key) === search) {
                    searches.delete(key);
                    notices = notices.filter(item => item.search !== search);
                    notify();
                }
            }, 15 * 60_000);
            if (!modoAdministrador) {
                const { result, error } = search.state;
                const total = result?.locais?.length ?? 0;
                notices = [{
                    search,
                    hobbyId,
                    hobbyNome,
                    read: false,
                    createdAt: new Date().toISOString(),
                    message: error || (total > 0
                        ? `${total} ${total === 1 ? "local disponível" : "locais disponíveis"}.`
                        : result?.mensagem || "A busca terminou sem locais para exibir."),
                }, ...notices];
            }
            update({ loading: false });
        }
    }
}
