
import { useCallback, useEffect, useState } from "react";
import { api } from "../api/api";

export default function AdminSuggestions() {
    const [sugestoes, setSugestoes] = useState([]);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState("");
    const [filtro, setFiltro] = useState("PENDENTE");
    const [sugestaoEmAnalise, setSugestaoEmAnalise] = useState(null);
    const [tipoAnalise, setTipoAnalise] = useState(null);
    const [justificativa, setJustificativa] = useState("");
    const [processando, setProcessando] = useState(false);
    const [erroAnalise, setErroAnalise] = useState("");
    const [sucesso, setSucesso] = useState("");

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

    function abrirAnalise(sugestao, tipo) {
        setSugestaoEmAnalise(sugestao);
        setTipoAnalise(tipo);
        setJustificativa("");
        setErroAnalise("");
        setSucesso("");
    }

    function fecharAnalise() {
        if (processando) return;

        setSugestaoEmAnalise(null);
        setTipoAnalise(null);
        setJustificativa("");
        setErroAnalise("");
    }

    async function rejeitarSugestao() {
        if (!sugestaoEmAnalise || processando) return;

        const motivo = justificativa.trim();

        if (!motivo) {
            setErroAnalise("Informe a justificativa da rejeição.");
            return;
        }

        if (motivo.length > 2000) {
            setErroAnalise(
                "A justificativa deve ter no máximo 2000 caracteres."
            );
            return;
        }

        try {
            setProcessando(true);
            setErroAnalise("");
            setSucesso("");

            await api.patch(
                `/admin/hobby-suggestions/${sugestaoEmAnalise.id}/reject`,
                {
                    justificativa: motivo
                },
                {
                    headers: {
                        Authorization:
                            `Bearer ${localStorage.getItem("token")}`
                    }
                }
            );

            setSucesso(
                `A sugestão "${sugestaoEmAnalise.nome}" foi rejeitada.`
            );

            setSugestaoEmAnalise(null);
            setTipoAnalise(null);
            setJustificativa("");

            await carregarSugestoes();

        } catch (error) {
            console.error("Erro ao rejeitar sugestão:", error);

            setErroAnalise(
                error.response?.data?.message ||
                error.response?.data?.error ||
                "Não foi possível rejeitar a sugestão."
            );

        } finally {
            setProcessando(false);
        }
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

            {sucesso && (
                <div className="admin-success" role="status">
                    {sucesso}
                </div>
            )}

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
                            {sugestao.status === "PENDENTE" && (
                                <div className="admin-suggestion-actions">
                                    <button
                                        type="button"
                                        className="admin-approve-button"
                                        onClick={() => abrirAnalise(sugestao, "APROVAR")}
                                    >
                                        Aprovar sugestão
                                    </button>

                                    <button
                                        type="button"
                                        className="admin-reject-button"
                                        onClick={() => abrirAnalise(sugestao, "REJEITAR")}
                                    >
                                        Rejeitar
                                    </button>
                                </div>
                            )}
                        </article>
                    ))}
                    {sugestaoEmAnalise && (
                        <div className="admin-review-overlay">
                            <div
                                className="admin-review-modal"
                                role="dialog"
                                aria-modal="true"
                                aria-labelledby="admin-review-title"
                            >
                                <h2 id="admin-review-title">
                                    {tipoAnalise === "APROVAR"
                                        ? "Aprovar sugestão"
                                        : "Rejeitar sugestão"}
                                </h2>

                                <p>
                                    Hobby: <strong>{sugestaoEmAnalise.nome}</strong>
                                </p>

                                <label htmlFor="admin-review-notes">
                                    {tipoAnalise === "REJEITAR"
                                        ? "Justificativa da rejeição"
                                        : "Observações da aprovação (opcional)"}
                                </label>

                                <textarea
                                    id="admin-review-notes"
                                    value={justificativa}
                                    onChange={(event) =>
                                        setJustificativa(event.target.value)
                                    }
                                    rows={4}
                                    maxLength={2000}
                                    placeholder={
                                        tipoAnalise === "REJEITAR"
                                            ? "Explique por que a sugestão não será aprovada..."
                                            : "Adicione observações, se necessário..."
                                    }
                                />

                                {erroAnalise && (
                                    <p className="admin-error" role="alert">
                                        {erroAnalise}
                                    </p>
                                )}

                                <div className="admin-review-actions">
                                    <button
                                        type="button"
                                        className="admin-review-cancel"
                                        onClick={fecharAnalise}
                                    >
                                        Cancelar
                                    </button>

                                    <button
                                        type="button"
                                        className={
                                            tipoAnalise === "APROVAR"
                                                ? "admin-approve-button"
                                                : "admin-reject-button"
                                        }
                                        disabled={
                                            processando ||
                                            tipoAnalise === "APROVAR" ||
                                            (
                                                tipoAnalise === "REJEITAR" &&
                                                !justificativa.trim()
                                            )
                                        }
                                        onClick={
                                            tipoAnalise === "REJEITAR"
                                                ? rejeitarSugestao
                                                : undefined
                                        }
                                    >
                                        {processando
                                            ? "Processando..."
                                            : tipoAnalise === "APROVAR"
                                                ? "Confirmar aprovação"
                                                : "Confirmar rejeição"}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
