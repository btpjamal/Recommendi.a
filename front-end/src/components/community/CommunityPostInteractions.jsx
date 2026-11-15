import { useEffect, useRef, useState } from "react";
import { api } from "../../api/api.js";
import { createChatSession } from "./communityChatState.mjs";
import { createPostAttempt, mergePosts } from "./communityPostState.mjs";
import "../../pages/css/CommunityInteractions.css";

const reasons = [["SPAM", "Spam"], ["OFENSA", "Ofensa ou assédio"], ["CONTEUDO_INADEQUADO", "Conteúdo inadequado"], ["OUTRO", "Outro motivo"]];
const msg = (e, fallback) => e.response?.data?.error || fallback;

export default function CommunityPostInteractions({ communityId, post, onUnavailable, onAccessLost, targetCommentId, initialSummary = null }) {
    const base = `/community/communities/${communityId}/posts/${post.id}`;
    const [summary, setSummary] = useState(initialSummary);
    const target = /^[1-9]\d*$/.test(targetCommentId || "") ? targetCommentId : null;
    const [open, setOpen] = useState(Boolean(target));
    const [comments, setComments] = useState([]);
    const [cursor, setCursor] = useState(null);
    const [more, setMore] = useState(false);
    const [loading, setLoading] = useState(false);
    const [revision, setRevision] = useState(0);
    const [summaryRevision, setSummaryRevision] = useState(0);
    const [text, setText] = useState("");
    const [editing, setEditing] = useState(null);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const [report, setReport] = useState(null);
    const [reason, setReason] = useState("SPAM");
    const [details, setDetails] = useState("");
    const current = useRef(null);
    const occupied = useRef(false);
    const attempt = useRef(null);

    useEffect(() => {
        const session = createChatSession(); current.current = session; occupied.current = false;
        async function load() {
            session.commit(() => { setSummary(null); setBusy(false); setError(""); });
            try {
                const { data } = await api.get(base + "/interactions", { signal: session.signal });
                session.commit(() => setSummary(data));
            } catch (e) {
                if (!session.isActive()) return;
                if ([401, 403].includes(e.response?.status)) onAccessLost();
                else if (e.response?.status === 404) onUnavailable(post.id);
                else session.commit(() => setError(msg(e, "Não foi possível carregar as interações.")));
            }
        }
        if (initialSummary != null && summaryRevision === 0) {
            session.commit(() => { setSummary(initialSummary); setBusy(false); setError(""); });
        } else {
            load();
        }
        return () => session.close();
    }, [base, post.id, post.versao, summaryRevision, onUnavailable, onAccessLost, initialSummary]);

    useEffect(() => {
        if (!open) return;
        const controller = new AbortController();
        async function load() {
            setLoading(true);
            try {
                const [page, counts, highlighted] = await Promise.all([
                    api.get(base + "/comments", { params: { limite: 10 }, signal: controller.signal }),
                    api.get(base + "/interactions", { signal: controller.signal }),
                    target ? api.get(base + `/comments/${target}`, { signal: controller.signal }).catch(e => { if (e.response?.status === 404) return null; throw e; }) : Promise.resolve(null)
                ]);
                if (!controller.signal.aborted) {
                    setComments(mergePosts(page.data.comentarios, highlighted ? [highlighted.data] : []));
                    if (target && !highlighted) setNotice("O comentário mencionado não está mais disponível para você.");
                    setMore(page.data.temMais); setCursor(page.data.proximoCursor); setSummary(counts.data);
                }
            } catch (e) {
                if (controller.signal.aborted) return;
                if ([401, 403].includes(e.response?.status)) onAccessLost();
                else if (e.response?.status === 404) onUnavailable(post.id);
                else setError(msg(e, "Não foi possível carregar os comentários."));
            } finally { if (!controller.signal.aborted) setLoading(false); }
        }
        load(); return () => controller.abort();
    }, [open, base, revision, post.id, post.versao, onUnavailable, onAccessLost, target]);

    useEffect(() => {
        if (open && !loading && target) document.getElementById(`community-comment-${target}`)?.scrollIntoView({ block: "center" });
    }, [open, loading, target]);

    function fail(e, session, fallback, parent = false) {
        if (!session.isActive()) return;
        if ([401, 403].includes(e.response?.status)) onAccessLost();
        else if (parent && e.response?.status === 404) onUnavailable(post.id);
        else setError(msg(e, fallback));
    }
    async function refreshSummary(session) {
        try {
            const { data } = await api.get(base + "/interactions", { signal: session.signal });
            session.commit(() => setSummary(data));
        } catch (e) { fail(e, session, "A operação foi salva, mas as contagens não foram atualizadas.", true); }
    }
    async function act(operation) {
        const session = current.current;
        if (!session?.isActive() || occupied.current || loading) return;
        occupied.current = true; setBusy(true); setError(""); setNotice("");
        try { await operation(session); }
        finally { session.commit(() => { occupied.current = false; setBusy(false); }); }
    }
    function like() {
        act(async session => {
            try {
                const { data } = summary.curtiu
                    ? await api.delete(base + "/like", { signal: session.signal })
                    : await api.put(base + "/like", null, { signal: session.signal });
                session.commit(() => setSummary(data));
            } catch (e) { fail(e, session, "Não foi possível atualizar a curtida. Você pode tentar novamente.", true); }
        });
    }
    async function submit(e) {
        e.preventDefault(); const content = text.trim(); if (!content) return;
        act(async session => {
            try {
                let response;
                if (editing) response = await api.put(base + `/comments/${editing.id}`, { conteudo: content, versao: editing.versao }, { signal: session.signal });
                else {
                    attempt.current = createPostAttempt(attempt.current, content, null);
                    response = await api.post(base + "/comments", { conteudo: content, clientId: attempt.current.clientId }, { signal: session.signal });
                }
                session.commit(() => {
                    setComments(old => mergePosts(old, [response.data])); setText(""); setEditing(null); attempt.current = null;
                    setNotice(editing ? "Comentário atualizado." : "Comentário enviado.");
                });
                await refreshSummary(session);
            } catch (e) { fail(e, session, "Não foi possível salvar. Seu comentário foi mantido."); }
        });
    }
    function remove(comment) {
        if (!window.confirm("Excluir este comentário?")) return;
        act(async session => {
            try {
                await api.delete(base + `/comments/${comment.id}`, { params: { versao: comment.versao }, signal: session.signal });
                session.commit(() => {
                    setComments(old => old.filter(c => c.id !== comment.id));
                    if (editing?.id === comment.id) { setEditing(null); setText(""); attempt.current = null; }
                    setNotice("Comentário excluído.");
                });
                await refreshSummary(session);
            } catch (e) { fail(e, session, "Não foi possível excluir. Atualize os comentários."); }
        });
    }
    function edit(comment) {
        if ((text || editing) && !window.confirm("Substituir seu rascunho pelo comentário selecionado?")) return;
        setEditing(comment); setText(comment.conteudo); setError(""); attempt.current = null;
    }
    function loadMore() {
        if (!cursor || !more) return;
        act(async session => {
            try {
                const { data } = await api.get(base + "/comments", { params: { antesDe: cursor, limite: 10 }, signal: session.signal });
                session.commit(() => { setComments(old => mergePosts(old, data.comentarios)); setCursor(data.proximoCursor); setMore(data.temMais); });
            } catch (e) { fail(e, session, "Não foi possível carregar os comentários anteriores.", true); }
        });
    }
    function startReport(commentId = null, version = post.versao) { setReport({ commentId, version }); setReason("SPAM"); setDetails(""); setError(""); setNotice(""); }
    function submitReport(e) {
        e.preventDefault();
        act(async session => {
            try {
                const path = report.commentId == null ? "/reports" : `/comments/${report.commentId}/reports`;
                await api.post(base + path, { motivo: reason, detalhes: details, versao: report.version }, { signal: session.signal });
                session.commit(() => { setReport(null); setNotice("Denúncia registrada para análise. Sua identidade não será exibida ao autor."); });
            } catch (e) { fail(e, session, "Não foi possível registrar a denúncia."); }
        });
    }
    return <section className="post-interactions" aria-label="Interações da publicação">
        <div className="post-interaction-buttons">
            <button type="button" disabled={busy || loading || !summary} aria-pressed={Boolean(summary?.curtiu)} onClick={like}>
                {summary?.curtiu ? "Curtido" : "Curtir"}{summary && ` · ${summary.curtidas}`}</button>
            <button type="button" disabled={busy || !summary} aria-expanded={open} onClick={() => { setOpen(v => !v); setLoading(false); }}>Comentários{summary && ` · ${summary.comentarios}`}</button>
            {!post.propria && <button type="button" disabled={busy || loading} onClick={() => startReport()}>Denunciar publicação</button>}
        </div>
        {error && <p role="alert" className="interaction-error">{error}</p>}
        {!summary && error && <button type="button" disabled={busy} onClick={() => setSummaryRevision(v => v + 1)}>Tentar carregar interações</button>}
        {notice && <p role="status" className="interaction-success">{notice}</p>}
        {report && <form className="interaction-report" onSubmit={submitReport}>
            <h4>Denunciar {report.commentId == null ? "publicação" : "comentário"}</h4>
            <label>Motivo<select value={reason} disabled={busy} onChange={e => setReason(e.target.value)}>{reasons.map(([v, label]) => <option key={v} value={v}>{label}</option>)}</select></label>
            <label>Detalhes (opcional)<textarea rows={3} maxLength={500} value={details} disabled={busy} onChange={e => setDetails(e.target.value)} /></label>
            <div className="post-interaction-buttons"><button type="button" disabled={busy} onClick={() => setReport(null)}>Cancelar</button><button disabled={busy || loading}>Enviar denúncia</button></div>
        </form>}
        {open && <section className="community-comment-section">
            <div className="post-interaction-buttons"><strong>Comentários mais recentes</strong><button type="button" disabled={busy || loading} onClick={() => setRevision(v => v + 1)}>Atualizar comentários</button></div>
            {loading ? <p role="status">Carregando comentários...</p> : <>
                <form className="community-comment-form" onSubmit={submit}>
                    <label htmlFor={`comment-${post.id}`}>{editing ? "Editar comentário" : "Seu comentário"}</label>
                    <textarea id={`comment-${post.id}`} rows={3} maxLength={1000} disabled={busy} value={text} onChange={e => setText(e.target.value)} />
                    <small>{text.length}/1000</small>
                    <div className="post-interaction-buttons">
                        {editing && <button type="button" disabled={busy} onClick={() => { setEditing(null); setText(""); attempt.current = null; }}>Cancelar edição</button>}
                        <button disabled={busy || !text.trim()}>{busy ? "Salvando..." : editing ? "Salvar edição" : "Comentar"}</button>
                    </div>
                </form>
                {comments.length === 0 && <p>Ainda não há comentários visíveis.</p>}
                {comments.map(comment => <article id={`community-comment-${comment.id}`} key={comment.id} className={`community-comment ${String(comment.id) === target ? "community-comment-notification-target" : ""}`}>
                    <header><strong>{comment.propria ? "Você" : comment.autorNome}</strong> <time dateTime={comment.criadoEm}>{new Date(comment.criadoEm).toLocaleString("pt-BR")}</time>{comment.editadoEm && <small> · Editado</small>}</header>
                    <p>{comment.conteudo}</p>
                    <div className="post-interaction-buttons">{comment.propria ? <><button type="button" disabled={busy} onClick={() => edit(comment)}>Editar</button><button type="button" disabled={busy} onClick={() => remove(comment)}>Excluir</button></>
                        : <button type="button" disabled={busy} onClick={() => startReport(comment.id, comment.versao)}>Denunciar</button>}</div>
                </article>)}
                {more && <button type="button" disabled={busy} onClick={loadMore}>Carregar comentários anteriores</button>}
            </>}
        </section>}
    </section>;
}
