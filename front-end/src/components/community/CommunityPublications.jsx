import RecommendationBanner from "../recommendations/RecommendationBanner";
import NotificationBell from "../social/SocialNotifications.jsx";
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../../api/api.js";
import { session, revokeSession } from "../../auth/session.js";
import { createChatSession } from "./communityChatState.mjs";
import { createPostAttempt, mergePosts } from "./communityPostState.mjs";
import CommunityPostInteractions from "./CommunityPostInteractions.jsx";
import CommunityPostImage from "./CommunityPostImage.jsx";
import CommunityTabs from "./CommunityTabs.jsx";
import "../../pages/css/CommunityPosts.css";

const message = (error, fallback) => error.response?.data?.error || fallback;

export default function CommunityPublications({ communityId, active, onTabChange, targetPostId, targetCommentId }) {
    const navigate = useNavigate();
    const [community, setCommunity] = useState(null);
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [access, setAccess] = useState(false);
    const [more, setMore] = useState(false);
    const [cursor, setCursor] = useState(null);
    const [loadingMore, setLoadingMore] = useState(false);
    const [revision, setRevision] = useState(0);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const [text, setText] = useState("");
    const [file, setFile] = useState(null);
    const [preview, setPreview] = useState(null);
    const [editing, setEditing] = useState(null);
    const [removeImage, setRemoveImage] = useState(false);
    const currentSession = useRef(null);
    const mutationBusy = useRef(false);
    const pageBusy = useRef(false);
    const attempt = useRef(null);
    const editorRef = useRef(null);
    const base = `/community/communities/${communityId}`;
    const target = /^[1-9]\d*$/.test(targetPostId || "") ? targetPostId : null;

    const lostAccess = useCallback(() => {
        const current = currentSession.current;
        if (!current?.isActive()) return;
        current.commit(() => {
            setPosts([]); setCommunity(null); setAccess(false); setLoading(false); setMore(false);
            setLoadingMore(false); setBusy(false); setNotice("");
            setError("Seu acesso à comunidade está indisponível. Confira sua participação no Hub Social.");
        });
        current.close();
    }, []);

    const removeUnavailable = useCallback((id) => { setPosts(old => old.filter(p => p.id !== id)); }, []);

    useEffect(() => {
        if (!active) return;
        const current = createChatSession(); currentSession.current = current;
        mutationBusy.current = false; pageBusy.current = false;
        async function load() {
            current.commit(() => {
                setLoading(true); setAccess(false); setPosts([]); setCommunity(null);
                setMore(false); setCursor(null); setError(""); setLoadingMore(false); setBusy(false);
            });
            try {
                const [details, page, highlighted] = await Promise.all([
                    api.get(base, { signal: current.signal }),
                    api.get(base + "/posts", { params: { limite: 20 }, signal: current.signal }),
                    target ? api.get(base + `/posts/${target}`, { signal: current.signal }).catch(e => { if (e.response?.status === 404) return null; throw e; }) : Promise.resolve(null)
                ]);
                current.commit(() => {
                    setCommunity(details.data); setPosts(mergePosts(page.data.publicacoes, highlighted ? [highlighted.data] : []));
                    if (target && !highlighted) setNotice("A publicação mencionada não está mais disponível para você.");
                    setMore(page.data.temMais);
                    setCursor(page.data.proximoCursor); setAccess(true);
                });
            } catch (e) {
                if ([401, 403].includes(e.response?.status)) lostAccess();
                else current.commit(() => setError(message(e, "Não foi possível carregar as publicações.")));
            } finally { current.commit(() => setLoading(false)); }
        }
        load();
        return () => current.close();
    }, [active, base, revision, lostAccess, target]);

    useEffect(() => {
        if (!file) return;
        const reader = new FileReader();
        reader.onload = () => setPreview({ url: reader.result, file });
        reader.readAsDataURL(file);
        return () => { reader.onload = null; if (reader.readyState === 1) reader.abort(); };
    }, [file]);

    useEffect(() => {
        if (active && !loading && target) document.getElementById(`community-post-${target}`)?.scrollIntoView({ block: "start" });
    }, [active, loading, target, targetCommentId]);

    function fail(e, current, fallback) {
        if (!current.isActive()) return;
        if ([401, 403].includes(e.response?.status)) lostAccess();
        else current.commit(() => setError(message(e, fallback)));
    }

    async function loadMore() {
        const current = currentSession.current;
        if (!current?.isActive() || !access || pageBusy.current || mutationBusy.current || !more || !cursor) return;
        pageBusy.current = true; setLoadingMore(true); setError("");
        try {
            const { data } = await api.get(base + "/posts", { params: { antesDe: cursor, limite: 20 }, signal: current.signal });
            current.commit(() => {
                setPosts(old => mergePosts(old, data.publicacoes)); setMore(data.temMais); setCursor(data.proximoCursor);
            });
        } catch (e) { fail(e, current, "Não foi possível carregar as publicações anteriores."); }
        finally { current.commit(() => { pageBusy.current = false; setLoadingMore(false); }); }
    }

    function resetComposer() {
        setText(""); setFile(null); setEditing(null); setRemoveImage(false); attempt.current = null;
    }
    function selectFile(value) {
        if (!value) return;
        if (value.size > 1024 * 1024 || !["image/png", "image/jpeg"].includes(value.type)) {
            setError("Use uma imagem PNG ou JPEG de até 1 MB."); return;
        }
        setFile(value); setRemoveImage(false); setError("");
    }
    function edit(post) {
        if (mutationBusy.current || pageBusy.current) return;
        if ((text.trim() || file || editing) && !window.confirm("Substituir o rascunho atual pela publicação selecionada?")) return;
        setEditing(post); setText(post.conteudo); setFile(null); setRemoveImage(false); setError(""); setNotice("");
        editorRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    async function submit(event) {
        event.preventDefault();
        const current = currentSession.current;
        const content = text.trim();
        const hasImage = Boolean(file || (editing?.possuiImagem && !removeImage));
        if (!current?.isActive() || !access || mutationBusy.current || pageBusy.current || (!content && !hasImage)) return;
        mutationBusy.current = true; setBusy(true); setError(""); setNotice("");
        const body = new FormData();
        let payload;
        if (editing) payload = { conteudo: content, versao: editing.versao, removerImagem: removeImage };
        else {
            attempt.current = createPostAttempt(attempt.current, content, file);
            payload = { conteudo: content, clientId: attempt.current.clientId };
        }
        body.append("publicacao", new Blob([JSON.stringify(payload)], { type: "application/json" }));
        if (file) body.append("imagem", file);
        try {
            const { data } = editing
                ? await api.put(base + `/posts/${editing.id}`, body, { signal: current.signal })
                : await api.post(base + "/posts", body, { signal: current.signal });
            current.commit(() => {
                setPosts(old => mergePosts(old, [data])); setNotice(editing ? "Publicação atualizada." : "Publicação enviada."); resetComposer();
            });
        } catch (e) { fail(e, current, "Não foi possível salvar. Seu rascunho foi mantido; você pode tentar novamente."); }
        finally { current.commit(() => { mutationBusy.current = false; setBusy(false); }); }
    }

    async function remove(post) {
        if (mutationBusy.current || pageBusy.current || !window.confirm("Excluir esta publicação e sua imagem? Esta ação não pode ser desfeita.")) return;
        const current = currentSession.current;
        if (!current?.isActive() || !access) return;
        mutationBusy.current = true; setBusy(true); setError(""); setNotice("");
        try {
            await api.delete(base + `/posts/${post.id}`, { params: { versao: post.versao }, signal: current.signal });
            current.commit(() => {
                setPosts(old => old.filter(p => p.id !== post.id));
                if (editing?.id === post.id) resetComposer();
                setNotice("Publicação excluída.");
            });
        } catch (e) { fail(e, current, "Não foi possível excluir. Atualize a lista para conferir o estado da publicação."); }
        finally { current.commit(() => { mutationBusy.current = false; setBusy(false); }); }
    }

    async function logout() { await revokeSession(); navigate("/login"); }

    return <main className="community-details-page recommendations-theme social-screen-theme">
        <header className="app-header"><div className="app-header-content">
            <div className="app-brand"><div><span className="app-brand-name">Recommendi.a</span><span className="app-brand-subtitle">Pratique e compartilhe seus hobbies</span></div></div>
            <div className="app-header-actions"><NotificationBell /><span className="app-user-greeting">Olá, {session.getItem("nome")}!</span>
                <button className="app-header-button" onClick={() => navigate("/community")}>Comunidade</button>
                <button className="app-header-button" onClick={() => navigate("/recommendations")}>Recomendações</button>
                <button className="app-logout-button" onClick={logout}>Sair</button>
            </div>
        </div></header>
        <section className="community-details-container community-posts-container">
            <button className="community-back-button" onClick={() => navigate("/community")}>Voltar ao Hub Social</button>
            <RecommendationBanner title={community?.hobbyNome || "Publicações da comunidade"}
                description="Compartilhe descobertas, experiências e seus primeiros passos neste hobby."
                detail={community && `${community.membros} ${community.membros === 1 ? "participante" : "participantes"}`} />
            <CommunityTabs active="posts" onChange={onTabChange} disabled={busy} />
            <div className="community-post-status" aria-live="polite">{notice && <p role="status">{notice}</p>}</div>
            {error && <div className="community-chat-error" role="alert">{error}</div>}
            <div className="community-post-toolbar"><button type="button" disabled={busy || loadingMore || loading} onClick={() => setRevision(r => r + 1)}>Atualizar publicações</button>
                <p>Publicações mais recentes primeiro. Bloqueios ocultam publicações entre as pessoas envolvidas.</p></div>
            {loading ? <p role="status">Carregando publicações...</p> : !access ? <p>Verifique sua participação para acessar esta comunidade.</p> : <>
                <form ref={editorRef} className="community-post-composer" onSubmit={submit}>
                    <h2>{editing ? "Editar publicação" : "O que você quer compartilhar?"}</h2>
                    <label htmlFor={`post-text-${communityId}`}>Texto da publicação</label>
                    <textarea id={`post-text-${communityId}`} rows={5} maxLength={4000} value={text} disabled={busy}
                        onChange={e => setText(e.target.value)} placeholder="Uma conquista, uma dúvida, algo que você aprendeu..." />
                    <span className="community-post-counter">{text.length}/4000</span>
                    <label className="community-post-file">{editing?.possuiImagem ? "Substituir imagem" : "Adicionar imagem (opcional)"}
                        <input type="file" accept="image/png,image/jpeg" disabled={busy} onChange={e => { selectFile(e.target.files[0]); e.target.value = ""; }} /></label>
                    <small>PNG ou JPEG, até 1 MB e 16 megapixels. A imagem é salva junto com a publicação.</small>
                    {file && <div className="community-post-preview">
                        {preview?.file === file && <img src={preview.url} alt="Prévia da imagem selecionada" />}
                        <span>{file.name}</span><button type="button" disabled={busy} onClick={() => setFile(null)}>Retirar imagem selecionada</button>
                    </div>}
                    {editing?.possuiImagem && !file && <label className="community-post-checkbox"><input type="checkbox" disabled={busy} checked={removeImage} onChange={e => setRemoveImage(e.target.checked)} />Remover imagem atual</label>}
                    <div className="community-post-actions">
                        {(editing || text || file) && <button type="button" disabled={busy} onClick={resetComposer}>{editing ? "Cancelar edição" : "Limpar rascunho"}</button>}
                        <button className="community-post-primary" type="submit" disabled={busy || loadingMore || (!text.trim() && !file && !(editing?.possuiImagem && !removeImage))}>
                            {busy ? "Salvando..." : editing ? "Salvar alterações" : "Publicar"}</button>
                    </div>
                </form>
                <section className="community-post-feed" aria-label="Publicações da comunidade">
                    {posts.length === 0 ? <div className="community-post-empty"><h2>A primeira publicação pode ser sua</h2><p>Compartilhe algo sobre este hobby para começar a conversa.</p></div>
                        : posts.map(post => <article id={`community-post-${post.id}`} className={`community-post-card ${String(post.id) === target ? "community-post-notification-target" : ""}`} key={post.id}>
                            <header className="community-post-meta"><div><strong>{post.propria ? "Você" : post.autorNome}</strong>
                                <time dateTime={post.criadoEm}>{new Date(post.criadoEm).toLocaleString("pt-BR")}</time>
                                {post.editadoEm && <small title={new Date(post.editadoEm).toLocaleString("pt-BR")}>Editada</small>}</div>
                                {post.propria && <div className="community-post-actions"><button type="button" disabled={busy || loadingMore} onClick={() => edit(post)}>Editar</button>
                                    <button type="button" disabled={busy || loadingMore} onClick={() => remove(post)}>Excluir</button></div>}
                            </header>
                            {post.conteudo && <p className="community-post-text">{post.conteudo}</p>}
                            {active && <CommunityPostInteractions key={`${post.id}-${String(post.id) === target ? targetCommentId : ""}`} communityId={communityId} post={post} targetCommentId={String(post.id) === target ? targetCommentId : null} onAccessLost={lostAccess} onUnavailable={removeUnavailable} />}
                            {active && post.possuiImagem && <CommunityPostImage communityId={communityId} post={post} onAccessLost={lostAccess} />}
                        </article>)}
                    {more && <button className="community-post-load" type="button" disabled={loadingMore || busy} onClick={loadMore}>{loadingMore ? "Carregando..." : "Carregar publicações anteriores"}</button>}
                </section>
            </>}
        </section>
    </main>;
}
