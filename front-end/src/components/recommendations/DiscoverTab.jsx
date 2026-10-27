import { session } from "../../auth/session.js";
import { useEffect, useState } from "react";
import { api } from "../../api/api";
import { useNavigate } from "react-router";
import "../../pages/css/Recommendations.css";


export default function DiscoverTab() {

    const navigate = useNavigate();

    const [recommendations, setRecommendations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [erro, setErro] = useState(null);

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

                                                {(item.alertas || []).slice(0, 1).map((alerta) => (
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
            </main>
        );
    }
