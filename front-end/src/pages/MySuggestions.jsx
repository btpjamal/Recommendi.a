import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../api/api";

import "./css/MySuggestions.css";
import "./css/RecommendationsLayout.css";

const STATUS = {
    PENDENTE: "Pendente",
    APROVADA: "Aprovada",
    REJEITADA: "Rejeitada"
};

export default function MySuggestions() {
    const navigate = useNavigate();

    const [sugestoes, setSugestoes] = useState([]);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState("");
    const [filtro, setFiltro] = useState("TODAS");

    const nome = localStorage.getItem("nome");

    useEffect(() => {
        async function carregarSugestoes() {
            try {
                setCarregando(true);
                setErro("");

                const response = await api.get(
                    "/hobby-suggestions/mine",
                    {
                        headers: {
                            Authorization: `Bearer ${localStorage.getItem("token")}`
                        }
                    }
                );

                setSugestoes(
                    Array.isArray(response.data) ? response.data : []
                );
            } catch (error) {
                console.error(error);
                setErro("Não foi possível carregar suas sugestões.");
            } finally {
                setCarregando(false);
            }
        }

        carregarSugestoes();
    }, []);

    const sugestoesFiltradas = sugestoes.filter(
        sugestao =>
            filtro === "TODAS" || sugestao.status === filtro
    );

    function formatarData(data) {
        if (!data) return "—";

        const dataFormatada = new Date(data);

        return Number.isNaN(dataFormatada.getTime())
            ? "—"
            : dataFormatada.toLocaleDateString("pt-BR");
    }

    function logout() {
        localStorage.clear();
        navigate("/login");
    }

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
                            onClick={() => navigate("/community")}
                        >
                            Comunidade
                        </button>

                        <button
                            className="app-header-button"
                            onClick={() => navigate("/suggest-hobby")}
                        >
                            Sugerir hobby
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
                <div className="my-suggestions-heading">
                    <div>
                        <span className="community-label">
                            CONTRIBUIÇÕES
                        </span>
                        <h1>Minhas sugestões</h1>
                        <p>
                            Acompanhe a análise dos hobbies que você
                            sugeriu para o Recommendi.a.
                        </p>
                    </div>

                    <button
                        className="community-primary-button"
                        onClick={() => navigate("/suggest-hobby")}
                    >
                        + Nova sugestão
                    </button>
                </div>

                <div className="my-suggestions-filters">
                    {["TODAS", "PENDENTE", "APROVADA", "REJEITADA"].map(
                        opcao => (
                            <button
                                key={opcao}
                                className={filtro === opcao ? "active" : ""}
                                onClick={() => setFiltro(opcao)}
                            >
                                {opcao === "TODAS"
                                    ? "Todas"
                                    : STATUS[opcao]}
                            </button>
                        )
                    )}
                </div>

                {carregando && (
                    <div className="community-state">
                        <h2>Carregando sugestões...</h2>
                    </div>
                )}

                {!carregando && erro && (
                    <div className="community-error">
                        {erro}
                    </div>
                )}

                {!carregando && !erro && sugestoesFiltradas.length === 0 && (
                    <div className="community-state">
                        <div className="community-state-icon">✦</div>
                        <h2>Nenhuma sugestão encontrada</h2>
                        <p>
                            {filtro === "TODAS"
                                ? "Você ainda não enviou sugestões de hobbies."
                                : "Você não possui sugestões com esse status."}
                        </p>
                        {filtro === "TODAS" && (
                            <button
                                className="community-primary-button"
                                onClick={() => navigate("/suggest-hobby")}
                            >
                                Sugerir meu primeiro hobby
                            </button>
                        )}
                    </div>
                )}

                {!carregando && !erro && sugestoesFiltradas.length > 0 && (
                    <div className="my-suggestions-grid">
                        {sugestoesFiltradas.map(sugestao => (
                            <article
                                className="my-suggestion-card"
                                key={sugestao.id}
                            >
                                <div className="my-suggestion-card-top">
                                    <div>
                                        <span className="my-suggestion-category">
                                            {sugestao.categoryNome || "Hobby"}
                                        </span>
                                        <h2>{sugestao.nome}</h2>
                                    </div>

                                    <span
                                        className={`my-suggestion-status status-${sugestao.status?.toLowerCase()}`}
                                    >
                                        {STATUS[sugestao.status] || sugestao.status}
                                    </span>
                                </div>

                                <p className="my-suggestion-description">
                                    {sugestao.descricao}
                                </p>

                                <div className="my-suggestion-meta">
                                    <span>
                                        Enviada em {formatarData(sugestao.createdAt)}
                                    </span>
                                </div>

                                {sugestao.status === "APROVADA" && (
                                    <div className="my-suggestion-feedback approved">
                                        Sua sugestão foi aprovada e adicionada
                                        ao catálogo de hobbies!
                                    </div>
                                )}

                                {sugestao.status === "REJEITADA" && (
                                    <div className="my-suggestion-feedback rejected">
                                        <strong>Resultado da avaliação</strong>
                                        <p>
                                            {sugestao.reviewNotes ||
                                                "Sugestão não aprovada pela administração."}
                                        </p>
                                    </div>
                                )}
                            </article>
                        ))}
                    </div>
                )}
            </section>
        </main>
    );
}