import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../api/api";

const FORM_INICIAL = {
    nome: "", descricao: "", imagemUrl: "", categoryId: "",
    custoEstimado: "", custoRecorrenteEstimado: "", nivelDificuldade: "",
    tempoNecessario: "", tipoSocializacao: "", nivelAtividadeFisica: "",
    ambiente: "", requerEquipamento: false
};

const OPCOES = {
    tipoSocializacao: ["INDIVIDUAL", "SOCIAL", "INDIFERENTE"],
    nivelAtividadeFisica: ["BAIXO", "MODERADO", "ALTO", "INDIFERENTE"],
    ambiente: ["CASA", "AO_AR_LIVRE", "AMBIENTE_FECHADO", "INDIFERENTE"]
};

const rotulo = (valor) => valor.replaceAll("_", " ").toLowerCase()
    .replace(/^./, (letra) => letra.toUpperCase());

export default function AdminHobbies() {
    const [hobbies, setHobbies] = useState([]);
    const [busca, setBusca] = useState("");
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState("");
    const [sucesso, setSucesso] = useState("");
    const [modalAberto, setModalAberto] = useState(false);
    const [hobbyEmEdicao, setHobbyEmEdicao] = useState(null);
    const [form, setForm] = useState(FORM_INICIAL);
    const [categorias, setCategorias] = useState([]);
    const [carregandoCategorias, setCarregandoCategorias] = useState(false);
    const [erroCategorias, setErroCategorias] = useState("");
    const [erroCadastro, setErroCadastro] = useState("");
    const [salvando, setSalvando] = useState(false);
    const salvandoRef = useRef(false);
    const modalRef = useRef(null);
    const botaoNovoRef = useRef(null);
    const botaoOrigemRef = useRef(null);
    const [alterandoStatusId, setAlterandoStatusId] = useState(null);
    const [filtroStatus, setFiltroStatus] = useState("TODOS");
    const [ordenacao, setOrdenacao] = useState("RECENTES");
    const [hobbyParaDesativar, setHobbyParaDesativar] = useState(null);
    const [erroDesativacao, setErroDesativacao] = useState("");
    const modalConfirmacaoRef = useRef(null);
    const botaoCancelarRef = useRef(null);
    const botaoDesativarOrigemRef = useRef(null);
    const processandoDesativacaoRef = useRef(false);

    const headers = () => ({ Authorization: `Bearer ${localStorage.getItem("token")}` });

    const carregarHobbies = useCallback(async () => {
        try {
            setCarregando(true);
            setErro("");
            const response = await api.get("/hobbies", { headers: headers() });
            setHobbies(response.data);
        } catch (error) {
            console.error("Erro ao carregar hobbies:", error);
            setHobbies([]);
            setErro(error.response?.data?.message || "Não foi possível carregar os hobbies.");
        } finally {
            setCarregando(false);
        }
    }, []);

    useEffect(() => { carregarHobbies(); }, [carregarHobbies]);

    useEffect(() => {
        if (!modalAberto) return;
        let ativo = true;
        async function carregarCategorias() {
            setCarregandoCategorias(true);
            setErroCategorias("");
            try {
                const response = await api.get("/hobby-categories", { headers: headers() });
                if (ativo) setCategorias(response.data);
            } catch (error) {
                console.error("Erro ao carregar categorias:", error);
                if (ativo) setErroCategorias("Não foi possível carregar as categorias.");
            } finally {
                if (ativo) setCarregandoCategorias(false);
            }
        }
        carregarCategorias();
        modalRef.current?.querySelector("input[name='nome']")?.focus();
        return () => { ativo = false; };
    }, [modalAberto]);

    useEffect(() => {
        if (!modalAberto) return;
        function aoTeclar(event) {
            if (event.key === "Escape" && !salvandoRef.current) {
                setModalAberto(false);
                requestAnimationFrame(() => botaoOrigemRef.current?.focus());
            }
            if (event.key !== "Tab" || !modalRef.current) return;
            const elementos = [...modalRef.current.querySelectorAll(
                'button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled)'
            )];
            if (!elementos.length) return;
            const primeiro = elementos[0];
            const ultimo = elementos[elementos.length - 1];
            if (event.shiftKey && document.activeElement === primeiro) {
                event.preventDefault(); ultimo.focus();
            } else if (!event.shiftKey && document.activeElement === ultimo) {
                event.preventDefault(); primeiro.focus();
            }
        }
        document.addEventListener("keydown", aoTeclar);
        return () => document.removeEventListener("keydown", aoTeclar);
    }, [modalAberto]);

    useEffect(() => {
        if (!hobbyParaDesativar) return;

        const modal = modalConfirmacaoRef.current;
        const focoAnterior = document.activeElement;

        botaoCancelarRef.current?.focus();

        function aoTeclar(event) {
            if (event.key === "Escape") {
                event.preventDefault();

                if (!processandoDesativacaoRef.current) {
                    fecharConfirmacao();
                }
                return;
            }

            if (event.key !== "Tab" || !modal) return;

            const elementos = Array.from(
                modal.querySelectorAll(
                    'button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'
                )
            ).filter((elemento) => elemento.getClientRects().length > 0);

            if (!elementos.length) {
                event.preventDefault();
                modal.focus();
                return;
            }

            const primeiro = elementos[0];
            const ultimo = elementos[elementos.length - 1];

            if (event.shiftKey) {
                if (
                    document.activeElement === primeiro ||
                    !modal.contains(document.activeElement)
                ) {
                    event.preventDefault();
                    ultimo.focus();
                }
            } else if (
                document.activeElement === ultimo ||
                !modal.contains(document.activeElement)
            ) {
                event.preventDefault();
                primeiro.focus();
            }
        }

        document.addEventListener("keydown", aoTeclar);

        return () => {
            document.removeEventListener("keydown", aoTeclar);

            if (focoAnterior?.isConnected) {
                // O retorno ao botão de origem é controlado
                // pelas funções de fechamento.
            }
        };
    }, [hobbyParaDesativar]);

    function abrirModal() {
        botaoOrigemRef.current = botaoNovoRef.current;
        setHobbyEmEdicao(null);
        setForm({ ...FORM_INICIAL });
        setErroCadastro("");
        setSucesso("");
        setModalAberto(true);
    }

    function abrirEdicao(hobby, elementoOrigem) {
        botaoOrigemRef.current = elementoOrigem;
        setHobbyEmEdicao(hobby);
        setForm({
            nome: hobby.nome ?? "",
            descricao: hobby.descricao ?? "",
            imagemUrl: hobby.imagemUrl ?? "",
            categoryId: hobby.category?.id != null ? String(hobby.category.id) : "",
            custoEstimado: hobby.custoEstimado != null ? String(hobby.custoEstimado) : "",
            custoRecorrenteEstimado: hobby.custoRecorrenteEstimado != null ? String(hobby.custoRecorrenteEstimado) : "",
            nivelDificuldade: hobby.nivelDificuldade != null ? String(hobby.nivelDificuldade) : "",
            tempoNecessario: hobby.tempoNecessario != null ? String(hobby.tempoNecessario) : "",
            tipoSocializacao: hobby.tipoSocializacao ?? "",
            nivelAtividadeFisica: hobby.nivelAtividadeFisica ?? "",
            ambiente: hobby.ambiente ?? "",
            requerEquipamento: Boolean(hobby.requerEquipamento)
        });
        setErroCadastro("");
        setSucesso("");
        setModalAberto(true);
    }

    function fecharModal() {
        if (salvandoRef.current) return;
        setModalAberto(false);
        requestAnimationFrame(() => botaoOrigemRef.current?.focus());
    }

    function alterar(event) {
        const { name, value, type, checked } = event.target;
        setForm((atual) => ({ ...atual, [name]: type === "checkbox" ? checked : value }));
    }

    async function salvarHobby(event) {
        event.preventDefault();
        if (salvandoRef.current) return;
        setErroCadastro("");

        const custo = Number(form.custoEstimado);
        const recorrente = form.custoRecorrenteEstimado.trim() === ""
            ? null : Number(form.custoRecorrenteEstimado);
        const tempo = Number(form.tempoNecessario);
        const dificuldade = Number(form.nivelDificuldade);

        if (!form.nome.trim() || !form.descricao.trim() || !form.categoryId ||
            form.custoEstimado.trim() === "" || !Number.isFinite(custo) || custo < 0 ||
            (recorrente !== null && (!Number.isFinite(recorrente) || recorrente < 0)) ||
            form.tempoNecessario.trim() === "" || !Number.isFinite(tempo) || tempo <= 0 ||
            !Number.isInteger(dificuldade) || dificuldade < 1 || dificuldade > 5 ||
            !form.tipoSocializacao || !form.nivelAtividadeFisica || !form.ambiente ||
            form.nome.trim().length > 150 || form.imagemUrl.length > 2000) {
            setErroCadastro("Confira os campos obrigatórios e os valores informados.");
            return;
        }

        const payload = {
            nome: form.nome.trim(), descricao: form.descricao.trim(),
            imagemUrl: form.imagemUrl.trim() || null,
            categoryId: Number(form.categoryId),
            custoEstimado: custo, custoRecorrenteEstimado: recorrente,
            nivelDificuldade: dificuldade, tempoNecessario: tempo,
            tipoSocializacao: form.tipoSocializacao,
            nivelAtividadeFisica: form.nivelAtividadeFisica,
            ambiente: form.ambiente, requerEquipamento: form.requerEquipamento
        };

        salvandoRef.current = true;
        setSalvando(true);
        try {
            if (hobbyEmEdicao) {
                await api.put(`/hobbies/${hobbyEmEdicao.id}`, payload, { headers: headers() });
            } else {
                await api.post("/hobbies", payload, { headers: headers() });
            }
            setModalAberto(false);
            if (!hobbyEmEdicao) setBusca("");
            setSucesso(hobbyEmEdicao ? "Hobby atualizado com sucesso!" : "Hobby cadastrado com sucesso!");
            await carregarHobbies();
            requestAnimationFrame(() => botaoOrigemRef.current?.focus());
        } catch (error) {
            console.error("Erro ao salvar hobby:", error);
            setErroCadastro(error.response?.data?.message || (hobbyEmEdicao ? "Não foi possível atualizar o hobby." : "Não foi possível cadastrar o hobby."));
        } finally {
            salvandoRef.current = false;
            setSalvando(false);
        }
    }

    function abrirConfirmacao(hobby, botaoOrigem) {
        botaoDesativarOrigemRef.current = botaoOrigem;
        setErroDesativacao("");
        setHobbyParaDesativar(hobby);
    }

    function fecharConfirmacao() {
        if (processandoDesativacaoRef.current) return;

        setHobbyParaDesativar(null);
        setErroDesativacao("");

        requestAnimationFrame(() => {
            botaoDesativarOrigemRef.current?.focus();
        });
    }

    async function alternarStatus(hobby) {
        if (alterandoStatusId !== null || processandoDesativacaoRef.current) {
            return;
        }

        processandoDesativacaoRef.current = true;
        setAlterandoStatusId(hobby.id);
        setErro("");
        setErroDesativacao("");
        setSucesso("");

        try {
            if (hobby.ativo) {
                await api.delete(`/hobbies/${hobby.id}`, {
                    headers: headers()
                });
            } else {
                await api.patch(
                    `/hobbies/${hobby.id}/reativar`,
                    {},
                    { headers: headers() }
                );
            }

            await carregarHobbies();

            setSucesso(
                hobby.ativo
                    ? `Hobby "${hobby.nome}" desativado com sucesso!`
                    : `Hobby "${hobby.nome}" reativado com sucesso!`
            );

            if (hobby.ativo) {
                setHobbyParaDesativar(null);

                requestAnimationFrame(() => {
                    botaoDesativarOrigemRef.current?.focus();
                });
            }
        } catch (error) {
            console.error("Erro ao alterar status do hobby:", error);

            const mensagem =
                error.response?.data?.message ||
                "Não foi possível alterar o status do hobby.";

            if (hobby.ativo) {
                setErroDesativacao(mensagem);
            } else {
                setErro(mensagem);
            }
        } finally {
            processandoDesativacaoRef.current = false;
            setAlterandoStatusId(null);
        }
    }

    const totalHobbies = hobbies.length;
    const totalAtivos = hobbies.filter((hobby) => hobby.ativo).length;
    const totalInativos = totalHobbies - totalAtivos;

    const hobbiesFiltrados = hobbies
        .filter((hobby) => {
            const correspondeNome = hobby.nome
                ?.toLowerCase()
                .includes(busca.trim().toLowerCase());

            const correspondeStatus =
                filtroStatus === "TODOS" ||
                (filtroStatus === "ATIVOS" && hobby.ativo) ||
                (filtroStatus === "INATIVOS" && !hobby.ativo);

            return correspondeNome && correspondeStatus;
        })
        .sort((a, b) => {
            switch (ordenacao) {
                case "NOME_ASC":
                    return (a.nome ?? "").localeCompare(
                        b.nome ?? "",
                        "pt-BR",
                        { sensitivity: "base" }
                    );

                case "NOME_DESC":
                    return (b.nome ?? "").localeCompare(
                        a.nome ?? "",
                        "pt-BR",
                        { sensitivity: "base" }
                    );

                case "ANTIGOS":
                    return a.id - b.id;

                case "RECENTES":
                default:
                    return b.id - a.id;
            }
        });

    function formatarMoeda(valor) {
        if (valor == null) return "Não informado";
        return Number(valor).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
    }

    return (
        <div className="admin-hobbies">
            <div className="admin-page-heading">
                <h1>Catálogo de hobbies</h1>
                <p>Consulte e gerencie os hobbies cadastrados no Recommendi.a.</p>
            </div>

            <div className="admin-hobbies-toolbar">
                <input type="search" placeholder="Buscar hobby pelo nome..."
                       value={busca} onChange={(event) => setBusca(event.target.value)}
                       aria-label="Buscar hobby pelo nome" />
                <button type="button" className="admin-refresh-button"
                        onClick={carregarHobbies} disabled={carregando}>Atualizar lista</button>
                <button type="button" className="admin-hobby-new-button"
                        ref={botaoNovoRef} onClick={abrirModal}>+ Novo hobby</button>
            </div>

            <div className="admin-hobbies-filters" role="group" aria-label="Filtrar hobbies por status">
                {[
                    { valor: "TODOS", rotulo: "Todos", total: totalHobbies },
                    { valor: "ATIVOS", rotulo: "Ativos", total: totalAtivos },
                    { valor: "INATIVOS", rotulo: "Inativos", total: totalInativos }
                ].map((opcao) => (
                    <button
                        key={opcao.valor}
                        type="button"
                        className={`admin-hobbies-filter-button ${
                            filtroStatus === opcao.valor ? "active" : ""
                        }`}
                        onClick={() => setFiltroStatus(opcao.valor)}
                        aria-pressed={filtroStatus === opcao.valor}
                    >
                        {opcao.rotulo} ({opcao.total})
                    </button>
                ))}
            </div>

            <div className="admin-hobbies-sort">
                <label htmlFor="ordenacao-hobbies">
                    Ordenar por:
                </label>

                <select
                    id="ordenacao-hobbies"
                    value={ordenacao}
                    onChange={(event) => setOrdenacao(event.target.value)}
                >
                    <option value="RECENTES">Mais recentes</option>
                    <option value="ANTIGOS">Mais antigos</option>
                    <option value="NOME_ASC">Nome (A–Z)</option>
                    <option value="NOME_DESC">Nome (Z–A)</option>
                </select>
            </div>

            {sucesso && <p className="admin-hobby-success" role="status">{sucesso}</p>}
            {!carregando && !erro && <p className="admin-hobbies-count">{hobbiesFiltrados.length} hobby(s) encontrado(s)</p>}
            {carregando && <p className="admin-loading">Carregando hobbies...</p>}
            {erro && <p className="admin-error" role="alert">{erro}</p>}
            {!carregando && !erro && hobbiesFiltrados.length === 0 && (
                <div className="admin-empty-state">
                    <h2>Nenhum hobby encontrado</h2>
                    <p>{busca ? "Tente pesquisar por outro nome." : "Ainda não existem hobbies cadastrados."}</p>
                </div>
            )}
            {!carregando && !erro && hobbiesFiltrados.length > 0 && (
                <div className="admin-hobbies-list">
                    {hobbiesFiltrados.map((hobby) => (
                        <article className="admin-hobby-card" key={hobby.id}>
                            <div className="admin-suggestion-header">
                                <div>
                                    <h2>{hobby.nome}</h2>
                                    <span className="admin-suggestion-category">{hobby.category?.nome || "Sem categoria"}</span>
                                    <span
                                        className={`admin-hobby-status ${
                                            hobby.ativo
                                                ? "admin-hobby-status-active"
                                                : "admin-hobby-status-inactive"
                                        }`}
                                    >
                                        {hobby.ativo ? "Ativo" : "Inativo"}
                                    </span>
                                </div>
                                <div className="admin-hobby-card-controls">
                                    <span className="admin-hobby-id">#{hobby.id}</span>

                                    <button
                                        type="button"
                                        className="admin-hobby-edit-button"
                                        onClick={(event) =>
                                            abrirEdicao(hobby, event.currentTarget)
                                        }
                                        disabled={alterandoStatusId !== null}
                                    >
                                        Editar
                                    </button>

                                    <button
                                        type="button"
                                        className={
                                            hobby.ativo
                                                ? "admin-hobby-deactivate-button"
                                                : "admin-hobby-reactivate-button"
                                        }
                                        onClick={(event) => {
                                            if (hobby.ativo) {
                                                abrirConfirmacao(hobby, event.currentTarget);
                                            } else {
                                                alternarStatus(hobby);
                                            }
                                        }}
                                        disabled={alterandoStatusId !== null}
                                    >
                                        {alterandoStatusId === hobby.id
                                            ? "Processando..."
                                            : hobby.ativo
                                                ? "Desativar"
                                                : "Reativar"}
                                    </button>
                                </div>
                            </div>
                            <p className="admin-suggestion-description">{hobby.descricao}</p>
                            <div className="admin-suggestion-details">
                                <div><small>Custo inicial</small><strong>{formatarMoeda(hobby.custoEstimado)}</strong></div>
                                <div><small>Custo recorrente</small><strong>{formatarMoeda(hobby.custoRecorrenteEstimado)}</strong></div>
                                <div><small>Dificuldade</small><strong>{hobby.nivelDificuldade ?? "—"}</strong></div>
                                <div><small>Tempo necessário</small><strong>{hobby.tempoNecessario != null ? `${hobby.tempoNecessario} h` : "Não informado"}</strong></div>
                            </div>
                        </article>
                    ))}
                </div>
            )}

            {hobbyParaDesativar && (
                <div className="admin-hobby-confirm-overlay">
                    <div
                        ref={modalConfirmacaoRef}
                        className="admin-hobby-confirm-modal"
                        role="alertdialog"
                        aria-modal="true"
                        aria-labelledby="titulo-confirmacao-hobby"
                        aria-describedby="descricao-confirmacao-hobby"
                        tabIndex={-1}
                    >
                        <div className="admin-hobby-confirm-icon">
                            !
                        </div>

                        <h2 id="titulo-confirmacao-hobby">
                            Desativar hobby?
                        </h2>

                        <p id="descricao-confirmacao-hobby">
                            Você está prestes a desativar o hobby{" "}
                            <strong>{hobbyParaDesativar.nome}</strong>.
                        </p>

                        <p>
                            Ele deixará de aparecer nas novas recomendações,
                            mas os históricos e relacionamentos dos usuários
                            serão preservados.
                        </p>

                        {erroDesativacao && (
                            <p className="admin-error" role="alert">
                                {erroDesativacao}
                            </p>
                        )}

                        <div className="admin-hobby-confirm-actions">
                            <button
                                ref={botaoCancelarRef}
                                type="button"
                                className="admin-hobby-confirm-cancel"
                                disabled={alterandoStatusId !== null}
                                onClick={fecharConfirmacao}
                            >
                                Cancelar
                            </button>

                            <button
                                type="button"
                                className="admin-hobby-confirm-submit"
                                disabled={alterandoStatusId !== null}
                                onClick={() => alternarStatus(hobbyParaDesativar)}
                            >
                                {alterandoStatusId !== null
                                    ? "Desativando..."
                                    : "Confirmar desativação"}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {modalAberto && (
                <div className="admin-hobby-overlay">
                    <div className="admin-hobby-modal" ref={modalRef} role="dialog" aria-modal="true" aria-labelledby="titulo-form-hobby">
                        <div className="admin-hobby-modal-heading">
                            <h2 id="titulo-form-hobby">{hobbyEmEdicao ? "Editar hobby" : "Cadastrar novo hobby"}</h2>
                            <button type="button" onClick={fecharModal} disabled={salvando} aria-label="Fechar formulário">×</button>
                        </div>
                        <form onSubmit={salvarHobby} className="admin-hobby-form">
                            <label>Nome *<input name="nome" value={form.nome} onChange={alterar} required maxLength={150} /></label>
                            <label>Descrição *<textarea name="descricao" value={form.descricao} onChange={alterar} required rows={4} /></label>
                            <label>URL da imagem<input name="imagemUrl" value={form.imagemUrl} onChange={alterar} maxLength={2000} /></label>
                            <label>Categoria *
                                <select name="categoryId" value={form.categoryId} onChange={alterar} required disabled={carregandoCategorias || !!erroCategorias}>
                                    <option value="">Selecione uma categoria</option>
                                    {categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nome}</option>)}
                                </select>
                            </label>
                            {carregandoCategorias && <p>Carregando categorias...</p>}
                            {erroCategorias && <p role="alert" className="admin-error">{erroCategorias}</p>}
                            <div className="admin-hobby-form-grid">
                                <label>Custo inicial (R$) *<input type="number" name="custoEstimado" value={form.custoEstimado} onChange={alterar} min="0" step="any" required /></label>
                                <label>Custo recorrente (R$)<input type="number" name="custoRecorrenteEstimado" value={form.custoRecorrenteEstimado} onChange={alterar} min="0" step="any" /></label>
                                <label>Dificuldade (1 a 5) *<select name="nivelDificuldade" value={form.nivelDificuldade} onChange={alterar} required>
                                    <option value="">Selecione</option>
                                    {[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n}</option>)}
                                </select></label>
                                <label>Tempo necessário (horas) *<input type="number" name="tempoNecessario" value={form.tempoNecessario} onChange={alterar} min="0.01" step="any" required /></label>
                                {Object.entries(OPCOES).map(([campo, opcoes]) => (
                                    <label key={campo}>
                                        {campo === "tipoSocializacao" ? "Socialização" : campo === "nivelAtividadeFisica" ? "Atividade física" : "Ambiente"} *
                                        <select name={campo} value={form[campo]} onChange={alterar} required>
                                            <option value="">Selecione</option>
                                            {opcoes.map((opcao) => <option key={opcao} value={opcao}>{rotulo(opcao)}</option>)}
                                        </select>
                                    </label>
                                ))}
                            </div>
                            <label className="admin-hobby-checkbox"><input type="checkbox" name="requerEquipamento" checked={form.requerEquipamento} onChange={alterar} /> Requer equipamento</label>
                            {erroCadastro && <p className="admin-error" role="alert">{erroCadastro}</p>}
                            <div className="admin-hobby-actions">
                                <button type="button" onClick={fecharModal} disabled={salvando}>Cancelar</button>
                                <button type="submit" className="admin-hobby-new-button" disabled={salvando || carregandoCategorias || !!erroCategorias}>
                                    {salvando ? "Salvando..." : hobbyEmEdicao ? "Salvar alterações" : "Cadastrar hobby"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
