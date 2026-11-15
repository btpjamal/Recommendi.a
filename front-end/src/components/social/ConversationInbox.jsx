import { useEffect, useState } from "react";
import { Link } from "react-router";
import { api } from "../../api/api.js";
import SocialAvatar from "./SocialAvatar.jsx";
import "./ConversationInbox.css";

function InboxPage({ page, loadPage, openRequests, onNewConversation }) {
    const [data, setData] = useState(null);
    const [error, setError] = useState("");
    const [revision, retry] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        let timer;
        let busy = false;
        async function refresh() {
            if (busy || controller.signal.aborted) return;
            clearTimeout(timer);
            if (document.hidden) return;
            busy = true;
            let again = true;
            try {
                const response = await api.get("/community/private-messages/inbox", {
                    params: { pagina: page }, signal: controller.signal,
                });
                if (!controller.signal.aborted) {
                    setData(response.data);
                    setError("");
                }
            } catch (e) {
                if (!controller.signal.aborted) setError("Não foi possível atualizar as conversas.");
                again = ![401, 403].includes(e.response?.status);
            } finally {
                busy = false;
                if (again && !controller.signal.aborted) timer = setTimeout(refresh, 15_000);
            }
        }
        document.addEventListener("visibilitychange", refresh);
        refresh();
        return () => {
            controller.abort();
            clearTimeout(timer);
            document.removeEventListener("visibilitychange", refresh);
        };
    }, [page, revision]);

    return (
        <>
            <div className="conversation-inbox-actions">
                <button type="button" onClick={openRequests}>
                    Solicitações{data?.solicitacoesPendentes > 0 && ` (${data.solicitacoesPendentes})`}
                </button>
                <button type="button" onClick={onNewConversation}>Nova conversa</button>
            </div>
            {error && <p role="status">{error} <button type="button" onClick={() => retry(v => v + 1)}>Tentar novamente</button></p>}
            {!data && !error && <p role="status">Carregando conversas...</p>}
            {data?.conversas.length === 0 && <p>{page === 0 ? "Você ainda não tem conversas aceitas." : "Nenhuma conversa nesta página."}</p>}
            {data?.conversas.length > 0 && (
                <ul className="conversation-inbox-list">
                    {data.conversas.map(item => (
                        <li key={item.contatoId} className="conversation-inbox-row">
                            <SocialAvatar usuario={item} ampliavel />
                            <Link to={`/community/conversations/${item.contatoId}`} className="conversation-inbox-link">
                                <div className="conversation-inbox-text">
                                    <strong>{item.nome}</strong>
                                    <span>{item.ultimaMensagem ? `${item.propria ? "Você: " : ""}${item.ultimaMensagem}` : "Conversa aceita. Envie a primeira mensagem."}</span>
                                </div>
                                <div className="conversation-inbox-meta">
                                    <time dateTime={item.atividade}>{new Date(item.atividade).toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" })}</time>
                                    {item.naoLidas > 0 && <span className="private-unread-badge" aria-label={`${item.naoLidas} mensagens não lidas`}>{item.naoLidas}</span>}
                                </div>
                            </Link>
                        </li>
                    ))}
                </ul>
            )}
            {(page > 0 || data?.temMais) && <div className="conversation-inbox-actions">
                <button type="button" disabled={page === 0} onClick={() => loadPage(page - 1)}>Anterior</button>
                <button type="button" disabled={!data?.temMais} onClick={() => loadPage(page + 1)}>Próxima</button>
            </div>}
        </>
    );
}

export default function ConversationInbox({ onCaixaChange, onNewConversation }) {
    const [page, setPage] = useState(0);
    return (
        <section className="conversation-inbox" aria-labelledby="conversation-inbox-title">
            <header className="conversation-inbox-header">
                <h1 id="conversation-inbox-title">Conversas</h1>
                <details>
                    <summary aria-label="Mais opções">⋯</summary>
                    <div className="conversation-inbox-menu">
                        <button type="button" onClick={() => onCaixaChange("HISTORICO")}>Histórico</button>
                        <button type="button" onClick={() => onCaixaChange("BLOQUEADOS")}>Bloqueados</button>
                    </div>
                </details>
            </header>
            <InboxPage key={page} page={page} loadPage={setPage} onNewConversation={onNewConversation} openRequests={() => onCaixaChange("RECEBIDAS")} />
        </section>
    );
}
