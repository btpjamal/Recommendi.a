import RecommendationBanner from "../recommendations/RecommendationBanner";
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../../api/api.js";
import { createChatSession } from "../community/communityChatState.mjs";
import CommunityPostImage from "../community/CommunityPostImage.jsx";
import CommunityPostInteractions from "../community/CommunityPostInteractions.jsx";
import SocialFollowButton from "./SocialFollowButton.jsx";
import "../../pages/css/CommunityPosts.css";
import "./SocialFeed.css";

export default function SocialFeed() {
    const [filter, setFilter] = useState("todos");
    return <section className="social-feed-section" aria-label="Feed social">
        <RecommendationBanner title="Feed"
            description="Publicações das comunidades das quais você participa, das mais recentes às mais antigas." />
        <div className="community-segments community-scope-filters" role="group" aria-label="Filtro do feed">
            <button type="button" className={filter === "todos" ? "active" : ""} aria-pressed={filter === "todos"}
                onClick={() => setFilter("todos")}>Minhas comunidades</button>
            <button type="button" className={filter === "seguindo" ? "active" : ""} aria-pressed={filter === "seguindo"}
                onClick={() => setFilter("seguindo")}>Pessoas que sigo</button>
        </div>
        <FeedContent key={filter} following={filter === "seguindo"} />
    </section>;
}

function FeedContent({ following }) {
    const [items, setItems] = useState([]);
    const [cursor, setCursor] = useState(null);
    const [more, setMore] = useState(false);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState("");
    const [revision, setRevision] = useState(0);
    const current = useRef(null);
    const occupied = useRef(false);

    const refresh = useCallback(() => setRevision(v => v + 1), []);
    const remove = useCallback(id => setItems(old => old.filter(item => item.publicacao.id !== id)), []);

    useEffect(() => {
        window.addEventListener("social-follow-changed", refresh);
        return () => window.removeEventListener("social-follow-changed", refresh);
    }, [refresh]);

    useEffect(() => {
        const session = createChatSession();
        current.current = session;
        occupied.current = false;
        async function load() {
            setItems([]);
            setLoading(true);
            setLoadingMore(false);
            setMore(false);
            setCursor(null);
            setError("");
            try {
                const { data } = await api.get("/community/feed", {
                    params: { seguindo: following, limite: 20 }, signal: session.signal
                });
                session.commit(() => {
                    setItems(data.itens);
                    setMore(data.temMais);
                    setCursor(data.proximoCursor);
                });
            } catch (e) {
                session.commit(() => setError(e.response?.data?.error || "Não foi possível carregar o feed."));
            } finally {
                session.commit(() => setLoading(false));
            }
        }
        load();
        return () => session.close();
    }, [following, revision]);

    async function loadMore() {
        const session = current.current;
        if (!session?.isActive() || occupied.current || !more || !cursor) return;
        occupied.current = true;
        setLoadingMore(true);
        setError("");
        try {
            const { data } = await api.get("/community/feed", {
                params: { seguindo: following, antesDe: cursor, limite: 20 }, signal: session.signal
            });
            session.commit(() => {
                setItems(old => {
                    const rows = new Map(old.map(item => [item.publicacao.id, item]));
                    for (const item of data.itens) rows.set(item.publicacao.id, item);
                    return [...rows.values()].sort((a, b) => b.publicacao.id - a.publicacao.id);
                });
                setCursor(data.proximoCursor);
                setMore(data.temMais);
            });
        } catch (e) {
            session.commit(() => {
                if ([401, 403].includes(e.response?.status)) {
                    setItems([]);
                    setMore(false);
                }
                setError(e.response?.data?.error || "Não foi possível carregar as publicações anteriores.");
            });
        } finally {
            session.commit(() => { occupied.current = false; setLoadingMore(false); });
        }
    }

    return <>
        <div className="community-post-toolbar">
            <button type="button" disabled={loading || loadingMore} onClick={refresh}>Atualizar feed</button>
        </div>
        {error && <p role="alert" className="community-error">{error}</p>}
        {loading ? <p role="status">Carregando feed...</p> : <section className="community-post-feed">
            {!error && items.length === 0 && <div className="community-post-empty"><p>{following
                ? "Ainda não há publicações das pessoas que você segue nas suas comunidades."
                : "Ainda não há publicações disponíveis. Entre em comunidades para acompanhar e compartilhar experiências."}</p></div>}
            {items.map(item => <FeedCard key={item.publicacao.id} item={item} onUnavailable={remove} onAccessLost={refresh} />)}
            {more && <button type="button" className="community-post-load" disabled={loadingMore} onClick={loadMore}>
                {loadingMore ? "Carregando..." : "Carregar publicações anteriores"}</button>}
        </section>}
    </>;
}

function FeedCard({ item, onUnavailable, onAccessLost }) {
    const navigate = useNavigate();
    const post = item.publicacao;
    const scope = item.tipoComunidade === "MUNICIPAL" ? `${item.cidade} / ${item.estado}`
        : item.tipoComunidade === "REGIONAL" ? item.estado : "Para todos";
    return <article className="community-post-card">
        <div className="social-feed-community">
            <button type="button" onClick={() => navigate(`/community/communities/${post.communityId}?publicacao=${post.id}`)}>
                {item.hobbyNome} · {scope}
            </button>
        </div>
        <header className="community-post-meta">
            <div>
                <button type="button" className="social-feed-author" onClick={() => navigate(post.propria ? "/community/profile" : `/community/users/${post.autorId}`)}>
                    {post.propria ? "Você" : post.autorNome}
                </button>
                <time dateTime={post.criadoEm}>{new Date(post.criadoEm).toLocaleString("pt-BR")}</time>
                {post.editadoEm && <small>Editada</small>}
            </div>
            {!post.propria && <SocialFollowButton userId={post.autorId} initialFollowing={item.seguindo ?? null} />}
        </header>
        {post.conteudo && <p className="community-post-text">{post.conteudo}</p>}
        {post.possuiImagem && <CommunityPostImage communityId={post.communityId} post={post} onAccessLost={onAccessLost} />}
        <CommunityPostInteractions initialSummary={item.interacoes} communityId={post.communityId} post={post} onUnavailable={onUnavailable} onAccessLost={onAccessLost} />
    </article>;
}
