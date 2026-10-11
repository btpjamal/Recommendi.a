
import { useCallback, useEffect, useState } from "react";
import { api } from "../api/api";

export default function AdminSuggestions() {
    const [sugestoes, setSugestoes] = useState([]);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState("");
    const [filtro, setFiltro] = useState("PENDENTE");

    const carregarSugestoes = useCallback(async () => {
        setCarregando(true);
        setErro("");

        try {
            const response = await api.get(
                "/admin/hobby-suggestions",
                {
                    params: filtro === "TODAS"
                        ? {}
                        : { status: filtro },

                    headers: {
                        Authorization:
                            `Bearer ${localStorage.getItem("token")}`
                    }
                }
            );

            setSugestoes(response.data);

        } catch (error) {
            console.error("Erro ao carregar sugestões:", error);

            setErro(
                error.response?.data?.message ||
                "Não foi possível carregar as sugestões."
            );

        } finally {
            setCarregando(false);
        }
    }, [filtro]);

    useEffect(() => {
        carregarSugestoes();
    }, [carregarSugestoes]);

    function formatarData(data) {
        if (!data) return "Não informada";

        return new Date(data).toLocaleDateString("pt-BR");
    }

    function formatarMoeda(valor) {
        if (valor == null) return "Não informado";

        return Number(valor).toLocaleString("pt-BR", {
            style: "currency",
            currency: "BRL"
        });
    }

    return (
        <div className="admin-suggestions">
            <div className="admin-page-heading">
                <h1>Sugestões de hobbies</h1>

                <p>
                    Analise os hobbies sugeridos pela comunidade
                    antes de adicioná-los ao catálogo oficial.
                </p>
            </div>

            <div className="admin-suggestions-toolbar">
                <div>
                    <strong>
                        {sugestoes.length} {
                        filtro === "TODAS"
                            ? "sugestões"
                            : filtro === "PENDENTE"
                                ? "sugestões pendentes"
                                : filtro === "APROVADA"
                                    ? "sugestões aprovadas"
                                    : "sugestões rejeitadas"
                    }
                    </strong>
                </div>

                <button
                    type="button"
                    onClick={carregarSugestoes}
                    disabled={carregando}
                    className="admin-refresh-button"
                >
                    Atualizar lista
                </button>
            </div>

            <div className="admin-suggestions-filters">
                {[
                    { valor: "TODAS", nome: "Todas" },
                    { valor: "PENDENTE", nome: "Pendentes" },
                    { valor: "APROVADA", nome: "Aprovadas" },
                    { valor: "REJEITADA", nome: "Rejeitadas" }
                ].map((opcao) => (
                    <button
                        key={opcao.valor}
                        type="button"
                        onClick={() => setFiltro(opcao.valor)}
                        className={
                            filtro === opcao.valor ? "active" : ""
                        }
                    >
                        {opcao.nome}
                    </button>
                ))}
            </div>

            {carregando && (
                <p className="admin-loading">
                    Carregando sugestões...
                </p>
            )}

            {erro && (
                <p className="admin-error" role="alert">
                    {erro}
                </p>
            )}

            {!carregando && !erro && sugestoes.length === 0 && (
                <div className="admin-empty-state">
                    <h2>Nenhuma sugestão encontrada</h2>

                    <p>
                        Não existem sugestões para o filtro selecionado.
                    </p>
                </div>
            )}

            {!carregando && !erro && sugestoes.length > 0 && (
                <div className="admin-suggestions-list">
                    {sugestoes.map((sugestao) => (
                        <article
                            key={sugestao.id}
                            className="admin-suggestion-card"
                        >
                            <div className="admin-suggestion-header">
                                <div>
                                    <h2>{sugestao.nome}</h2>

                                    <span className="admin-suggestion-category">
                                        {sugestao.categoryNome ||
                                            "Sem categoria"}
                                    </span>
                                </div>

                                <span
                                    className={`admin-status-badge ${
                                        sugestao.status?.toLowerCase() || ""
                                    }`}
                                >
                                    {sugestao.status === "PENDENTE"
                                        ? "Pendente"
                                        : sugestao.status === "APROVADA"
                                            ? "Aprovada"
                                            : sugestao.status === "REJEITADA"
                                                ? "Rejeitada"
                                                : sugestao.status || "Desconhecido"}
                                </span>
                            </div>

                            <p className="admin-suggestion-description">
                                {sugestao.descricao}
                            </p>

                            <div className="admin-suggestion-details">
                                <div>
                                    <small>Custo inicial</small>
                                    <strong>
                                        {formatarMoeda(
                                            sugestao.custoEstimado
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <small>Custo recorrente</small>
                                    <strong>
                                        {formatarMoeda(
                                            sugestao.custoRecorrenteEstimado
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <small>Dificuldade</small>
                                    <strong>
                                        {sugestao.nivelDificuldade ?? "—"}
                                    </strong>
                                </div>

                                <div>
                                    <small>Tempo necessário</small>
                                    <strong>
                                        {sugestao.tempoNecessario != null
                                            ? `${sugestao.tempoNecessario} h`
                                            : "Não informado"}
                                    </strong>
                                </div>
                            </div>

                            <div className="admin-suggestion-footer">
                                <span>
                                    Enviado por{" "}
                                    <strong>
                                        {sugestao.createdByNome ||
                                            "Usuário"}
                                    </strong>
                                </span>

                                <span>
                                    {formatarData(sugestao.createdAt)}
                                </span>
                            </div>
                        </article>
                    ))}
                </div>
            )}
        </div>
    );
}
