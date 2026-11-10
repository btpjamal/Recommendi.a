import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router";
import { api } from "../../api/api.js";
import { mergeNotifications, safeNotificationDestination } from "./notificationState.mjs";
import "./SocialNotifications.css";

const NotificationContext = createContext(null);

export function SocialNotificationsProvider({ children }) {
    const location = useLocation();
    const [count, setCount] = useState(0);
    const [available, setAvailable] = useState(false);
    const [error, setError] = useState("");
    const [revision, setRevision] = useState(0);
    const refresh = useCallback(() => setRevision(value => value + 1), []);
    useEffect(() => {
        const controller = new AbortController(); let timer; let busy = false;
        async function poll() {
            if (controller.signal.aborted || busy) return;
            clearTimeout(timer);
            if (document.hidden) { timer = setTimeout(poll, 15000); return; }
            busy = true; let repeat = true;
            try {
                const { data } = await api.get("/social/notifications/unread", { signal: controller.signal });
                if (!controller.signal.aborted) { setCount(data.naoLidas); setAvailable(true); setError(""); }
            } catch (e) {
                if (controller.signal.aborted) return;
                setCount(0);
                if ([401, 403].includes(e.response?.status)) { setAvailable(false); repeat = false; }
                else setError("Não foi possível atualizar as notificações.");
            } finally {
                busy = false;
                if (!controller.signal.aborted && repeat) timer = setTimeout(poll, 15000);
            }
        }
        const visible = () => { if (!document.hidden) poll(); };
        document.addEventListener("visibilitychange", visible);
        window.addEventListener("social-participation-changed", refresh);
        poll();
        return () => { clearTimeout(timer); controller.abort(); document.removeEventListener("visibilitychange", visible); window.removeEventListener("social-participation-changed", refresh); };
    }, [location.key, revision, refresh]);
    const value = useMemo(() => ({ count, available, error, refresh }), [count, available, error, refresh]);
    return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export default function NotificationBell() {
    const context = useContext(NotificationContext);
    const location = useLocation();
    return context?.available ? <NotificationBellPanel key={location.key} context={context} /> : null;
}

function NotificationBellPanel({ context }) {
    const { refresh } = context;
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);
    const [rows, setRows] = useState([]);
    const [cursor, setCursor] = useState(null);
    const [more, setMore] = useState(false);
    const [loading, setLoading] = useState(false);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [revision, setRevision] = useState(0);
    const root = useRef(null), button = useRef(null), panel = useRef(null);
    const controller = useRef(null), occupied = useRef(false);
    useEffect(() => {
        if (!open) return;
        const abort = new AbortController(); controller.current = abort; occupied.current = false;
        async function load() {
            setLoading(true); setError(""); setRows([]); setMore(false); setBusy(false);
            try {
                const { data } = await api.get("/social/notifications", { params: { limite: 20 }, signal: abort.signal });
                if (!abort.signal.aborted) { setRows(data.notificacoes); setCursor(data.proximoCursor); setMore(data.temMais); }
            } catch (e) { if (!abort.signal.aborted) { setError(e.response?.data?.error || "Não foi possível carregar as notificações."); if ([401, 403].includes(e.response?.status)) refresh(); } }
            finally { if (!abort.signal.aborted) setLoading(false); }
        }
        function close() { setOpen(false); button.current?.focus(); }
        function key(e) { if (e.key === "Escape") close(); }
        function outside(e) { if (!root.current?.contains(e.target)) close(); }
        panel.current?.querySelector("button")?.focus();
        document.addEventListener("keydown", key); document.addEventListener("pointerdown", outside);
        load();
        return () => { abort.abort(); document.removeEventListener("keydown", key); document.removeEventListener("pointerdown", outside); };
    }, [open, revision, refresh]);

    async function moreRows() {
        const abort = controller.current; if (!abort || abort.signal.aborted || occupied.current || !more || !cursor) return;
        occupied.current = true; setBusy(true); setError("");
        try {
            const { data } = await api.get("/social/notifications", { params: { antesDe: cursor, limite: 20 }, signal: abort.signal });
            if (!abort.signal.aborted) { setRows(old => mergeNotifications(old, data.notificacoes)); setCursor(data.proximoCursor); setMore(data.temMais); }
        } catch (e) { if (!abort.signal.aborted) setError(e.response?.data?.error || "Não foi possível carregar avisos anteriores."); }
        finally { if (!abort.signal.aborted) { occupied.current = false; setBusy(false); } }
    }
    async function read(row, visit = false) {
        const abort = controller.current; if (!abort || abort.signal.aborted || occupied.current || loading) return;
        const destination = safeNotificationDestination(row.destino);
        if (visit && !destination) { setError("O destino desta notificação é inválido."); return; }
        occupied.current = true; setBusy(true); setError("");
        try {
            await api.put(`/social/notifications/${row.id}/read`, null, { signal: abort.signal });
            if (abort.signal.aborted) return;
            setRows(old => old.map(item => item.id === row.id ? { ...item, lida: true } : item)); refresh();
            if (visit) { setOpen(false); navigate(destination); }
        } catch (e) {
            if (abort.signal.aborted) return;
            if (e.response?.status === 404) { setRows(old => old.filter(item => item.id !== row.id)); refresh(); }
            else if ([401, 403].includes(e.response?.status)) refresh();
            setError(e.response?.data?.error || "Não foi possível marcar como lida. Tente novamente.");
        } finally { if (!abort.signal.aborted) { occupied.current = false; setBusy(false); } }
    }
    return <div className="social-notifications" ref={root}>
        <button ref={button} className="social-notification-bell" type="button" aria-expanded={open} aria-haspopup="dialog"
            aria-label={`Notificações, ${context.count} não lidas`} title={context.error || "Notificações"} onClick={() => setOpen(value => !value)}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></svg>
            {context.count > 0 && <span className="social-notification-badge">{context.count > 99 ? "99+" : context.count}</span>}
        </button>
        {open && <section className="social-notification-panel" ref={panel} role="dialog" aria-label="Notificações">
            <header><h2>Notificações</h2><button type="button" onClick={() => { setOpen(false); button.current?.focus(); }}>Fechar</button></header>
            <button className="social-notification-refresh" type="button" disabled={loading || busy} onClick={() => { setRevision(v => v + 1); refresh(); }}>Atualizar</button>
            {(error || context.error) && <p className="social-notification-error" role="alert">{error || context.error}</p>}
            {loading ? <p role="status">Carregando notificações...</p> : rows.length === 0 ? <p>Nenhuma notificação disponível.</p> : <ul>
                {rows.map(row => <li key={row.id} className={row.lida ? "read" : "unread"}>
                    <button className="social-notification-destination" type="button" disabled={busy} onClick={() => read(row, true)}>
                        <span>{row.mensagem}</span><time dateTime={row.criadoEm}>{new Date(row.criadoEm).toLocaleString("pt-BR")}</time>
                        {!row.lida && <small>Não lida</small>}
                    </button>
                    {!row.lida && <button className="social-notification-read" type="button" disabled={busy} onClick={() => read(row)}>Marcar como lida</button>}
                </li>)}
            </ul>}
            {more && <button type="button" disabled={busy || loading} onClick={moreRows}>{busy ? "Carregando..." : "Carregar anteriores"}</button>}
        </section>}
    </div>;
}

