import { API_BASE_URL } from "./config.js";
import { clearSessionIfCurrent, isCurrentSession, sessionSnapshot } from "../auth/session.js";
import axios from "axios";
import { invalidarMiniaturas } from "../components/social/socialAvatarRequest.js";

export const api = axios.create({
    baseURL: API_BASE_URL,
});

const requests = new WeakMap();
const staleResponse = () => new axios.CanceledError("A sessão da requisição mudou.");

api.interceptors.request.use((config) => {
    const destination = new URL(api.getUri(config), window.location.href || window.location.origin);
    const server = new URL(API_BASE_URL + "/", window.location.origin);
    if (destination.origin !== server.origin) throw new Error("Destino de API não permitido.");
    const { generation, token } = sessionSnapshot();
    const login = config.method === "post" && destination.pathname.replace(/\/$/, "").endsWith("/auth/login");
    const supplied = config.headers.get("Authorization");
    if (!login && token && supplied && supplied !== `Bearer ${token}`) throw staleResponse();

    const authenticated = Boolean(token && !login);
    if (authenticated) config.headers.set("Authorization", `Bearer ${token}`, true);
    else config.headers.delete("Authorization");
    requests.set(config, { generation, authenticated });
    return config;
}, error => { throw error; }, { synchronous: true });

api.interceptors.response.use(response => {
    const request = requests.get(response.config);
    if (request && !isCurrentSession(request.generation)) throw staleResponse();
    const { method, url } = response.config;
    if ((method === "post" || method === "delete") &&
        /^\/community\/contacts\/(?:blocks\/\d+|\d+\/(?:accept|decline|cancel))$/.test(url)) {
        invalidarMiniaturas();
    }
    return response;
}, error => {
    if (axios.isCancel(error)) throw error;
    const request = requests.get(error.config);
    if (request && !isCurrentSession(request.generation)) throw staleResponse();

    if (request?.authenticated && error.response?.status === 401) {
        if (clearSessionIfCurrent(request.generation) && window.location.pathname !== "/login") {
            window.location.assign("/login");
        }
    }
    throw error;
});
