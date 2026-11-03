import { session } from "../auth/session.js";

import { useCallback, useEffect, useRef, useState } from "react";
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
    const [dadosAprovacao, setDadosAprovacao] = useState(null);
    const [categorias, setCategorias] = useState([]);
    const [carregandoCategorias, setCarregandoCategorias] = useState(false);
    const [erroCategorias, setErroCategorias] = useState("");
    const modalRef = useRef(null);

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
                            `Bearer ${session.getItem("token")}`
                    }
                }
            );

            setSugestoes(response.data);

        } catch (error) {


            setErro(
                error.response?.data?.message ||
                "Não foi possível carregar as sugestões."
            );
            setSugestoes([]);

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

        if (tipo === "APROVAR") {
            carregarCategorias();

            setDadosAprovacao({
                nome: sugestao.nome || "",
                descricao: sugestao.descricao || "",
                imagemUrl: "",
                custoEstimado: sugestao.custoEstimado ?? 0,
                custoRecorrenteEstimado:
                    sugestao.custoRecorrenteEstimado ?? "",
                nivelDificuldade: sugestao.nivelDificuldade ?? 1,
                tempoNecessario: sugestao.tempoNecessario ?? "",
                tipoSocializacao: sugestao.tipoSocializacao || "INDIFERENTE",
                nivelAtividadeFisica:
                    sugestao.nivelAtividadeFisica || "INDIFERENTE",
                ambiente: sugestao.ambiente || "INDIFERENTE",
                requerEquipamento: sugestao.requerEquipamento ?? false,
                categoryId: sugestao.categoryId,
                observacoes: ""
            });
        } else {
            setDadosAprovacao(null);
        }
    }

    function fecharAnalise() {
        if (processando) return;

        setSugestaoEmAnalise(null);
        setTipoAnalise(null);
        setJustificativa("");
        setErroAnalise("");
        setDadosAprovacao(null);
    }

    useEffect(() => {
        if (!sugestaoEmAnalise) return;

        const elementoAnterior = document.activeElement;
        const modal = modalRef.current;

        modal?.focus();

        function controlarTeclado(event) {
            if (event.key === "Escape") {
                if (!processando) {
                    fecharAnalise();
                }
                return;
            }

            if (event.key !== "Tab" || !modal) return;

            const elementos = Array.from(
                modal.querySelectorAll(
                    'button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href]'
                )
            ).filter((elemento) => elemento.getClientRects().length > 0);

            if (elementos.length === 0) {
                event.preventDefault();
                modal.focus();
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
            }
        }

        document.addEventListener("keydown", controlarTeclado);

        return () => {
            document.removeEventListener("keydown", controlarTeclado);

            if (elementoAnterior instanceof HTMLElement) {
                elementoAnterior.focus();
            }
        };
    }, [sugestaoEmAnalise, processando]);

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
                            `Bearer ${session.getItem("token")}`
                    }
                }
            );

            setSucesso(
                `A sugestão "${sugestaoEmAnalise.nome}" foi rejeitada com sucesso.`
            );

            setSugestaoEmAnalise(null);
            setTipoAnalise(null);
            setJustificativa("");

            await carregarSugestoes();

        } catch (error) {


            setErroAnalise(
                error.response?.data?.message ||
                error.response?.data?.error ||
                "Não foi possível rejeitar a sugestão."
            );

        } finally {
            setProcessando(false);
        }
    }

    async function aprovarSugestao() {
        if (!sugestaoEmAnalise || !dadosAprovacao || processando) return;

        const payload = {
            ...dadosAprovacao,
            nome: dadosAprovacao.nome.trim(),
            descricao: dadosAprovacao.descricao.trim(),
            imagemUrl: dadosAprovacao.imagemUrl.trim() || null,
            custoEstimado:
                dadosAprovacao.custoEstimado === "" ||
                dadosAprovacao.custoEstimado == null
                    ? null
                    : Number(dadosAprovacao.custoEstimado),
            custoRecorrenteEstimado:
                dadosAprovacao.custoRecorrenteEstimado === ""
                    ? null
                    : Number(dadosAprovacao.custoRecorrenteEstimado),
            nivelDificuldade: Number(dadosAprovacao.nivelDificuldade),
            tempoNecessario: Number(dadosAprovacao.tempoNecessario),
            categoryId: Number(dadosAprovacao.categoryId),
            observacoes: dadosAprovacao.observacoes.trim() || null
        };

        if (
            !payload.nome ||
            payload.nome.length > 150 ||
            !payload.descricao ||
            payload.custoEstimado === null ||
            !Number.isFinite(payload.custoEstimado) ||
            payload.custoEstimado < 0 ||
            !Number.isInteger(payload.nivelDificuldade) ||
            payload.nivelDificuldade < 1 ||
            payload.nivelDificuldade > 5 ||
            !Number.isFinite(payload.tempoNecessario) ||
            payload.tempoNecessario <= 0 ||
            !Number.isInteger(payload.categoryId) ||
            payload.categoryId <= 0 ||
            (
                payload.custoRecorrenteEstimado !== null &&
                (
                    !Number.isFinite(payload.custoRecorrenteEstimado) ||
                    payload.custoRecorrenteEstimado < 0
                )
            ) ||
            payload.observacoes?.length > 2000
        ) {
            setErroAnalise("Revise os campos obrigatórios e seus valores.");
            return;
        }

        try {
            setProcessando(true);
            setErroAnalise("");
            setSucesso("");

            await api.patch(
                `/admin/hobby-suggestions/${sugestaoEmAnalise.id}/approve`,
                payload,
                {
                    headers: {
                        Authorization: `Bearer ${session.getItem("token")}`
                    }
                }
            );

            setSucesso(
                `A sugestão "${payload.nome}" foi aprovada. O hobby foi adicionado ao catálogo.`
            );

            setSugestaoEmAnalise(null);
            setTipoAnalise(null);
            setDadosAprovacao(null);
            setJustificativa("");

            await carregarSugestoes();

        } catch (error) {


            setErroAnalise(
                error.response?.data?.message ||
                error.response?.data?.error ||
                "Não foi possível aprovar a sugestão."
            );

        } finally {
            setProcessando(false);
        }
    }

    const carregarCategorias = useCallback(async () => {
        try {
            setCarregandoCategorias(true);
            setErroCategorias("");

            const response = await api.get("/hobby-categories", {
                headers: {
                    Authorization: `Bearer ${session.getItem("token")}`
                }
            });

            setCategorias(response.data);

        } catch {


            setErroCategorias(
                "Não foi possível carregar as categorias."
            );
        } finally {
            setCarregandoCategorias(false);
        }
    }, []);

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
                            {sugestao.status !== "PENDENTE" && (
                                <div className="admin-suggestion-review">
                                    <div className="admin-suggestion-review-heading">
                                        <h3>Histórico da avaliação</h3>

                                        <span
                                            className={`admin-status-badge ${
                                                sugestao.status?.toLowerCase() || ""
                                            }`}
                                        >
                {sugestao.status === "APROVADA"
                    ? "Aprovada"
                    : "Rejeitada"}
            </span>
                                    </div>

                                    <div className="admin-suggestion-review-details">
                                        <div>
                                            <small>Data da avaliação</small>
                                            <strong>
                                                {sugestao.reviewedAt
                                                    ? formatarData(sugestao.reviewedAt)
                                                    : "Não informada"}
                                            </strong>
                                        </div>

                                        {sugestao.status === "APROVADA" &&
                                            sugestao.approvedHobbyId != null && (
                                                <div>
                                                    <small>Hobby cadastrado</small>
                                                    <strong>
                                                        ID #{sugestao.approvedHobbyId}
                                                    </strong>
                                                </div>
                                            )}
                                    </div>

                                    <div className="admin-suggestion-review-notes">
                                        <small>
                                            {sugestao.status === "REJEITADA"
                                                ? "Justificativa da rejeição"
                                                : "Observações da aprovação"}
                                        </small>

                                        <p>
                                            {sugestao.reviewNotes ||
                                                "Nenhuma observação registrada."}
                                        </p>
                                    </div>
                                </div>
                            )}
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
                                ref={modalRef}
                                className="admin-review-modal"
                                role="dialog"
                                aria-modal="true"
                                aria-labelledby="admin-review-title"
                                tabIndex={-1}
                            >
                                <h2 id="admin-review-title">
                                    {tipoAnalise === "APROVAR"
                                        ? "Aprovar sugestão"
                                        : "Rejeitar sugestão"}
                                </h2>

                                <p>
                                    Hobby: <strong>{sugestaoEmAnalise.nome}</strong>
                                </p>

                                {tipoAnalise === "REJEITAR" ? (
                                    <>
                                        <label htmlFor="admin-review-notes">
                                            Justificativa da rejeição
                                        </label>

                                        <textarea
                                            id="admin-review-notes"
                                            value={justificativa}
                                            onChange={(event) => setJustificativa(event.target.value)}
                                            rows={4}
                                            maxLength={2000}
                                            placeholder="Explique por que a sugestão não será aprovada..."
                                        />
                                    </>
                                ) : dadosAprovacao && (
                                    <div className="admin-approval-form">
                                        <label>
                                            Nome do hobby
                                            <input
                                                type="text"
                                                maxLength={150}
                                                value={dadosAprovacao.nome}
                                                onChange={(e) =>
                                                    setDadosAprovacao({
                                                        ...dadosAprovacao,
                                                        nome: e.target.value
                                                    })
                                                }
                                            />
                                        </label>

                                        <label>
                                            Descrição
                                            <textarea
                                                rows={4}
                                                value={dadosAprovacao.descricao}
                                                onChange={(e) =>
                                                    setDadosAprovacao({
                                                        ...dadosAprovacao,
                                                        descricao: e.target.value
                                                    })
                                                }
                                            />
                                        </label>

                                        <div className="admin-approval-grid">
                                            <label>
                                                Custo inicial (R$)
                                                <input
                                                    type="number"
                                                    min="0"
                                                    step="0.01"
                                                    value={dadosAprovacao.custoEstimado}
                                                    onChange={(e) =>
                                                        setDadosAprovacao({
                                                            ...dadosAprovacao,
                                                            custoEstimado: e.target.value
                                                        })
                                                    }
                                                />
                                            </label>

                                            <label>
                                                Custo recorrente (R$)
                                                <input
                                                    type="number"
                                                    min="0"
                                                    step="0.01"
                                                    value={dadosAprovacao.custoRecorrenteEstimado}
                                                    onChange={(e) =>
                                                        setDadosAprovacao({
                                                            ...dadosAprovacao,
                                                            custoRecorrenteEstimado: e.target.value
                                                        })
                                                    }
                                                />
                                            </label>

                                            <label>
                                                Dificuldade (1 a 5)
                                                <input
                                                    type="number"
                                                    min="1"
                                                    max="5"
                                                    step="1"
                                                    value={dadosAprovacao.nivelDificuldade}
                                                    onChange={(e) =>
                                                        setDadosAprovacao({
                                                            ...dadosAprovacao,
                                                            nivelDificuldade: e.target.value
                                                        })
                                                    }
                                                />
                                            </label>

                                            <label>
                                                Tempo necessário (horas)
                                                <input
                                                    type="number"
                                                    min="0.1"
                                                    step="0.1"
                                                    value={dadosAprovacao.tempoNecessario}
                                                    onChange={(e) =>
                                                        setDadosAprovacao({
                                                            ...dadosAprovacao,
                                                            tempoNecessario: e.target.value
                                                        })
                                                    }
                                                />
                                            </label>

                                            <label>
                                                Socialização
                                                <select
                                                    value={dadosAprovacao.tipoSocializacao}
                                                    onChange={(e) =>
                                                        setDadosAprovacao({
                                                            ...dadosAprovacao,
                                                            tipoSocializacao: e.target.value
                                                        })
                                                    }
                                                >
                                                    <option value="INDIVIDUAL">Individual</option>
                                                    <option value="SOCIAL">Social</option>
                                                    <option value="INDIFERENTE">Indiferente</option>
                                                </select>
                                            </label>

                                            <label>
                                                Atividade física
                                                <select
                                                    value={dadosAprovacao.nivelAtividadeFisica}
                                                    onChange={(e) =>
                                                        setDadosAprovacao({
                                                            ...dadosAprovacao,
                                                            nivelAtividadeFisica: e.target.value
                                                        })
                                                    }
                                                >
                                                    <option value="BAIXO">Baixa</option>
                                                    <option value="MODERADO">Moderada</option>
                                                    <option value="ALTO">Alta</option>
                                                    <option value="INDIFERENTE">Indiferente</option>
                                                </select>
                                            </label>

                                            <label>
                                                Ambiente
                                                <select
                                                    value={dadosAprovacao.ambiente}
                                                    onChange={(e) =>
                                                        setDadosAprovacao({
                                                            ...dadosAprovacao,
                                                            ambiente: e.target.value
                                                        })
                                                    }
                                                >
                                                    <option value="CASA">Casa</option>
                                                    <option value="AO_AR_LIVRE">Ao ar livre</option>
                                                    <option value="AMBIENTE_FECHADO">Ambiente fechado</option>
                                                    <option value="INDIFERENTE">Indiferente</option>
                                                </select>
                                            </label>
                                        </div>

                                        <label>
                                            Categoria

                                            <select
                                                value={dadosAprovacao.categoryId ?? ""}
                                                disabled={carregandoCategorias || !!erroCategorias}
                                                onChange={(e) =>
                                                    setDadosAprovacao({
                                                        ...dadosAprovacao,
                                                        categoryId: e.target.value
                                                    })
                                                }
                                            >
                                                <option value="">
                                                    {carregandoCategorias
                                                        ? "Carregando categorias..."
                                                        : "Selecione uma categoria"}
                                                </option>

                                                {categorias.map((categoria) => (
                                                    <option
                                                        key={categoria.id}
                                                        value={categoria.id}
                                                    >
                                                        {categoria.nome}
                                                    </option>
                                                ))}
                                            </select>

                                            {erroCategorias && (
                                                <span className="admin-category-error">
                                                    {erroCategorias}
                                                </span>
                                            )}
                                        </label>

                                        <label className="admin-approval-checkbox">
                                            <input
                                                type="checkbox"
                                                checked={dadosAprovacao.requerEquipamento}
                                                onChange={(e) =>
                                                    setDadosAprovacao({
                                                        ...dadosAprovacao,
                                                        requerEquipamento: e.target.checked
                                                    })
                                                }
                                            />
                                            Requer equipamento
                                        </label>

                                        <label>
                                            URL da imagem (opcional)
                                            <input
                                                type="text"
                                                value={dadosAprovacao.imagemUrl}
                                                onChange={(e) =>
                                                    setDadosAprovacao({
                                                        ...dadosAprovacao,
                                                        imagemUrl: e.target.value
                                                    })
                                                }
                                            />
                                        </label>

                                        <label>
                                            Observações (opcional)
                                            <textarea
                                                rows={3}
                                                maxLength={2000}
                                                value={dadosAprovacao.observacoes}
                                                onChange={(e) =>
                                                    setDadosAprovacao({
                                                        ...dadosAprovacao,
                                                        observacoes: e.target.value
                                                    })
                                                }
                                            />
                                        </label>
                                    </div>
                                )}

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
                                        disabled={processando}
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
                                            (
                                                tipoAnalise === "REJEITAR" &&
                                                !justificativa.trim()
                                            ) ||
                                            (
                                                tipoAnalise === "APROVAR" &&
                                                (
                                                    carregandoCategorias ||
                                                    !!erroCategorias ||
                                                    categorias.length === 0 ||
                                                    !categorias.some(
                                                        categoria =>
                                                            Number(categoria.id) ===
                                                            Number(dadosAprovacao?.categoryId)
                                                    )
                                                )
                                            )
                                        }
                                        onClick={
                                            tipoAnalise === "APROVAR"
                                                ? aprovarSugestao
                                                : rejeitarSugestao
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
