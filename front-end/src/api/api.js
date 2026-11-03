import { API_BASE_URL } from "./config.js";
import { session } from "../auth/session.js";
import axios from "axios";

export const api = axios.create({
    baseURL: API_BASE_URL,
});

api.interceptors.request.use((config) => {
    const destination = new URL(config.url || "", new URL(API_BASE_URL + "/", window.location.origin));
    const server = new URL(API_BASE_URL + "/", window.location.origin);
    if (destination.origin !== server.origin) throw new Error("Destino de API não permitido.");
    const token = session.getItem("token");

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

api.interceptors.response.use(response => response, error => {
    if (error.response?.status === 401) {
        session.clear();
        if (!window.location.pathname.startsWith("/login")) window.location.assign("/login");
    }
    return Promise.reject(error);
});
