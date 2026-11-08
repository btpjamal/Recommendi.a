import { session } from "../../auth/session.js";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../../api/api";
import "../../pages/css/Recommendations.css";
import RecommendationImage from "./RecommendationImage";
import "./InterestedTab.css";

export default function InterestedTab() {

    const navigate = useNavigate();

    const [hobbies, setHobbies] = useState([]);
    const [loading, setLoading] = useState(true);
    const [erro, setErro] = useState("");
    const [possuiPlanoGeral, setPossuiPlanoGeral] = useState(false);
    const [avaliacoes, setAvaliacoes] = useState({});
    const [salvandoAvaliacao, setSalvandoAvaliacao] = useState(null);

    useEffect(() => {
        carregarInteressados();
        carregarAvaliacoes();
    }, []);

    useEffect(() => {
        verificarPlanoGeral();
    }, []);

    async function carregarInteressados() {
        try {
            setLoading(true);
            setErro("");

            const userId = session.getItem("userId");
            const token = session.getItem("token");

            const response = await api.get(
                `/recommendation-feedbacks/${userId}?tipo=INTERESSADO`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            setHobbies(response.data);

        } catch {


            setErro(
                "Não foi possível carregar seus hobbies de interesse."
            );
        } finally {
            setLoading(false);
        }
    }

    async function carregarAvaliacoes() {
        try {
            const userId = session.getItem("userId");
            const token = session.getItem("token");

            const response = await api.get(
                `/users/${userId}/feedbacks`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            const avaliacoesPorHobby = {};

            response.data.forEach((avaliacao) => {
                avaliacoesPorHobby[avaliacao.hobby.id] =
                    avaliacao.rating;
            });

            setAvaliacoes(avaliacoesPorHobby);

        } catch (error) {
            console.error(
                "Erro ao carregar avaliações:",
                error
            );
        }
    }

    async function verificarPlanoGeral() {
        try {
            await api.get("/ai/general-plan");
            setPossuiPlanoGeral(true);
        } catch (error) {
            if (error.response?.status === 404) {
                setPossuiPlanoGeral(false);
                return;
            }


        }
    }

    async function devolverParaDescobrir(hobbyId) {
        try {
            const userId = session.getItem("userId");
            const token = session.getItem("token");

            await api.delete(
                `/recommendation-feedbacks/${userId}/${hobbyId}`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            setHobbies((atuais) =>
                atuais.filter(
                    (hobby) => hobby.hobbyId !== hobbyId
                )
            );

        } catch {


            setErro(
                "Não foi possível desfazer sua escolha."
            );
        }
    }

    async function atualizarNivel(hobbyId, nivelAtual) {
        try {
            const userId = session.getItem("userId");
            const token = session.getItem("token");

            await api.patch(
                `/user-hobbies/${userId}/${hobbyId}/nivel`,
                {
                    nivelAtual
                },
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            await carregarInteressados();

            setHobbies((atuais) =>
                atuais.map((hobby) =>
                    hobby.hobbyId === hobbyId
                        ? {
                            ...hobby,
                            nivelAtual: nivelAtual
                        }
                        : hobby
                )
            );

        } catch {


            setErro(
                "Não foi possível atualizar seu nível."
            );
        }
    }

    async function atualizarStatus(hobbyId, statusAtual) {
        try {
            const userId = session.getItem("userId");
            const token = session.getItem("token");

            await api.patch(
                `/user-hobbies/${userId}/${hobbyId}/status`,
                {
                    statusAtual
                },
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            setHobbies((atuais) =>
                atuais.map((hobby) =>
                    hobby.hobbyId === hobbyId
                        ? {
                            ...hobby,
                            statusAtual
                        }
                        : hobby
                )
            );

        } catch {


            setErro(
                "Não foi possível atualizar a situação do hobby."
            );
        }
    }

    async function salvarAvaliacao(hobbyId, rating) {
        if (salvandoAvaliacao !== null) return;

        try {
            setSalvandoAvaliacao(hobbyId);

            const userId = session.getItem("userId");
            const token = session.getItem("token");

            await api.post(
                `/users/${userId}/feedbacks`,
                {
                    hobbyId,
                    rating
                },
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );

            setAvaliacoes((atuais) => ({
                ...atuais,
                [hobbyId]: rating
            }));

        } catch (error) {
            console.error("Erro ao salvar avaliação:", error);
            alert("Não foi possível salvar sua avaliação.");

        } finally {
            setSalvandoAvaliacao(null);
        }
    }


    if (loading) {
        return <p>Carregando hobbies...</p>;
    }

    if (erro) {
        return <p>{erro}</p>;
    }

    if (hobbies.length === 0) {
        return (
            <section>

                <div className="interested-empty">
                    <div className="interested-empty-icon">
                        ♡
                    </div>

                    <h2>Nenhum hobby de interesse ainda</h2>

                    <p>
                        Você ainda não marcou nenhum hobby como interessante.
                        Explore as recomendações e salve aqueles que mais
                        combinam com você.
                    </p>
                </div>
            </section>
        );
    }

    return (
        <section className="interested-section">

            <div className="interested-header">
                <div>
                    <h2>Tenho interesse</h2>

                    <p>
                        Acompanhe seus hobbies salvos e organize
                        sua evolução pessoal.
                    </p>
                </div>

                <button
                    type="button"
                    className="general-plan-button"
                    onClick={() => navigate("/general-plan")}
                >
                    {possuiPlanoGeral
                        ? "📋 Ver plano geral"
                        : "✨ Criar plano geral"}
                </button>
            </div>

            <div className="hobby-status-info">
                <p>
                    <strong>ⓘ Meu nível</strong> representa quanta
                    experiência você possui com o hobby, mesmo
                    que não o pratique atualmente.
                </p>

                <p>
                    <strong>ⓘ Situação atual</strong> representa
                    sua relação com o hobby neste momento.
                </p>
            </div>

            <div className="recommendations-grid">
                {hobbies.map((hobby) => (
                    <article
                        key={hobby.hobbyId}
                        className="recommendation-card interested-card"
                    >

                        <div className="recommendation-card-header">
                            <span className="recommendation-category">
                                {hobby.categoria}
                            </span>

                            <span className="recommendation-score">
                                {hobby.score} pts
                            </span>
                        </div>

                        <div className="recommendation-card-content">
                            <h3 className="recommendation-name">
                                {hobby.nome}
                            </h3>

                            <p className="recommendation-description">
                                {hobby.descricao}
                            </p>

                            <RecommendationImage imagemUrl={hobby.imagemUrl} nome={hobby.nome} />

                            <div className="interested-fields">

                                <label className="interested-field">
                                    <span>Meu nível</span>

                                    <select
                                        value={
                                            hobby.nivelAtual ?? "INICIANTE"
                                        }
                                        onChange={(e) =>
                                            atualizarNivel(
                                                hobby.hobbyId,
                                                e.target.value
                                            )
                                        }
                                    >
                                        <option value="INICIANTE">
                                            Iniciante
                                        </option>

                                        <option value="INTERMEDIARIO">
                                            Intermediário
                                        </option>

                                        <option value="AVANCADO">
                                            Avançado
                                        </option>
                                    </select>
                                </label>

                                <label className="interested-field">
                                    <span>Situação atual</span>

                                    <select
                                        value={
                                            hobby.statusAtual ?? "INTERESSADO"
                                        }
                                        onChange={(e) =>
                                            atualizarStatus(
                                                hobby.hobbyId,
                                                e.target.value
                                            )
                                        }
                                    >
                                        <option value="INTERESSADO">
                                            Tenho interesse
                                        </option>

                                        <option value="PRATICANDO">
                                            Estou praticando
                                        </option>

                                        <option value="PAUSADO">
                                            Está pausado
                                        </option>
                                    </select>
                                </label>

                            </div>

                            {(hobby.statusAtual === "PRATICANDO" || hobby.statusAtual === "PAUSADO") && (

                                <div className="hobby-rating">
                                    <span className="hobby-rating-title">
                                        Minha avaliação
                                    </span>

                                    <div className="hobby-rating-stars">
                                        {[1, 2, 3, 4, 5].map((estrela) => (
                                            <button
                                                disabled={salvandoAvaliacao !== null}
                                                key={estrela}
                                                type="button"
                                                className={
                                                    estrela <= (avaliacoes[hobby.hobbyId] ?? 0)
                                                        ? "star-filled"
                                                        : "star-empty"
                                                }
                                                onClick={() =>
                                                    salvarAvaliacao(hobby.hobbyId, estrela)
                                                }
                                                aria-label={`Avaliar com ${estrela} estrela${estrela > 1 ? "s" : ""}`}
                                                title={`Avaliar com ${estrela} estrela${estrela > 1 ? "s" : ""}`}
                                            >
                                                ★
                                            </button>
                                        ))}
                                    </div>
                                    {salvandoAvaliacao === hobby.hobbyId && (
                                        <span className="hobby-rating-description">
                                        Salvando avaliação...
                                        </span>
                                    )}

                                    <span className="hobby-rating-description">
        {avaliacoes[hobby.hobbyId] != null
            ? `Sua nota: ${avaliacoes[hobby.hobbyId]}/5`
            : "Você ainda não avaliou este hobby."}
    </span>
                                </div>
                            )}

                            <details className="hobby-status-help">
                                <summary>
                                    Como funcionam essas opções?
                                </summary>

                                <div>
                                    <p>
                                        <strong>Meu nível</strong> indica
                                        sua experiência com este hobby:
                                    </p>

                                    <ul>
                                        <li>
                                            <strong>Iniciante:</strong>{" "}
                                            pouca ou nenhuma experiência.
                                        </li>

                                        <li>
                                            <strong>Intermediário:</strong>{" "}
                                            já possui alguma experiência.
                                        </li>

                                        <li>
                                            <strong>Avançado:</strong>{" "}
                                            possui bastante experiência.
                                        </li>
                                    </ul>

                                    <p>
                                        <strong>Situação atual</strong> indica
                                        sua relação com o hobby:
                                    </p>

                                    <ul>
                                        <li>
                                            <strong>Tenho interesse:</strong>{" "}
                                            deseja começar ou voltar.
                                        </li>

                                        <li>
                                            <strong>Estou praticando:</strong>{" "}
                                            pratica atualmente.
                                        </li>

                                        <li>
                                            <strong>Está pausado:</strong>{" "}
                                            não pratica no momento.
                                        </li>
                                    </ul>
                                </div>
                            </details>
                        </div>

                        <div className="interested-actions">
                            <button
                                type="button"
                                className="interested-remove-button"
                                onClick={() =>
                                    devolverParaDescobrir(hobby.hobbyId)
                                }
                            >
                                Mudei de ideia
                            </button>
                        </div>

                        <button
                            type="button"
                            className="recommendation-details-link"
                            onClick={() =>
                                navigate(
                                    `/recommendations/${hobby.hobbyId}`
                                )
                            }
                        >
                            Ver detalhes →
                        </button>

                    </article>
                ))}
            </div>

        </section>
    );
}