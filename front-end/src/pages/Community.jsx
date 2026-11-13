import RecommendationBanner from "../components/recommendations/RecommendationBanner";
import "./css/CommunityVisual.css";
import SocialFeed from "../components/social/SocialFeed.jsx";
import SocialFollowButton from "../components/social/SocialFollowButton.jsx";
import NotificationBell from "../components/social/SocialNotifications.jsx";
import { API_BASE_URL } from "../api/config.js";
import { session, revokeSession } from "../auth/session.js";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useOutletContext, useSearchParams } from "react-router";
import axios from "axios";

import "./css/Community.css";
import "./css/RecommendationsLayout.css";
import "./css/SocialLocation.css";
import SocialAvatar from "../components/social/SocialAvatar.jsx";
import SocialContactRequestButton from "../components/social/SocialContactRequestButton";
import SocialContactsPanel from "../components/social/SocialContactsPanel";
import usePrivateUnread from "../components/social/usePrivateUnread.js";


export default function Community() {

    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const { mobile, unread } = useOutletContext();

    const [status, setStatus] = useState(null);
    const [resultadoPessoas, setResultadoPessoas] = useState(null);
    const [paginaPessoas, setPaginaPessoas] = useState(0);
    const [carregando, setCarregando] = useState(true);
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

    const [comunidades, setComunidades] = useState([]);
    const [alterandoComunidade, setAlterandoComunidade] = useState(null);
    const [comunidadesRegionais, setComunidadesRegionais] = useState([]);
    const [tipoComunidade, setTipoComunidade] = useState("geral");
    const [comunidadesMunicipais, setComunidadesMunicipais] = useState([]);
    const [configLocal, setConfigLocal] = useState(null);
    const [filtroPessoas, setFiltroPessoas] = useState("GERAL");
    const [carregandoRegiao, setCarregandoRegiao] = useState(false);
    const [erroRegiao, setErroRegiao] = useState("");
    const [salvandoVisibilidade, setSalvandoVisibilidade] = useState(false);
    const [erroVisibilidade, setErroVisibilidade] = useState("");
    const [revisaoLocal, setRevisaoLocal] = useState(0);
    const unreadDesktop = usePrivateUnread(!mobile && Boolean(status?.elegivel && status?.participa), revisaoLocal);
    const naoLidas = mobile ? unread : unreadDesktop;

    const nome = session.getItem("nome");
    const token = session.getItem("token");
    const pessoasAtiva = Boolean(status?.elegivel && status?.participa && abaAtiva === "pessoas");
    const consultaPessoas = useMemo(() => pessoasAtiva ? {
        escopo: filtroPessoas, pagina: paginaPessoas, revisao: revisaoLocal, token
    } : null, [pessoasAtiva, filtroPessoas, paginaPessoas, revisaoLocal, token]);
    const resultadoAtual = resultadoPessoas?.consulta === consultaPessoas ? resultadoPessoas : null;
    const usuarios = resultadoAtual?.pessoas ?? [];
    const maisPessoas = resultadoAtual?.temMais ?? false;
    const carregandoPessoas = pessoasAtiva && !resultadoAtual;
    const erroPessoas = resultadoAtual?.erro ?? "";

    useEffect(() => {
        if (!consultaPessoas) return;
        const controller = new AbortController();
        axios.get(API_BASE_URL + "/community/users/page", {
            headers: { Authorization: `Bearer ${consultaPessoas.token}` },
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

    useEffect(() => {
        setErroRegiao("");
        setCarregandoRegiao(false);
        if (!status?.participa || abaAtiva !== "comunidades" || tipoComunidade === "geral") return;
        const controller = new AbortController();
        setCarregandoRegiao(true);
        axios.get(API_BASE_URL + "/community/communities/" + tipoComunidade, {
            headers: { Authorization: `Bearer ${token}` },
            signal: controller.signal
        }).then((resposta) => {
            if (controller.signal.aborted) return;
            if (tipoComunidade === "municipal") setComunidadesMunicipais(resposta.data);
            else setComunidadesRegionais(resposta.data);
        }).catch((error) => {
            if (!controller.signal.aborted) setErroRegiao(
                error.response?.data?.error || "Não foi possível carregar as comunidades desta região."
            );
        }).finally(() => {
            if (!controller.signal.aborted) setCarregandoRegiao(false);
        });
        return () => controller.abort();
    }, [status?.participa, abaAtiva, tipoComunidade, revisaoLocal, token]);

    async function alterarVisibilidade(compartilharRegiao) {
        if (salvandoVisibilidade) return;
        setSalvandoVisibilidade(true);
        setErroVisibilidade("");
        try {
            const resposta = await axios.put(API_BASE_URL + "/community/location-visibility", {
                compartilharRegiao
            }, { headers: { Authorization: `Bearer ${token}` } });
            setConfigLocal(resposta.data);
        } catch (error) {
            setErroVisibilidade(error.response?.data?.error || "Não foi possível salvar sua escolha.");
        } finally {
            setSalvandoVisibilidade(false);
        }
    }


    useEffect(() => {
        carregarComunidade();
    }, []);


    async function carregarComunidade() {

        try {

            setCarregando(true);
            setErro("");

            const statusResponse = await axios.get(
                API_BASE_URL + "/social/me/status",
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );

            const statusAtual = statusResponse.data;

            setStatus(statusAtual);


            if (
                statusAtual.elegivel &&
                statusAtual.participa
            ) {
                const [locationResponse, communitiesResponse] =
                    await Promise.all([

                        axios.get(
                            API_BASE_URL + "/community/location",
                            {
                                headers: {
                                    Authorization: `Bearer ${token}`
                                }
                            }
                        ),

                        axios.get(
                            API_BASE_URL + "/community/communities",
                            {
                                headers: {
                                    Authorization: `Bearer ${token}`
                                }
                            }
                        )
                    ]);

                setConfigLocal(locationResponse.data);
                setComunidades(communitiesResponse.data);
            }

        } catch (error) {


            setErro(
                error.response?.data?.error ||
                "Não foi possível carregar a comunidade."
            );

        } finally {
            setCarregando(false);
        }
    }


    async function ativarParticipacao() {

        try {

            setAtivando(true);
            setErro("");

            await axios.post(
                API_BASE_URL + "/social/me/enable",
                {},
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );

            window.dispatchEvent(new Event("social-participation-changed"));
            await carregarComunidade();

        } catch (error) {


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

    async function entrarComunidade(communityId) {

        try {
            setAlterandoComunidade(communityId);

            const response = await axios.post(
                `${API_BASE_URL}/community/communities/${communityId}/join`,
                {},
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );

            atualizarComunidade(response.data);

        } catch (error) {


            setErro(
                error.response?.data?.error ||
                "Não foi possível entrar na comunidade."
            );

        } finally {
            setAlterandoComunidade(null);
        }
    }


    async function sairComunidade(communityId) {

        try {
            setAlterandoComunidade(communityId);

            const response = await axios.delete(
                `${API_BASE_URL}/community/communities/${communityId}/leave`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );

            atualizarComunidade(response.data);

        } catch (error) {


            setErro(
                error.response?.data?.error ||
                "Não foi possível sair da comunidade."
            );

        } finally {
            setAlterandoComunidade(null);
        }
    }


    function atualizarComunidade(comunidadeAtualizada) {

        const atualizarLista = lista =>
            lista.map(comunidade =>
                comunidade.id === comunidadeAtualizada.id
                    ? comunidadeAtualizada
                    : comunidade
            );

        setComunidades(atualizarLista);
        setComunidadesRegionais(atualizarLista);
        setComunidadesMunicipais(atualizarLista);
    }

    const comunidadesExibidas =
        tipoComunidade === "municipal" ? comunidadesMunicipais : tipoComunidade === "regional"
            ? comunidadesRegionais
            : comunidades;

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
                                    <div className="social-location-controls">
                                        <h2 id="filtro-local-pessoas">Encontrar pessoas</h2>
                                        <div className="community-segments" role="group" aria-labelledby="filtro-local-pessoas">
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
                                        {!configLocal?.cidadeDisponivel && <p>Preencha cidade e UF no seu perfil para habilitar todos os filtros regionais.</p>}
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
                                <div role="alert"><p>{erroPessoas}</p><button onClick={() => setRevisaoLocal((atual) => atual + 1)}>Tentar novamente</button></div>
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


                                    {!configLocal?.cidadeDisponivel && <p>Preencha cidade e UF no seu perfil para acessar as comunidades municipais.</p>}
                                    {carregandoRegiao ? <p role="status">Carregando comunidades...</p> : erroRegiao ? (
                                        <div role="alert"><p>{erroRegiao}</p><button onClick={() => setRevisaoLocal((atual) => atual + 1)}>Tentar novamente</button></div>
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
                                                    key={comunidade.id}
                                                    comunidade={comunidade}
                                                    alterando={
                                                        alterandoComunidade === comunidade.id
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
                                onSair(comunidade.id)
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
                                onEntrar(comunidade.id)
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

