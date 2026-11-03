const configured = import.meta.env?.VITE_API_BASE_URL;
export const API_BASE_URL = configured || (import.meta.env?.DEV ? "http://localhost:8080/api/v1" : "/api/v1");
if (/^https?:/i.test(API_BASE_URL)) {
    const url = new URL(API_BASE_URL);
    if (url.protocol !== "https:" && !["localhost", "127.0.0.1", "[::1]"].includes(url.hostname)) {
        throw new Error("A conexão com o servidor deve usar HTTPS.");
    }
}
