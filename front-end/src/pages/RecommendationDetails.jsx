import { useEffect, useState } from "react";
import {api} from "../api/api.js";
import SafeMarkdown from "../components/SafeMarkdown.jsx";
import "../pages/css/RecommendationsDetails.css";
import { useNavigate, useParams } from "react-router-dom";
import HobbyLocations from "../components/HobbyLocations";

function RecommendationDetails() {
    const { hobbyId } = useParams();

    const [detalhes, setDetalhes] = useState(null);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState("");
    const [gerandoPlano, setGerandoPlano] = useState(false);
    const [erroPlano, setErroPlano] = useState("");
    const navigate = useNavigate();


    const carregarDetalhes = async () => {
        try {
            setCarregando(true);
            setErro("");

            const response = await api.get(
                `/recommendations/${hobbyId}/details`
            );

            setDetalhes(response.data);

            } catch {

                setErro(
                    "Não foi possível carregar os detalhes do hobby."
                );
            } finally {
                setCarregando(false);
            }
    };


    useEffect(() => {
        carregarDetalhes();
    }, [hobbyId]);

    const gerarPlano = async () => {
            try {
                setGerandoPlano(true);
                setErroPlano("");

                await api.post(
                    `/ai/plan/${hobbyId}`
                );

                await carregarDetalhes();

            } catch (error) {


                setErroPlano(
                    error.response?.data?.error ??
                    "Não foi possível gerar o plano personalizado."
                );
            } finally {
                setGerandoPlano(false);
            }
        };

        const regenerarPlano = async () => {
            try {
                setGerandoPlano(true);
                setErroPlano("");

                await api.post(
                    `/ai/plan/${hobbyId}/regenerate`
                );

                await carregarDetalhes();

            } catch (error) {


                setErroPlano(
                    error.response?.data?.error ??
                    "Não foi possível atualizar o plano personalizado."
                );
            } finally {
                setGerandoPlano(false);
            }
        };

    if (carregando) {
        return <p>Carregando...</p>;
    }

    if (erro) {
        return <p>{erro}</p>;
    }

    if (!detalhes) {
        return <p>Hobby não encontrado.</p>;
    }


    return (
        <div className="recommendation-details-page">

            <button
                className="back-button"
                onClick={() => navigate(-1)}
            >
                ← Voltar
            </button>

            <header className="recommendation-details-header">

                <div className="recommendation-details-meta">

                    <span className="recommendation-details-badge">
                        {detalhes.categoria}
                    </span>

                    <span className="recommendation-details-badge score-badge">
                        {detalhes.score} pts
                    </span>

                </div>

                <h1>{detalhes.nome}</h1>

                <p className="recommendation-details-description">
                    {detalhes.descricao}
                </p>

            </header>

            <section className="recommendation-details-section">

                <h2>Por que recomendamos este hobby?</h2>

                <div className="recommendation-details-reasons">

                    {(detalhes.motivos || []).map((motivo, index) => (

                        <div
                            className="recommendation-details-reason"
                            key={index}
                        >

                            <span className="details-reason-icon">
                                ✓
                            </span>

                            <span>{motivo}</span>

                        </div>

                    ))}

                </div>

            </section>

            {(detalhes.alertas || []).length > 0 && (

                <section className="recommendation-details-section">

                    <h2>Pontos de atenção</h2>

                    <div className="recommendation-details-alerts">

                        {detalhes.alertas.map((alerta, index) => (

                            <div
                                className="recommendation-details-alert"
                                key={index}
                            >

                                <span>⚠</span>

                                <span>{alerta}</span>

                            </div>

                        ))}

                    </div>

                </section>

            )}

            <section className="recommendation-details-section">

                <h2>Sua relação com este hobby</h2>

                <div className="recommendation-relation">

                    <div className="relation-item">

                        <span className="relation-label">
                            Meu nível
                        </span>

                        <span className="relation-value">
                            {detalhes.nivelAtual
                                ? detalhes.nivelAtual.charAt(0) +
                                  detalhes.nivelAtual.slice(1).toLowerCase()
                                : "Ainda não definido"}
                        </span>

                    </div>

                    <div className="relation-item">

                        <span className="relation-label">
                            Situação atual
                        </span>

                        <span className="relation-value">
                            {detalhes.statusAtual === "PRATICANDO"
                                ? "Estou praticando"
                                : detalhes.statusAtual === "PAUSADO"
                                    ? "Está pausado"
                                    : detalhes.statusAtual === "INTERESSADO"
                                        ? "Tenho interesse"
                                        : "Ainda não definido"}
                        </span>

                    </div>

                </div>

            </section>

            <section className="recommendation-details-section">
                <h2>Plano personalizado</h2>

                {erroPlano && (
                    <p>{erroPlano}</p>
                )}

                {!detalhes.plano.existe ? (
                    <>
                        <p>
                            Você ainda não possui um plano personalizado
                            para este hobby.
                        </p>

                        <button
                            className="plan-action-button"
                            onClick={gerarPlano}
                            disabled={gerandoPlano}
                        >
                            {gerandoPlano
                                ? "Gerando plano..."
                                : "Gerar meu plano"}
                        </button>
                    </>
                ) : (
                    <>
                        {detalhes.plano.stale && (
                            <div className="plan-stale-warning">
                                <p>
                                    Seu perfil mudou desde que este plano
                                    foi criado.
                                </p>

                                <button
                                    className="plan-action-button"
                                    onClick={regenerarPlano}
                                    disabled={gerandoPlano}
                                >
                                    {gerandoPlano
                                        ? "Atualizando plano..."
                                        : "Atualizar plano"}
                                </button>
                            </div>
                        )}

                        <div className="personalized-plan-content">
                            <SafeMarkdown>
                                {detalhes.plano.conteudo}
                            </SafeMarkdown>
                        </div>
                    </>
                )}
            </section>

            <section className="recommendation-details-section">
                <h2>Onde praticar este hobby?</h2>

                <HobbyLocations
                    hobbyId={hobbyId}
                    hobbyNome={detalhes.nome}
                />
            </section>

        </div>
    );
    }
    export default RecommendationDetails;