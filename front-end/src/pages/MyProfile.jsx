import { API_BASE_URL } from "../api/config.js";
import { session } from "../auth/session.js";
import { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./css/MyProfile.css";

const formatarEnum = (valor) => {
        if (!valor) return "-";

        return valor
            .toLowerCase()
            .replaceAll("_", " ")
            .replace(/\b\w/g, letra => letra.toUpperCase());
    };

    const formatarMoeda = (valor) => {
        if (valor == null) return "-";

        return valor.toLocaleString("pt-BR", {
            style: "currency",
            currency: "BRL"
        });
    };

function HobbyGroup({
    titulo,
    hobbies = [],
    vazio,
    mostrarNivel = false
}) {

    const navigate = useNavigate();

    return (
        <div className="hobby-profile-group">

            <h3>{titulo}</h3>

            {hobbies.length === 0 ? (
                <p className="empty-hobbies">{vazio}</p>

            ) : (
                <div className="hobby-profile-list">

                    {hobbies.map((hobby) => (
                        <button
                            key={hobby.hobbyId}
                            className="hobby-profile-item"
                            onClick={() =>
                                navigate(
                                    `/recommendations/${hobby.hobbyId}`
                                )
                            }
                        >
                            <strong>{hobby.nome}</strong>

                            {mostrarNivel && hobby.nivelAtual && (
                                <span>
                                    {formatarEnum(hobby.nivelAtual)}
                                </span>
                            )}
                        </button>
                    ))}

                </div>
            )}

        </div>
    );
}

function MyProfile() {
    const [perfil, setPerfil] = useState(null);
    const [loading, setLoading] = useState(true);
    const [erro, setErro] = useState("");
    const navigate = useNavigate();

    useEffect(() => {
        carregarPerfil();
    }, []);

    const carregarPerfil = async () => {
        try {
            const token = session.getItem("token");

            const response = await axios.get(
                API_BASE_URL + "/profile/me",
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );

            setPerfil(response.data);

        } catch {

            setErro("Não foi possível carregar o perfil.");
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return <p>Carregando perfil...</p>;
    }

    if (erro) {
        return <p>{erro}</p>;
    }

    if (!perfil) {
        return <p>Perfil não encontrado.</p>;
    }


    return (
        <div className="profile-page">

            <div className="profile-container">

                {/* Cabeçalho */}
                <section className="profile-header">
                    <div>
                        <span className="profile-label">MEU PERFIL</span>

                        <h1>{perfil.nome}</h1>

                        <p className="profile-location">
                            {perfil.idade} anos
                            {perfil.cidade && ` • ${perfil.cidade}`}
                            {perfil.estado && ` - ${perfil.estado}`}
                        </p>
                    </div>

                    <button
                                        type="button"
                                        className="recommendations-button"
                                        onClick={() => navigate("/Recommendations")}
                                    >
                                        Descobrir hobbies
                                    </button>
                </section>

                {/* Dados do onboarding */}
                <section className="profile-section">
                    <div className="section-title">
                        <div>
                            <h2>Meu perfil</h2>
                            <p>Preferências utilizadas nas suas recomendações.</p>
                        </div>

                        <button
                            className="edit-profile-button"
                            onClick={() => navigate("/onboarding?editar=true")}
                        >
                            Atualizar perfil
                        </button>
                    </div>

                    <div className="profile-stats">

                        <div className="stat-card">
                            <span>Tempo disponível</span>
                            <strong>
                                {perfil.tempoDisponivelSemanal}h
                            </strong>
                            <small>por semana</small>
                        </div>

                        <div className="stat-card">
                            <span>Orçamento inicial</span>
                            <strong>
                                {formatarMoeda(perfil.orcamentoInicial)}
                            </strong>
                        </div>

                        <div className="stat-card">
                            <span>Socialização</span>
                            <strong>
                                {formatarEnum(perfil.tipoSocializacao)}
                            </strong>
                        </div>

                        <div className="stat-card">
                            <span>Atividade física</span>
                            <strong>
                                {formatarEnum(
                                    perfil.nivelAtividadeFisicaDesejada
                                )}
                            </strong>
                        </div>

                        <div className="stat-card">
                            <span>Ambiente</span>
                            <strong>
                                {formatarEnum(perfil.ambientePreferido)}
                            </strong>
                        </div>

                    </div>
                </section>

                {/* Interesses e objetivos */}
                <section className="profile-section profile-preferences">

                    <div>
                        <h2>Interesses</h2>

                        <div className="profile-tags">
                            {perfil.interesses?.map((interesse) => (
                                <span
                                    className="profile-tag"
                                    key={interesse}
                                >
                                    {interesse}
                                </span>
                            ))}
                        </div>
                    </div>

                    <div>
                        <h2>Objetivos</h2>

                        <div className="profile-tags">
                            {perfil.objetivos?.map((objetivo) => (
                                <span
                                    className="profile-tag"
                                    key={objetivo}
                                >
                                    {objetivo}
                                </span>
                            ))}
                        </div>
                    </div>

                </section>

                {/* Configurações de IA */}
                <section className="profile-section">
                    <div className="section-title">
                        <div>
                            <h2>Inteligência Artificial</h2>
                            <p>
                                Gerencie sua chave do Google Gemini para
                                gerar planos personalizados.
                            </p>
                        </div>

                        <button
                            type="button"
                            className="edit-profile-button"
                            onClick={() => navigate("/ai-settings")}
                        >
                            Configurar IA
                        </button>
                    </div>
                </section>

                {/* Hobbies */}
                <section className="profile-section">

                    <h2>Meus hobbies</h2>

                    <div className="hobby-profile-grid">

                        <HobbyGroup
                            titulo="Atualmente praticando"
                            hobbies={perfil.praticando}
                            vazio="Nenhum hobby sendo praticado no momento."
                            mostrarNivel
                        />

                        <HobbyGroup
                            titulo="Tenho interesse"
                            hobbies={perfil.interessados}
                            vazio="Nenhum hobby marcado como interessado."
                        />

                        <HobbyGroup
                            titulo="Recomendados para mim"
                            hobbies={perfil.recomendados}
                            vazio="Nenhuma nova sugestão disponível."
                        />

                    </div>

                </section>

            </div>

        </div>
    );
}

export default MyProfile;