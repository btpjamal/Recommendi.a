import { session } from "../../auth/session.js";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../../api/api";
import "../../pages/css/Recommendations.css";
import "./NotInterestedTab.css";

export default function NotInterestedTab() {

    const navigate = useNavigate();

        const [hobbies, setHobbies] = useState([]);
        const [loading, setLoading] = useState(true);
        const [erro, setErro] = useState("");

        useEffect(() => {
                carregarNaoInteressados();
            }, []);

    async function carregarNaoInteressados() {
            try {
                setLoading(true);
                setErro("");

                const userId = session.getItem("userId");
                const token = session.getItem("token");

                const response = await api.get(
                    `/recommendation-feedbacks/${userId}?tipo=NAO_INTERESSADO`,
                    {
                        headers: {
                            Authorization: `Bearer ${token}`,
                        },
                    }
                );

                setHobbies(response.data);

            } catch {


                setErro(
                    "Não foi possível carregar seus hobbies descartados."
                );
            } finally {
                setLoading(false);
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

        if (loading) {
            return <p>Carregando hobbies...</p>;
        }

        if (erro) {
            return <p>{erro}</p>;
        }

        if (hobbies.length === 0) {
            return (
                <section className="not-interested-section">

                    <div className="not-interested-empty">

                        <span className="not-interested-empty-icon">
                            ✨
                        </span>

                        <h2>Nenhum hobby descartado</h2>

                        <p>
                            Você ainda não marcou nenhum hobby como
                            desinteressante. Continue explorando novas
                            atividades na aba Descobrir.
                        </p>

                    </div>

                </section>
            );
        }

        return (
            <section className="not-interested-section">

                <div className="not-interested-header">
                    <div>
                        <h2>Não me interessa</h2>

                        <p>
                            Hobbies que você decidiu não acompanhar
                            no momento. Você pode reconsiderar sua
                            escolha quando quiser.
                        </p>
                    </div>
                </div>

                <div className="recommendations-grid">

                    {hobbies.map((hobby) => (

                        <article
                            key={hobby.hobbyId}
                            className="recommendation-card not-interested-card"
                        >

                            <div className="recommendation-card-header">

                                <span className="recommendation-category">
                                    {hobby.categoria}
                                </span>

                                <span className="not-interested-badge">
                                    Não me interessa
                                </span>

                            </div>

                            <div className="recommendation-card-content">

                                <h3 className="recommendation-name">
                                    {hobby.nome}
                                </h3>

                                <p className="recommendation-description">
                                    {hobby.descricao}
                                </p>

                            </div>

                            <button
                                type="button"
                                className="not-interested-restore-button"
                                onClick={() =>
                                    devolverParaDescobrir(hobby.hobbyId)
                                }
                            >
                                ↩ Voltar a descobrir
                            </button>

                        </article>

                    ))}

                </div>

            </section>
        );
}