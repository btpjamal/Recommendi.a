import { API_BASE_URL } from "../api/config.js";
import { session, revokeSession } from "../auth/session.js";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import axios from "axios";

import "./css/Community.css";
import "./css/RecommendationsLayout.css";


export default function Community() {

    const navigate = useNavigate();

    const [status, setStatus] = useState(null);
    const [usuarios, setUsuarios] = useState([]);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState("");
    const [ativando, setAtivando] = useState(false);
    const [abaAtiva, setAbaAtiva] = useState("pessoas");
    const [comunidades, setComunidades] = useState([]);
    const [alterandoComunidade, setAlterandoComunidade] = useState(null);
    const [comunidadesRegionais, setComunidadesRegionais] = useState([]);
    const [tipoComunidade, setTipoComunidade] = useState("geral");

    const nome = session.getItem("nome");
    const token = session.getItem("token");


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
                const [usersResponse, communitiesResponse, regionalResponse] =
                    await Promise.all([

                        axios.get(
                            API_BASE_URL + "/community/users",
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
                        ),

                        axios.get(
                            API_BASE_URL + "/community/communities/regional",
                            {
                                headers: {
                                    Authorization: `Bearer ${token}`
                                }
                            }
                        )
                    ]);

                setUsuarios(usersResponse.data);
                setComunidades(communitiesResponse.data);
                setComunidadesRegionais(regionalResponse.data);
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


    async function desativarParticipacao() {

        const confirmar = window.confirm(
            "Deseja sair da comunidade? Seu perfil deixará de aparecer para outros participantes."
        );

        if (!confirmar) {
            return;
        }

        try {

            await axios.post(
                API_BASE_URL + "/social/me/disable",
                {},
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );

            setUsuarios([]);

            setStatus({
                elegivel: true,
                participa: false
            });

        } catch (error) {


            setErro(
                error.response?.data?.error ||
                "Não foi possível desativar sua participação."
            );
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
    }

    const comunidadesExibidas =
        tipoComunidade === "regional"
            ? comunidadesRegionais
            : comunidades;

    return (
        <main className="community-page">

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


                    <div className="app-header-actions">

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
                            <div className="community-heading">

                                <div>
                                <span className="community-label">
                                    COMUNIDADE
                                </span>

                                    <h1>
                                        Pessoas com interesses parecidos
                                    </h1>

                                    <p>
                                        Descubra participantes que
                                        compartilham hobbies e interesses
                                        com você.
                                    </p>
                                </div>

                                <button
                                    className="community-leave-button"
                                    onClick={desativarParticipacao}
                                >
                                    Sair da comunidade
                                </button>

                            </div>

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

                            <div className="community-tabs">

                                <button
                                    className={abaAtiva === "pessoas" ? "active" : ""}
                                    onClick={() => setAbaAtiva("pessoas")}
                                >
                                    Pessoas
                                </button>

                                <button
                                    className={abaAtiva === "comunidades" ? "active" : ""}
                                    onClick={() => setAbaAtiva("comunidades")}
                                >
                                    Comunidades
                                </button>

                            </div>

                            {abaAtiva === "pessoas" && (
                                <>
                            {usuarios.length === 0 ? (

                                <div className="community-state">

                                    <div className="community-state-icon">
                                        ♡
                                    </div>

                                    <h2>
                                        Ainda não há outras pessoas por aqui
                                    </h2>

                                    <p>
                                        Conforme novos participantes entrarem
                                        na comunidade, eles aparecerão aqui.
                                    </p>

                                </div>

                            ) : (

                                <div className="community-grid">

                                    {usuarios.map(usuario => (

                                        <SocialUserCard
                                            key={usuario.userId}
                                            usuario={usuario}
                                        />
                                    ))}
                                </div>
                            )}
                        </>
                    )}
                            {abaAtiva === "comunidades" && (

                                <div className="communities-section">

                                    <div className="communities-intro">
                                        <h2>Comunidades por hobby</h2>

                                        <p>
                                            Participe de espaços dedicados aos hobbies
                                            que você pratica, já conhece ou deseja descobrir.
                                        </p>
                                    </div>

                                    <div className="community-type-tabs">

                                        <button
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
                                            className={
                                                tipoComunidade === "regional"
                                                    ? "active"
                                                    : ""
                                            }
                                            onClick={() =>
                                                setTipoComunidade("regional")
                                            }
                                        >
                                            No seu estado
                                        </button>

                                    </div>


                                    {comunidadesExibidas.length === 0 ? (

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


function SocialUserCard({ usuario }) {

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

                <div className="social-user-avatar">
                    {usuario.nome
                        ?.charAt(0)
                        .toUpperCase()}
                </div>

                <div>
                    <h2>{usuario.nome}</h2>

                    {possuiAfinidade && (
                        <span className="social-affinity-badge">
                            Interesses em comum
                        </span>
                    )}
                </div>

            </div>


            <p className="social-user-summary">
                {usuario.resumo}
            </p>


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
                    Vocês ainda não possuem interesses em comum,
                    mas talvez descubram novos hobbies juntos.
                </p>
            )}

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
                            {comunidade.tipo === "REGIONAL"
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

                    {comunidade.tipo === "REGIONAL" ? (
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