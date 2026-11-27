import axios from "axios";
import { API_BASE_URL } from "../../api/config.js";
import { session, subscribeSessionToken } from "../../auth/session.js";

function criarCache(tipo, maxEntradas, maxBytes) {
    const cache = new Map();
    const pendentes = new Map();
    let bytes = 0;

    function descartar(id) {
        const anterior = cache.get(id);
        if (!anterior) return;
        bytes -= anterior.blob.size;
        cache.delete(id);
    }

    function guardar(id, entrada) {
        descartar(id);
        if (!entrada.etag || entrada.blob.size > maxBytes) return;
        cache.set(id, entrada);
        bytes += entrada.blob.size;
        while (cache.size > maxEntradas || bytes > maxBytes) {
            descartar(cache.keys().next().value);
        }
    }

    function solicitar(userId, token) {
        if (!token || token !== session.getItem("token")) {
            return { promise: Promise.reject(new Error("Sessão inválida.")), liberar() {} };
        }

        const id = String(userId);
        let entrada = pendentes.get(id);
        if (!entrada) {
            const controller = new AbortController();
            const atual = { controller, consumidores: 0, promise: null };
            const anterior = cache.get(id);
            const headers = { Authorization: `Bearer ${token}` };
            if (anterior) headers["If-None-Match"] = anterior.etag;

            atual.promise = axios.get(
                `${API_BASE_URL}/social/profiles/${encodeURIComponent(id)}/${tipo}`,
                {
                    headers,
                    responseType: "blob",
                    signal: controller.signal,
                    validateStatus: status => status === 200 || status === 304
                }
            ).then(response => {
                if (pendentes.get(id) !== atual || controller.signal.aborted ||
                    token !== session.getItem("token")) {
                    throw new axios.CanceledError();
                }

                const etag = response.headers.etag;
                if (response.status === 304 && (!anterior || etag !== anterior.etag)) {
                    throw new Error("Versão da imagem não confirmada.");
                }
                const imagem = response.status === 304 ? anterior : { blob: response.data, etag };
                if (!(imagem.blob instanceof Blob) || !imagem.blob.size) {
                    throw new Error("Imagem inválida.");
                }
                guardar(id, imagem);
                return imagem.blob;
            }).catch(error => {
                if (pendentes.get(id) === atual) descartar(id);
                throw error;
            }).finally(() => {
                if (pendentes.get(id) === atual) pendentes.delete(id);
            });
            pendentes.set(id, atual);
            entrada = atual;
        }

        entrada.consumidores++;
        let liberado = false;
        return {
            promise: entrada.promise,
            liberar() {
                if (liberado) return;
                liberado = true;
                entrada.consumidores--;
                if (entrada.consumidores === 0 && pendentes.get(id) === entrada) {
                    pendentes.delete(id);
                    entrada.controller.abort();
                }
            }
        };
    }

    function cancelar() {
        const anteriores = [...pendentes.values()];
        pendentes.clear();
        for (const entrada of anteriores) entrada.controller.abort();
    }

    function limpar() {
        cache.clear();
        bytes = 0;
        cancelar();
    }

    return { solicitar, cancelar, limpar };
}

const miniaturas = criarCache("thumbnail", 128, 2 * 1024 * 1024);
// Keep a small set of recently opened originals within a separate memory budget.
const fotos = criarCache("photo", 8, 4 * 1024 * 1024);
const listeners = new Set();
let revision = 0;

function notificar() {
    revision++;
    listeners.forEach(listener => listener());
}

export function invalidarMiniaturas() {
    miniaturas.limpar();
    fotos.limpar();
    notificar();
}

export function subscribeMiniaturas(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
}

export function getThumbnailRevision() {
    return revision;
}

export const solicitarMiniatura = miniaturas.solicitar;
export const solicitarFoto = fotos.solicitar;

subscribeSessionToken(invalidarMiniaturas);
globalThis.window?.addEventListener("social-participation-changed", invalidarMiniaturas);
globalThis.window?.addEventListener("focus", () => {
    miniaturas.cancelar();
    notificar();
});
