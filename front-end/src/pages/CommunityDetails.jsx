import { API_BASE_URL } from "../api/config.js";
import { session, revokeSession } from "../auth/session.js";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import axios from "axios";

import "./css/CommunityDetails.css";
import "./css/RecommendationsLayout.css";


export default function CommunityDetails() {

    const { communityId } = useParams();
    const navigate = useNavigate();

    const token = session.getItem("token");
    const nome = session.getItem("nome");

    const [mensagens, setMensagens] = useState([]);
    const [conteudo, setConteudo] = useState("");

    const [carregando, setCarregando] = useState(true);
    const [enviando, setEnviando] = useState(false);
    const [erro, setErro] = useState("");
    const [comunidade, setComunidade] = useState(null);


    useEffect(() => {

        async function carregarPagina() {

            try {

                setCarregando(true);
                setErro("");

                await Promise.all([
                    carregarDetalhes(),
                    carregarMensagens()
                ]);

            } catch (error) {


                setErro(
                    error.response?.data?.error ||
                    "Não foi possível carregar a comunidade."
                );

            } finally {
                setCarregando(false);
            }
        }

        carregarPagina();

    }, [communityId]);

    useEffect(() => {

        const interval = setInterval(
            atualizarMensagens,
            5000
        );

        return () => {
            clearInterval(interval);
        };

    }, [communityId]);


    async function carregarMensagens() {

        try {

            const response = await axios.get(
                `${API_BASE_URL}/community/communities/${communityId}/messages`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );

            setMensagens(response.data);

        } catch (error) {


            setErro(
                error.response?.data?.error ||
                "Não foi possível carregar as mensagens."
            );

        }
    }

    async function atualizarMensagens() {

        try {

            const response = await axios.get(
                `${API_BASE_URL}/community/communities/${communityId}/messages`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );


            setMensagens(atual => {

                const novas = response.data;

                const iguais =
                    atual.length === novas.length &&
                    atual.every(
                        (mensagem, index) =>
                            mensagem.id === novas[index]?.id
                    );

                return iguais
                    ? atual
                    : novas;
            });

        } catch {
                // Não registrar erros de requisição, pois podem conter credenciais.
            }
    }


    async function enviarMensagem(event) {

        event.preventDefault();

        const mensagem = conteudo.trim();

        if (!mensagem || enviando) {
            return;
        }

        try {

            setEnviando(true);
            setErro("");

            const response = await axios.post(
                `${API_BASE_URL}/community/communities/${communityId}/messages`,
                {
                    conteudo: mensagem
                },
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );

            setMensagens(atual => [
                ...atual,
                response.data
            ]);

            setConteudo("");

        } catch (error) {


            setErro(
                error.response?.data?.error ||
                "Não foi possível enviar a mensagem."
            );

        } finally {
            setEnviando(false);
        }
    }


    async function logout() {
        await revokeSession();
        navigate("/login");
    }

    async function carregarDetalhes() {

        const response = await axios.get(
            `${API_BASE_URL}/community/communities/${communityId}`,
            {
                headers: {
                    Authorization: `Bearer ${token}`
                }
            }
        );

        setComunidade(response.data);
    }


    return (
        <main className="community-details-page">

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
                                navigate("/community")
                            }
                        >
                            Comunidade
                        </button>

                        <button
                            className="app-header-button"
                            onClick={() =>
                                navigate("/recommendations")
                            }
                        >
                            Recomendações
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


            <section className="community-details-container">

                <button
                    className="community-back-button"
                    onClick={() =>
                        navigate("/community")
                    }
                >
                    ← Voltar
                </button>


                <div className="community-chat">

                    <div className="community-chat-header">

                        <span>
                            {comunidade?.tipo === "REGIONAL"
                            ? "COMUNIDADE REGIONAL"
                            : "COMUNIDADE GERAL"}
                        </span>

                        <h1>
                            {comunidade?.hobbyNome || "Comunidade"}
                        </h1>

                        <p>
                            {comunidade?.tipo === "REGIONAL"
                                ? "Converse com pessoas da sua região e compartilhe experiências sobre esse hobby."
                                : "Converse com outros participantes e compartilhe experiências sobre esse hobby."}
                        </p>

                        {comunidade && (
                            <div className="community-chat-info">
                                <span>
                                    {comunidade.membros}{" "}
                                    {comunidade.membros === 1
                                    ? "membro"
                                    : "membros"}
                                </span>
                            </div>
                        )}

                    </div>


                    {erro && (
                        <div className="community-chat-error">
                            {erro}
                        </div>
                    )}


                    <div className="community-messages">

                        {carregando ? (

                            <div className="community-messages-empty">
                                Carregando mensagens...
                            </div>

                        ) : mensagens.length === 0 ? (

                            <div className="community-messages-empty">

                                <strong>
                                    Ainda não há mensagens
                                </strong>

                                <span>
                                    Seja a primeira pessoa a
                                    iniciar uma conversa.
                                </span>

                            </div>

                        ) : (

                            mensagens.map(mensagem => (

                                <Message
                                    key={mensagem.id}
                                    mensagem={mensagem}
                                />

                            ))

                        )}

                    </div>


                    <form
                        className="community-message-form"
                        onSubmit={enviarMensagem}
                    >

                        <textarea
                            value={conteudo}
                            onChange={event =>
                                setConteudo(event.target.value)
                            }
                            placeholder="Escreva uma mensagem..."
                            maxLength={1000}
                            rows={3}
                        />

                        <div className="community-message-form-footer">

                            <span>
                                {conteudo.length}/1000
                            </span>

                            <button
                                type="submit"
                                disabled={
                                    enviando ||
                                    !conteudo.trim()
                                }
                            >
                                {enviando
                                    ? "Enviando..."
                                    : "Enviar"}
                            </button>

                        </div>

                    </form>

                </div>

            </section>

        </main>
    );
}


function Message({ mensagem }) {

    const data = new Date(
        mensagem.dataEnvio
    );

    const horario =
        data.toLocaleTimeString(
            "pt-BR",
            {
                hour: "2-digit",
                minute: "2-digit"
            }
        );


    return (
        <div
            className={
                mensagem.propria
                    ? "community-message own"
                    : "community-message"
            }
        >

            <div className="community-message-meta">

                <strong>
                    {mensagem.propria
                        ? "Você"
                        : mensagem.autorNome}
                </strong>

                <span>{horario}</span>

            </div>


            <p>
                {mensagem.conteudo}
            </p>

        </div>
    );
}