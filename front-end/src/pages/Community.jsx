import RecommendationBanner from "../components/recommendations/RecommendationBanner";
import "./css/CommunityVisual.css";
import SocialFeed from "../components/social/SocialFeed.jsx";
import SocialFollowButton from "../components/social/SocialFollowButton.jsx";
import NotificationBell from "../components/social/SocialNotifications.jsx";
import { api } from "../api/api.js";
import { session, revokeSession } from "../auth/session.js";
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useOutletContext, useSearchParams } from "react-router";

import "./css/Community.css";
import "./css/RecommendationsLayout.css";
import "./css/SocialLocation.css";
import SocialAvatar from "../components/social/SocialAvatar.jsx";
import SocialContactRequestButton from "../components/social/SocialContactRequestButton";
import SocialContactsPanel from "../components/social/SocialContactsPanel";
import usePrivateUnread from "../components/social/usePrivateUnread.js";


function useCommunityResource(path, fallback) {
    const [revision, setRevision] = useState(0);
    const [result, setResult] = useState(null);
    const request = useMemo(() => path ? { path, revision, fallback } : null, [path, revision, fallback]);

    useEffect(() => {
        if (!request) return;
        const controller = new AbortController();
        api.get(request.path, { signal: controller.signal }).then(({ data }) => {
            if (!controller.signal.aborted) setResult({ request, data });
        }).catch(error => {
            if (!controller.signal.aborted) {
                setResult({ request, error: error.response?.data?.error || request.fallback });
            }
        });
        return () => controller.abort();
    }, [request]);

    const current = request && result?.request === request ? result : null;
    return {
        data: current?.data ?? null,
        error: current?.error,
        loading: Boolean(path && !current),
        retry: () => setRevision(value => value + 1),
        update: change => setResult(previous =>
            previous?.request === request && previous?.data != null
                ? { ...previous, data: change(previous.data) } : previous),
    };
}

function LocationFeedback({ loading, error, retry }) {
    if (loading) return <p role="status">Carregando localização...</p>;
    if (!error) return null;
    return <div role="alert">
        <p>{error}</p>
        <button type="button" onClick={retry}>Tentar localização novamente</button>
    </div>;
}

export default function Community() {

    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const { mobile, unread } = useOutletContext();

    const [resultadoPessoas, setResultadoPessoas] = useState(null);
    const [paginaPessoas, setPaginaPessoas] = useState(0);
    const [erro, setErro] = useState("");
    const [ativando, setAtivando] = useState(false);
    const aba = searchParams.get("aba");
    const caixa = searchParams.get("caixa");
    const abaAtiva = ["feed", "pessoas", "comunidades", "conversas"].includes(aba) ? aba : "pessoas";
    const caixaContato = ["RECEBIDAS", "ENVIADAS", "ACEITAS", "HISTORICO", "BLOQUEADOS"].includes(caixa) ? caixa : "RECEBIDAS";

    function setAbaAtiva(value) {
        const next = new URLSearchParams(searchParams);
        next.set("aba", value);
        setSearchParams(next);
    }

    function setCaixaContato(value) {
        const next = new URLSearchParams(searchParams);
        next.set("aba", "conversas");
        next.set("caixa", value);
        setSearchParams(next);
    }

    function abrirSolicitacao(contato) {
        setCaixaContato(contato.status === "ACEITA" ? "ACEITAS" : contato.direcao === "RECEBIDA" ? "RECEBIDAS" : "ENVIADAS");
    }

    function participanteBloqueado() {
        setRevisaoLocal(atual => atual + 1);
        setCaixaContato("BLOQUEADOS");
    }

    const [alterandoComunidade, setAlterandoComunidade] = useState(null);
    const [tipoComunidade, setTipoComunidade] = useState("geral");
    const [filtroPessoas, setFiltroPessoas] = useState("GERAL");
    const [salvandoVisibilidade, setSalvandoVisibilidade] = useState(false);
    const [erroVisibilidade, setErroVisibilidade] = useState("");
    const [revisaoLocal, setRevisaoLocal] = useState(0);
    const statusRequest = useCommunityResource("/social/me/status", "Não foi possível carregar o status da comunidade.");
    const { data: status, loading: carregando, error: erroStatus, retry: retryStatus } = statusRequest;
    const participa = Boolean(status?.elegivel && status?.participa);
    const locationRequest = useCommunityResource(participa && (abaAtiva === "pessoas" || abaAtiva === "comunidades")
        ? "/community/location" : null, "Não foi possível carregar sua localização.");
    const { data: configLocal, loading: carregandoLocal, error: erroLocal, retry: retryLocal } = locationRequest;
    const catalogRequest = useCommunityResource(participa && abaAtiva === "comunidades"
        ? "/community/communities" + (tipoComunidade === "geral" ? "" : "/" + tipoComunidade)
        : null, "Não foi possível carregar as comunidades.");
    const { data: catalogo, loading: carregandoRegiao, error: erroRegiao, retry: retryCatalogo } = catalogRequest;
    const operacoes = useRef(null);
    useEffect(() => {
        const controller = new AbortController();
        operacoes.current = controller;
        return () => controller.abort();
    }, []);

    const unreadDesktop = usePrivateUnread(!mobile && Boolean(status?.elegivel && status?.participa), revisaoLocal);
    const naoLidas = mobile ? unread : unreadDesktop;

    const nome = session.getItem("nome");

    const pessoasAtiva = Boolean(status?.elegivel && status?.participa && abaAtiva === "pessoas");
    const consultaPessoas = useMemo(() => pessoasAtiva ? {
        escopo: filtroPessoas, pagina: paginaPessoas, revisao: revisaoLocal
    } : null, [pessoasAtiva, filtroPessoas, paginaPessoas, revisaoLocal]);
    const resultadoAtual = resultadoPessoas?.consulta === consultaPessoas ? resultadoPessoas : null;
    const usuarios = resultadoAtual?.pessoas ?? [];
    const maisPessoas = resultadoAtual?.temMais ?? false;
    const carregandoPessoas = pessoasAtiva && !resultadoAtual;
    const erroPessoas = resultadoAtual?.erro ?? "";

    useEffect(() => {
        if (!consultaPessoas) return;
        const controller = new AbortController();
        api.get("/community/users/page", {
            params: { escopo: consultaPessoas.escopo, pagina: consultaPessoas.pagina },
            signal: controller.signal
        }).then((resposta) => {
            if (!controller.signal.aborted) {
                setResultadoPessoas({ consulta: consultaPessoas, ...resposta.data });
            }
        }).catch((error) => {
            if (!controller.signal.aborted) setResultadoPessoas({
                consulta: consultaPessoas,
                erro: error.response?.data?.error || "Não foi possível carregar as pessoas."
            });
        });
        return () => controller.abort();
    }, [consultaPessoas]);

    async function alterarVisibilidade(compartilharRegiao) {
        const signal = operacoes.current?.signal;
        if (salvandoVisibilidade) return;
        setSalvandoVisibilidade(true);
        setErroVisibilidade("");
        try {
            const resposta = await api.put("/community/location-visibility", {
                compartilharRegiao
            }, { signal });
            if (!signal?.aborted) locationRequest.update(() => resposta.data);
        } catch (error) {
            if (signal?.aborted) return;
            setErroVisibilidade(error.response?.data?.error || "Não foi possível salvar sua escolha.");
        } finally {
            setSalvandoVisibilidade(false);
        }
    }


    async function ativarParticipacao() {
        const signal = operacoes.current?.signal;

        try {

            setAtivando(true);
            setErro("");

            await api.post(
                "/social/me/enable",
                {},
                {
                    signal
                }
            );

            if (signal?.aborted) return;
            window.dispatchEvent(new Event("social-participation-changed"));
            retryStatus();

        } catch (error) {
            if (signal?.aborted) return;


            setErro(
                error.response?.data?.error ||
                "Não foi possível ativar sua participação."
            );

        } finally {
            setAtivando(false);
        }
    }


    async function logout() {
        await revokeSession();
        navigate("/login");
    }

    async function entrarComunidade(comunidade) {
        const signal = operacoes.current?.signal;
        const caminho = comunidade.id ?? `catalog/${comunidade.tipo}/${comunidade.hobbyId}`;

        try {
            setAlterandoComunidade(chaveComunidade(comunidade));

            const response = await api.post(
                `/community/communities/${caminho}/join`,
                {},
                {
                    signal
                }
            );

            if (!signal?.aborted) atualizarComunidade(response.data);

        } catch (error) {
            if (signal?.aborted) return;


            setErro(
                error.response?.data?.error ||
                "Não foi possível entrar na comunidade."
            );

        } finally {
            setAlterandoComunidade(null);
        }
    }


    async function sairComunidade(comunidade) {
        const signal = operacoes.current?.signal;
        const communityId = comunidade.id;

        try {
            setAlterandoComunidade(chaveComunidade(comunidade));

            const response = await api.delete(
                `/community/communities/${communityId}/leave`,
                {
                    signal
                }
            );

            if (!signal?.aborted) atualizarComunidade(response.data);

        } catch (error) {
            if (signal?.aborted) return;


            setErro(
                error.response?.data?.error ||
                "Não foi possível sair da comunidade."
            );

        } finally {
            setAlterandoComunidade(null);
        }
    }


    function atualizarComunidade(comunidadeAtualizada) {
        catalogRequest.update(lista => lista.map(comunidade =>
            chaveComunidade(comunidade) === chaveComunidade(comunidadeAtualizada)
                ? comunidadeAtualizada : comunidade));
    }

    const comunidadesExibidas = catalogo ?? [];

    return (
        <main className="community-page recommendations-theme community-visual">

            <header className="app-header">

                <div className="app-header-content">

                    <div className="app-brand">
                        <span className="app-brand-icon">✦</span>

                        <div>
                            <span className="app-brand-name">
                                Recommendi.a
                            </span>

                            <span className="app-brand-subtitle">
                                Descubra seu próximo hobby
                            </span>
                        </div>
                    </div>


                    <div className="app-header-actions"><NotificationBell />

                        <span className="app-user-greeting">
                            Olá, {nome}!
                        </span>

                        <button
                            className="app-header-button"
                            onClick={() =>
                                navigate("/recommendations")
                            }
                        >
                            Recomendações
                        </button>

                        <button
                            className="app-header-button"
                            onClick={() =>
                                navigate("/myProfile")
                            }
                        >
                            Meu Perfil
                        </button>

                        <button
                            className="app-logout-button"
                            onClick={logout}
                        >
                            Sair
                        </button>

                    </div>

                </div>

            </header>


            <section className="community-container">

                {carregando && (
                    <div className="community-state">
                        <div className="community-state-icon">
                            ◌
                        </div>

                        <h2>Carregando comunidade...</h2>

                        <p>
                            Estamos preparando seu Hub Social.
                        </p>
                    </div>
                )}


                {!carregando && erroStatus && (
                    <div className="community-error" role="alert">
                        <p>{erroStatus}</p>
                        <button type="button" onClick={retryStatus}>Tentar novamente</button>
                    </div>
                )}
                {!carregando && erro && (
                    <div className="community-error">
                        {erro}
                    </div>
                )}


                {!carregando &&
                    status &&
                    !status.elegivel && (

                        <div className="community-state">

                            <div className="community-state-icon">
                                ◇
                            </div>

                            <h2>
                                Comunidade indisponível
                            </h2>

                            <p>
                                O Hub Social do Recommendi.a está
                                disponível somente para usuários
                                maiores de 18 anos.
                            </p>

                            <button
                                className="community-secondary-button"
                                onClick={() =>
                                    navigate("/recommendations")
                                }
                            >
                                Voltar às recomendações
                            </button>

                        </div>
                    )}


                {!carregando &&
                    status?.elegivel &&
                    !status.participa && (

                        <div className="community-state">

                            <div className="community-state-icon">
                                ♡
                            </div>

                            <h2>
                                Faça parte da comunidade
                            </h2>

                            <p>
                                Conheça pessoas com hobbies e
                                interesses semelhantes aos seus.
                                Sua participação é opcional e você
                                pode sair quando quiser.
                            </p>

                            <button
                                className="community-primary-button"
                                onClick={ativarParticipacao}
                                disabled={ativando}
                            >
                                {ativando
                                    ? "Entrando..."
                                    : "Participar da comunidade"}
                            </button>

                        </div>
                    )}


                {!carregando &&
                    status?.elegivel &&
                    status.participa && (

                        <>
                            <div className="community-suggestions">
                                <div className="community-suggestions-info">
                                    <span className="community-label">
                                        CONTRIBUA COM O RECOMMENDI.A
                                    </span>

                                    <h2>Tem uma ideia de hobby?</h2>

                                    <p>
                                        Sugira novas atividades para o catálogo
                                        e acompanhe a análise das suas sugestões.
                                    </p>
                                </div>

                                <div className="community-suggestions-actions">
                                    <button
                                        type="button"
                                        className="community-primary-button"
                                        onClick={() => navigate("/suggest-hobby")}
                                    >
                                        + Sugerir hobby
                                    </button>

                                    <button
                                        type="button"
                                        className="community-secondary-button"
                                        onClick={() => navigate("/my-suggestions")}
                                    >
                                        Minhas sugestões
                                    </button>
                                </div>
                            </div>

                            <button className="community-secondary-button community-social-profile-link" onClick={() => navigate("/community/profile")}>Meu perfil social</button>
                            <div className="community-tabs social-hub-tabs community-segments">
                                <button data-mobile-shortcut className={abaAtiva === "feed" ? "active" : ""} aria-pressed={abaAtiva === "feed"} onClick={() => setAbaAtiva("feed")}>
                                    Feed
                                </button>

                                <button
                                    className={abaAtiva === "pessoas" ? "active" : ""} aria-pressed={abaAtiva === "pessoas"}
                                    onClick={() => setAbaAtiva("pessoas")}
                                >
                                    Pessoas
                                </button>

                                <button
                                    className={abaAtiva === "comunidades" ? "active" : ""} aria-pressed={abaAtiva === "comunidades"}
                                    onClick={() => setAbaAtiva("comunidades")}
                                >
                                    Comunidades
                                </button>
                                <button data-mobile-shortcut className={abaAtiva === "conversas" ? "active" : ""} aria-pressed={abaAtiva === "conversas"}
                                    onClick={() => setAbaAtiva("conversas")}>
                                    Conversas
                                    {naoLidas.total > 0 && <span className="private-unread-badge" aria-label={`${naoLidas.total} mensagens não lidas`}>{naoLidas.total}</span>}
                                </button>

                            </div>

                            {abaAtiva === "feed" && <SocialFeed />}
                            {abaAtiva === "pessoas" && (
                                <>
                                    <RecommendationBanner title="Encontrar pessoas"
                                        description="Descubra participantes que compartilham hobbies e interesses com você." />

                                    <div className="community-segments community-scope-filters" role="group" aria-label="Abrangência da busca de pessoas">
                                        {[
                                            ["GERAL", "Geral", true],
                                            ["ESTADUAL", "No meu estado", configLocal?.estadoDisponivel],
                                            ["MUNICIPAL", "Na minha cidade", configLocal?.cidadeDisponivel],
                                        ].map(([value, label, available]) => (
                                            <button key={value} type="button" aria-pressed={filtroPessoas === value}
                                                disabled={!available} onClick={() => {
                                                    setPaginaPessoas(0);
                                                    setFiltroPessoas(value);
                                                }}>
                                                {label}
                                            </button>
                                        ))}
                                    </div>

                                    <div className="social-location-controls">
                                        <LocationFeedback loading={carregandoLocal} error={erroLocal} retry={retryLocal} />
                                        {configLocal && !configLocal.cidadeDisponivel && <p>Preencha cidade e UF no seu perfil para habilitar todos os filtros regionais.</p>}
                                        <label className="social-location-optin">
                                            <input type="checkbox" checked={configLocal?.compartilharRegiao ?? false}
                                                disabled={!configLocal || salvandoVisibilidade}
                                                onChange={(event) => alterarVisibilidade(event.target.checked)} />
                                            Permitir que meu perfil apareça nas buscas por região
                                        </label>
                                        <p>Sua cidade e estado serão usados nos filtros e nos indicadores de proximidade. A localização é informada pelo próprio participante.</p>
                                        <p>Os filtros regionais mostram somente participantes que ativaram essa opção.</p>
                                        {salvandoVisibilidade && <p role="status">Salvando sua escolha...</p>}
                                        {erroVisibilidade && <p role="alert">{erroVisibilidade}</p>}
                                    </div>
                            {carregandoPessoas ? <p role="status">Carregando pessoas...</p> : erroPessoas ? (
                                <div role="alert"><p>{erroPessoas}</p><button type="button" onClick={() => setRevisaoLocal(atual => atual + 1)}>Tentar novamente</button></div>
                            ) : usuarios.length === 0 ? (

                                <div className="community-state">

                                    <div className="community-state-icon">
                                        ♡
                                    </div>

                                    <h2>
                                        Nenhuma pessoa encontrada neste filtro
                                    </h2>

                                    <p>
                                        Tente outro filtro ou volte mais tarde para encontrar novos participantes.
                                    </p>

                                </div>

                            ) : (

                                <div className="community-grid">

                                    {usuarios.map(({ perfil, seguindo }) => (

                                        <SocialUserCard
                                            key={perfil.userId}
                                            usuario={perfil}
                                            seguindo={seguindo}
                                            onSolicitada={abrirSolicitacao}
                                            onBloqueado={participanteBloqueado}
                                        />
                                    ))}
                                </div>
                            )}
                            {(paginaPessoas > 0 || maisPessoas) && (
                                <nav className="community-suggestions-actions" aria-label="Páginas de pessoas">
                                    <button className="community-secondary-button" type="button"
                                        disabled={paginaPessoas === 0 || carregandoPessoas}
                                        onClick={() => setPaginaPessoas(pagina => pagina - 1)}>Anterior</button>
                                    <span aria-live="polite">Página {paginaPessoas + 1}</span>
                                    <button className="community-secondary-button" type="button"
                                        disabled={!maisPessoas || carregandoPessoas}
                                        onClick={() => setPaginaPessoas(pagina => pagina + 1)}>Próxima</button>
                                </nav>
                            )}
                        </>
                    )}
                            {abaAtiva === "conversas" && (
                                <SocialContactsPanel caixa={caixaContato} onCaixaChange={setCaixaContato}
                                    onBloqueioAlterado={() => setRevisaoLocal((atual) => atual + 1)}
                                    naoLidas={naoLidas} />
                            )}
                            {abaAtiva === "comunidades" && (

                                <div className="communities-section">

                                    <RecommendationBanner title="Comunidades por hobby"
                                        description="Participe de espaços dedicados aos hobbies que você pratica, já conhece ou deseja descobrir." />

                                    <div className="community-segments community-scope-filters" role="group" aria-label="Abrangência das comunidades">
                                        <button
                                            type="button" aria-pressed={tipoComunidade === "geral"}
                                            className={
                                                tipoComunidade === "geral"
                                                    ? "active"
                                                    : ""
                                            }
                                            onClick={() =>
                                                setTipoComunidade("geral")
                                            }
                                        >
                                            Para todos
                                        </button>

                                        <button
                                            type="button" aria-pressed={tipoComunidade === "regional"}
                                            className={
                                                tipoComunidade === "regional"
                                                    ? "active"
                                                    : ""
                                            }
                                            onClick={() =>
                                                setTipoComunidade("regional")
                                            }
                                            disabled={!configLocal?.estadoDisponivel}
                                        >
                                            No seu estado
                                        </button>
                                        <button className={tipoComunidade === "municipal" ? "active" : ""}
                                            type="button" aria-pressed={tipoComunidade === "municipal"}
                                            disabled={!configLocal?.cidadeDisponivel}
                                            onClick={() => setTipoComunidade("municipal")}>
                                            Na sua cidade
                                        </button>

                                    </div>


                                    <LocationFeedback loading={carregandoLocal} error={erroLocal} retry={retryLocal} />
                                    {configLocal && !configLocal.cidadeDisponivel && <p>Preencha cidade e UF no seu perfil para acessar as comunidades municipais.</p>}
                                    {carregandoRegiao ? <p role="status">Carregando comunidades...</p> : erroRegiao ? (
                                        <div role="alert"><p>{erroRegiao}</p><button type="button" onClick={retryCatalogo}>Tentar novamente</button></div>
                                    ) : comunidadesExibidas.length === 0 ? (

                                        <div className="community-state">

                                            <div className="community-state-icon">
                                                ♡
                                            </div>

                                            <h2>Nenhuma comunidade disponível</h2>

                                            <p>
                                                As comunidades de hobbies aparecerão aqui.
                                            </p>

                                        </div>

                                    ) : (

                                        <div className="communities-grid">

                                            {comunidadesExibidas.map(comunidade => (

                                                <CommunityCard
                                                    key={chaveComunidade(comunidade)}
                                                    comunidade={comunidade}
                                                    alterando={
                                                        alterandoComunidade === chaveComunidade(comunidade)
                                                    }
                                                    onEntrar={entrarComunidade}
                                                    onSair={sairComunidade}
                                                    onAbrir={(communityId) =>
                                                        navigate(
                                                            `/community/communities/${communityId}`
                                                        )
                                                    }
                                                />

                                            ))}

                                        </div>
                                    )}

                                </div>
                            )}

                        </>
                    )}

            </section>

        </main>
    );
}


function SocialUserCard({ usuario, seguindo, onSolicitada, onBloqueado }) {
    const navigate = useNavigate();

    const afinidade = usuario.afinidade;

    const possuiAfinidade =
        afinidade &&
        (
            afinidade.praticandoEmComum?.length > 0 ||
            afinidade.hobbiesDeInteresseEmComum?.length > 0 ||
            afinidade.interessesEmComum?.length > 0
        );

    return (
        <article className="social-user-card">

            <div className="social-user-header">

                <SocialAvatar usuario={usuario} />

                <div>
                    <h2>{usuario.nome}</h2>
                    <button className="community-secondary-button" onClick={() => navigate(`/community/users/${usuario.userId}`)}>Ver perfil</button>

                    {possuiAfinidade && (
                        <span className="social-affinity-badge">
                            Interesses em comum
                        </span>
                    )}
                </div>

            </div>


            {usuario.biografia && <p style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>{usuario.biografia}</p>}
            <p className="social-user-summary">
                {usuario.resumo}
            </p>
            {usuario.proximidade && (
                <p className="social-location-badge" title="Conforme a localização informada nos perfis.">
                    {usuario.proximidade === "MESMA_CIDADE"
                        ? "É da mesma cidade que você"
                        : "É do mesmo estado que você"}
                </p>
            )}


            {afinidade?.praticandoEmComum?.length > 0 && (

                <AffinityGroup
                    titulo="Vocês praticam"
                    itens={afinidade.praticandoEmComum}
                />
            )}


            {afinidade?.hobbiesDeInteresseEmComum?.length > 0 && (

                <AffinityGroup
                    titulo="Hobbies de interesse em comum"
                    itens={afinidade.hobbiesDeInteresseEmComum}
                />
            )}


            {afinidade?.interessesEmComum?.length > 0 && (

                <AffinityGroup
                    titulo="Interesses em comum"
                    itens={afinidade.interessesEmComum}
                />
            )}


            {!possuiAfinidade && (
                <p className="social-no-affinity">
                    Nenhum interesse em comum está visível,
                    mas talvez descubram novos hobbies juntos.
                </p>
            )}
            <SocialFollowButton userId={usuario.userId} initialFollowing={seguindo} />
            <SocialContactRequestButton usuario={usuario} onSolicitada={onSolicitada} onBloqueado={onBloqueado} />

        </article>
    );
}


function AffinityGroup({ titulo, itens }) {

    return (
        <div className="social-affinity-group">

            <span>{titulo}</span>

            <div className="social-affinity-tags">

                {itens.map(item => (
                    <span key={item}>
                        {item}
                    </span>
                ))}

            </div>

        </div>
    );
}

function chaveComunidade(comunidade) {
    return [comunidade.tipo, comunidade.estado, comunidade.cidade, comunidade.hobbyId].join(":");
}

function CommunityCard({
                           comunidade,
                           alterando,
                           onEntrar,
                           onSair,
                           onAbrir
                       }) {

    return (
        <article className="hobby-community-card">

            <div className="hobby-community-icon">
                ✦
            </div>


            <div className="hobby-community-content">

                <div className="hobby-community-top">

                    <div>
                        <span className="hobby-community-type">
                            {comunidade.tipo === "MUNICIPAL" ? "COMUNIDADE MUNICIPAL" : comunidade.tipo === "REGIONAL"
                            ? "COMUNIDADE REGIONAL"
                            : "COMUNIDADE GERAL"}
                        </span>

                        <h3>
                            {comunidade.hobbyNome}
                        </h3>
                    </div>


                    {comunidade.participando && (
                        <button
                            className="community-card-open"
                            onClick={() =>
                                onAbrir(comunidade.id)
                            }
                        >
                            Abrir comunidade
                        </button>
                    )}

                </div>


                <p className="hobby-community-description">

                    {comunidade.tipo === "MUNICIPAL" ? (
                        <>Converse com participantes de {comunidade.cidade} / {comunidade.estado} interessados em {comunidade.hobbyNome}.</>
                    ) : comunidade.tipo === "REGIONAL" ? (
                        <>
                            Converse com pessoas da sua região
                            interessadas em {comunidade.hobbyNome}.
                        </>
                    ) : (
                        <>
                            Converse com pessoas interessadas em{" "}
                            {comunidade.hobbyNome} e compartilhe
                            experiências sobre esse hobby.
                        </>
                    )}

                </p>


                <div className="hobby-community-footer">

                    <span className="community-member-count">
                        {comunidade.membros}{" "}
                        {comunidade.membros === 1
                            ? "membro"
                            : "membros"}
                    </span>


                    {comunidade.participando ? (

                        <button
                            className="community-card-leave"
                            disabled={alterando}
                            onClick={() =>
                                onSair(comunidade)
                            }
                        >
                            {alterando
                                ? "Saindo..."
                                : "Sair"}
                        </button>

                    ) : (

                        <button
                            className="community-card-join"
                            disabled={alterando}
                            onClick={() =>
                                onEntrar(comunidade)
                            }
                        >
                            {alterando
                                ? "Entrando..."
                                : "Participar"}
                        </button>
                    )}

                </div>

            </div>

        </article>
    );
}

