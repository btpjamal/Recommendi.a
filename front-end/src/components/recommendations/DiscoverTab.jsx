import { session } from "../../auth/session.js";
import { useEffect, useRef , useState } from "react";
import { api } from "../../api/api";
import { useNavigate } from "react-router";
import "../../pages/css/Recommendations.css";

const nomesCriterios = {
    BehaviorCriterion: "Comportamento",
    BudgetCriterion: "Orçamento",
    ExperienceAffinityCriterion: "Experiência com hobbies",
    FeedbackCriterion: "Avaliações anteriores",
    InterestCriterion: "Interesses pessoais",
    ObjectiveCriterion: "Objetivos pessoais",
    PreferenceCriterion: "Preferências",
    TimeCriterion: "Tempo disponível"
};

export default function DiscoverTab() {

    const navigate = useNavigate();

    const [recommendations, setRecommendations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [erro, setErro] = useState(null);
    const [hobbyExplicacao, setHobbyExplicacao] = useState(null);
    const botaoFecharRef = useRef(null);
    const modalRef = useRef(null);

    async function registrarFeedback(hobbyId, tipo) {
                try {
                    const userId = session.getItem("userId");
                    const token = session.getItem("token");

                    await api.post(
                        `/recommendation-feedbacks/${userId}/${hobbyId}`,
                        { tipo },
                        {
                            headers: {
                                Authorization: `Bearer ${token}`,
                            },
                        }
                    );

                    setRecommendations((atuais) =>
                        atuais.filter((recommendation) => recommendation.hobbyId !== hobbyId)
                    );

                } catch {
                // Não registrar erros de requisição, pois podem conter credenciais.
            }
            }

    useEffect(() => {
        async function carregarRecomendacoes() {
          try {
            setLoading(true);

            const token = session.getItem("token");
            const userId = session.getItem("userId");

            if (!token || !userId) {
                session.clear();
                navigate("/login");
                return;
            }

            const response = await api.get(`/recommendations/${userId}`, {
                headers: {
                  Authorization: `Bearer ${token}`,
                },
              },
            );


            setRecommendations(response.data);
          } catch (error) {


              if (error.response?.status === 401 || error.response?.status === 403) {
                  session.clear();
                  navigate("/login");
                  return;
              }

            if (error.response?.status === 404) { navigate("/profile"); return; }
            setErro("Não foi possível carregar as recomendações");
          } finally {
            setLoading(false);
          }
        }

        carregarRecomendacoes();
      }, [navigate]);

    useEffect(() => {
        if (!hobbyExplicacao) return;

        const elementoAnterior = document.activeElement;

        botaoFecharRef.current?.focus();

        function controlarTeclado(event) {
            if (event.key === "Escape") {
                setHobbyExplicacao(null);
                return;
            }

            if (event.key !== "Tab") return;

            const elementos = modalRef.current?.querySelectorAll(
                'button:not(:disabled), a[href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'
            );

            if (!elementos || elementos.length === 0) {
                event.preventDefault();
                return;
            }

            const primeiro = elementos[0];
            const ultimo = elementos[elementos.length - 1];

            if (event.shiftKey && document.activeElement === primeiro) {
                event.preventDefault();
                ultimo.focus();
            } else if (!event.shiftKey && document.activeElement === ultimo) {
                event.preventDefault();
                primeiro.focus();
            } else if (!modalRef.current?.contains(document.activeElement)) {
                event.preventDefault();
                primeiro.focus();
            }
        }

        document.addEventListener("keydown", controlarTeclado);

        return () => {
            document.removeEventListener("keydown", controlarTeclado);
            elementoAnterior?.focus?.();
        };
    }, [hobbyExplicacao]);

      if (loading) {
          return (
              <main className="recommendations-page">
                  <div className="recommendation-loading">
                      <p>Carregando recomendações...</p>
                  </div>
              </main>
          );
      }

      if (erro) {
          return (
              <main className="recommendations-page">
                  <div className="recommendation-error">
                      <p>{erro}</p>
                  </div>
              </main>
          );
      }

        return (
            <main className="recommendations-page">
                <div className="recommendations-container">
                    <header className="recommendations-header">
                        <div>
                            <p className="recommendations-subtitle">
                                Hobbies sugeridos com base no seu perfil, interesses e preferências.
                            </p>
                        </div>


                    </header>

                    {recommendations.length === 0 ? (
                        <div className="recommendation-empty">
                            <h2>Nenhuma recomendação encontrada</h2>
                            <p>
                                Ainda não encontramos sugestões para o seu perfil.
                            </p>
                        </div>
                    ) : (
                        <section className="recommendations-grid">
                            {recommendations.map((item) => (
                                <article
                                    className="recommendation-card"
                                    key={item.hobbyId}
                                >

                                    {/* Categoria e score */}
                                    <div className="recommendation-card-header">

                                        <span className="recommendation-category">
                                            {item.categoria}
                                        </span>

                                        <span className="recommendation-score">
                                            {item.score} pts
                                        </span>

                                    </div>

                                    {/* Informações principais */}
                                    <div className="recommendation-card-content">

                                        <h2 className="recommendation-name">
                                            {item.nome}
                                        </h2>

                                        <p className="recommendation-description">
                                            {item.descricao}
                                        </p>

                                        {/* Motivos */}
                                        <div className="recommendation-reasons">

                                            {(item.motivos || []).slice(0, 3).map((motivo) => (
                                                <div
                                                    className="recommendation-reason-item"
                                                    key={motivo}
                                                >
                                                    <span className="reason-icon">✓</span>

                                                    <span>{motivo}</span>
                                                </div>
                                            ))}

                                        </div>

                                        {/* Alertas */}
                                        {(item.alertas || []).length > 0 && (
                                            <div className="recommendation-alerts">

                                                {(item.alertas || []).slice(0, 2).map((alerta) => (
                                                    <div
                                                        className="recommendation-alert-item"
                                                        key={alerta}
                                                    >
                                                        <span>⚠</span>
                                                        <span>{alerta}</span>
                                                    </div>
                                                ))}

                                            </div>
                                        )}

                                    </div>

                                    <div className="recommendation-explanation-action">
                                        <button
                                            type="button"
                                            className="recommendation-explanation-button"
                                            onClick={() => setHobbyExplicacao(item)}
                                        >
                                            Por que este hobby?
                                        </button>
                                    </div>

                                    {/* Ações */}
                                    <div className="recommendation-card-actions">

                                        <button
                                            type="button"
                                            className="recommendation-button-secondary"
                                            onClick={() =>
                                                registrarFeedback(
                                                    item.hobbyId,
                                                    "NAO_INTERESSADO"
                                                )
                                            }
                                        >
                                            Não me interessa
                                        </button>

                                        <button
                                            type="button"
                                            className="recommendation-button-primary"
                                            onClick={() =>
                                                registrarFeedback(
                                                    item.hobbyId,
                                                    "INTERESSADO"
                                                )
                                            }
                                        >
                                            Tenho interesse
                                        </button>

                                    </div>

                                    {/* Detalhes
                                    <button
                                        type="button"
                                        className="recommendation-details-link"
                                        onClick={() =>
                                            navigate(`/recommendations/${item.hobbyId}`)
                                        }
                                    >
                                        Ver detalhes →
                                    </button>
                                    */}
                                </article>
                            ))}
                        </section>
                    )}
                </div>
                {hobbyExplicacao && (
                    <div
                        className="explanation-overlay"
                        onClick={() => setHobbyExplicacao(null)}
                    >
                        <div
                            ref={modalRef}
                            className="explanation-modal"
                            role="dialog"
                            aria-modal="true"
                            aria-labelledby="explanation-title"
                            onClick={(event) => event.stopPropagation()}
                        >
                            <div className="explanation-header">
                                <h2 id="explanation-title">
                                    Por que {hobbyExplicacao.nome}?
                                </h2>

                                <button
                                    ref={botaoFecharRef}
                                    type="button"
                                    onClick={() => setHobbyExplicacao(null)}
                                    aria-label="Fechar explicação"
                                >
                                    ✕
                                </button>
                            </div>

                            <p>
                                Pontuação total:{" "}
                                <strong>{hobbyExplicacao.score} pontos</strong>
                            </p>

                            <h3>Como calculamos sua recomendação</h3>

                            <div className="explanation-criteria">
                                {(hobbyExplicacao.criterios || []).map((criterio) => {
                                    const maiorValor = Math.max(
                                        1,
                                        ...(hobbyExplicacao.criterios || [])
                                            .map((item) => Math.abs(item.pontos))
                                    );

                                    const porcentagem =
                                        (Math.abs(criterio.pontos) / maiorValor) * 100;

                                    return (
                                        <div
                                            className="explanation-criterion"
                                            key={criterio.criterio}
                                        >
                                            <div className="explanation-criterion-info">
                                                <span>
                                                    {nomesCriterios[criterio.criterio] ??
                                                    criterio.criterio}
                                                </span>

                                                <strong
                                                    className={
                                                        criterio.pontos < 0
                                                            ? "criterion-negative"
                                                            : criterio.pontos > 0
                                                                ? "criterion-positive"
                                                                : "criterion-neutral"
                                                    }
                                                >
                                                    {criterio.pontos > 0 ? "+" : ""}
                                                    {criterio.pontos.toLocaleString("pt-BR", {
                                                        minimumFractionDigits: 1,
                                                        maximumFractionDigits: 2
                                                    })} pts
                                                </strong>
                                            </div>

                                            <div className="explanation-bar-track">
                                                <div
                                                    className={
                                                        criterio.pontos < 0
                                                            ? "explanation-bar-negative"
                                                            : criterio.pontos > 0
                                                                ? "explanation-bar-positive"
                                                                : "explanation-bar-neutral"
                                                    }
                                                    style={{ width: `${porcentagem}%` }}
                                                />
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>

                            <h3>Por que combina com você?</h3>

                            <ul>
                                {(hobbyExplicacao.motivos || []).map((motivo) => (
                                    <li key={motivo}>{motivo}</li>
                                ))}
                            </ul>

                            {(hobbyExplicacao.alertas || []).length > 0 && (
                                <>
                                    <h3>Pontos de atenção</h3>

                                    <ul>
                                        {hobbyExplicacao.alertas.map((alerta) => (
                                            <li key={alerta}>{alerta}</li>
                                        ))}
                                    </ul>
                                </>
                            )}
                        </div>
                    </div>
                )}
            </main>
        );
    }
