import { useEffect, useState } from "react";
import { api } from "../api/api";
import catalogoOSM from "../data/osmCatalogoAmpliadoPtBr.json";

const headers = () => ({
    Authorization: `Bearer ${localStorage.getItem("token")}`
});

const rotulo = (valor) =>
    valor?.replaceAll("_", " ").toLowerCase()
        .replace(/^./, (letra) => letra.toUpperCase()) || "—";

const TAMANHO_PAGINA_CATALOGO = 25;
const normalizarBusca = (valor) => String(valor ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("pt-BR");

const condicaoVazia = () => ({ osmKey: "", osmValue: "", matchType: "IGUAL" });

const formularioInicial = () => ({
    relevancia: "ALTA",
    ativo: true,
    conditions: [condicaoVazia()]
});

function mensagemErro(error, alternativa) {
    const dados = error.response?.data;
    return dados?.message || dados?.detail || dados?.error || alternativa;
}

export default function AdminLocationRules() {
    const [hobbies, setHobbies] = useState([]);
    const [hobbyId, setHobbyId] = useState("");
    const [regras, setRegras] = useState([]);

    const [carregandoHobbies, setCarregandoHobbies] = useState(true);
    const [carregandoRegras, setCarregandoRegras] = useState(false);
    const [erroHobbies, setErroHobbies] = useState("");
    const [erroRegras, setErroRegras] = useState("");

    const [formularioAberto, setFormularioAberto] = useState(false);
    const [formulario, setFormulario] = useState(formularioInicial);
    const [salvando, setSalvando] = useState(false);
    const [erroCadastro, setErroCadastro] = useState("");
    const [sucesso, setSucesso] = useState("");
    const [versaoListagem, setVersaoListagem] = useState(0);
    const [regraEmEdicao, setRegraEmEdicao] = useState(null);
    const [operacaoId, setOperacaoId] = useState(null);
    const [erroOperacao, setErroOperacao] = useState("");
    const [buscaCatalogo, setBuscaCatalogo] = useState("");
    const [categoriaCatalogo, setCategoriaCatalogo] = useState("Todas");
    const [catalogoAberto, setCatalogoAberto] = useState(true);
    const [paginaCatalogo, setPaginaCatalogo] = useState(1);

    useEffect(() => {
        let ativo = true;

        async function carregarHobbies() {
            try {
                const response = await api.get("/hobbies", {
                    headers: headers()
                });

                if (ativo) setHobbies(response.data);
            } catch (error) {
                if (ativo) {
                    setErroHobbies(mensagemErro(error, "Não foi possível carregar os hobbies."));
                }
            } finally {
                if (ativo) setCarregandoHobbies(false);
            }
        }

        carregarHobbies();
        return () => { ativo = false; };
    }, []);

    useEffect(() => {
        if (!hobbyId) {
            setRegras([]);
            setErroRegras("");
            setCarregandoRegras(false);
            return;
        }

        let ativo = true;

        async function carregarRegras() {
            setCarregandoRegras(true);
            setErroRegras("");
            setRegras([]);

            try {
                const response = await api.get(
                    `/admin/hobbies/${hobbyId}/location-rules`,
                    { headers: headers() }
                );

                if (ativo) setRegras(response.data);
            } catch (error) {
                if (ativo) {
                    setErroRegras(mensagemErro(error, "Não foi possível carregar as regras."));
                }
            } finally {
                if (ativo) setCarregandoRegras(false);
            }
        }

        carregarRegras();
        return () => { ativo = false; };
    }, [hobbyId, versaoListagem]);

    function selecionarHobby(novoHobbyId) {
        setHobbyId(novoHobbyId);
        setFormularioAberto(false);
        setFormulario(formularioInicial());
        setErroCadastro("");
        setSucesso("");
        setRegraEmEdicao(null);
        setErroOperacao("");
    }

    function abrirCadastro() {
        setRegraEmEdicao(null);
        setFormulario(formularioInicial());
        setErroCadastro("");
        setErroOperacao("");
        setSucesso("");
        setFormularioAberto(true);
    }

    function abrirEdicao(regra) {
        setRegraEmEdicao(regra.id);
        setFormulario({
            relevancia: regra.relevancia,
            ativo: regra.ativo,
            conditions: regra.conditions.map((condicao) => ({
                osmKey: condicao.osmKey,
                osmValue: condicao.osmValue ?? "",
                matchType: condicao.matchType
            }))
        });
        setErroCadastro("");
        setErroOperacao("");
        setSucesso("");
        setFormularioAberto(true);
    }

    async function alterarStatus(regra) {
        if (operacaoId !== null || salvando) return;
        setOperacaoId(regra.id);
        setErroOperacao("");
        setSucesso("");
        try {
            await api.patch(
                `/admin/hobbies/${hobbyId}/location-rules/${regra.id}/status`,
                { ativo: !regra.ativo },
                { headers: headers() }
            );
            setSucesso(`Regra #${regra.id} ${regra.ativo ? "desativada" : "ativada"} com sucesso.`);
            setVersaoListagem((versao) => versao + 1);
        } catch (error) {
            setErroOperacao(mensagemErro(error, "Não foi possível alterar o status da regra."));
        } finally {
            setOperacaoId(null);
        }
    }

    async function excluirRegra(regra) {
        if (operacaoId !== null || salvando) return;
        if (!window.confirm(`Excluir definitivamente a regra #${regra.id}?`)) return;
        setOperacaoId(regra.id);
        setErroOperacao("");
        setSucesso("");
        try {
            await api.delete(
                `/admin/hobbies/${hobbyId}/location-rules/${regra.id}`,
                { headers: headers() }
            );
            if (regraEmEdicao === regra.id) {
                setRegraEmEdicao(null);
                setFormularioAberto(false);
                setFormulario(formularioInicial());
            }
            setSucesso(`Regra #${regra.id} excluída com sucesso.`);
            setVersaoListagem((versao) => versao + 1);
        } catch (error) {
            setErroOperacao(mensagemErro(error, "Não foi possível excluir a regra."));
        } finally {
            setOperacaoId(null);
        }
    }

    function alterarCondicao(indice, campo, valor) {
        setFormulario((atual) => ({
            ...atual,
            conditions: atual.conditions.map((condicao, i) => {
                if (i !== indice) return condicao;
                if (campo === "matchType") {
                    return {
                        ...condicao,
                        matchType: valor,
                        osmValue: valor === "EXISTE" ? "" : condicao.osmValue
                    };
                }
                return { ...condicao, [campo]: valor };
            })
        }));
    }

    const categoriasCatalogo = ["Todas", ...Array.from(new Set(catalogoOSM.map((item) => item.categoria))).sort((a, b) => a.localeCompare(b, "pt-BR"))];
    const resultadosCatalogo = catalogoOSM.filter((item) => {
        if (categoriaCatalogo !== "Todas" && item.categoria !== categoriaCatalogo) return false;
        const termo = normalizarBusca(buscaCatalogo.trim());
        if (!termo) return true;
        const texto = [item.nome, item.categoria, item.osmKey, item.osmValue,
            ...(item.termosBusca || [])].join(" ");
        return normalizarBusca(texto).includes(termo);
    });
    const totalPaginasCatalogo = Math.max(1, Math.ceil(resultadosCatalogo.length / TAMANHO_PAGINA_CATALOGO));
    const paginaAtualCatalogo = Math.min(paginaCatalogo, totalPaginasCatalogo);
    const resultadosPaginaCatalogo = resultadosCatalogo.slice(
        (paginaAtualCatalogo - 1) * TAMANHO_PAGINA_CATALOGO,
        paginaAtualCatalogo * TAMANHO_PAGINA_CATALOGO
    );

    function adicionarDoCatalogo(item) {
        const nova = { osmKey: item.osmKey, osmValue: item.osmValue, matchType: item.matchType };
        setErroCadastro("");
        setFormulario((atual) => {
            const jaExiste = atual.conditions.some((condicao) =>
                condicao.osmKey === nova.osmKey && condicao.osmValue === nova.osmValue && condicao.matchType === nova.matchType
            );
            if (jaExiste) return atual;
            const somenteVazia = atual.conditions.length === 1 &&
                !atual.conditions[0].osmKey.trim() && !atual.conditions[0].osmValue.trim();
            return { ...atual, conditions: somenteVazia ? [nova] : [...atual.conditions, nova] };
        });
    }

    function adicionarCondicao() {
        setFormulario((atual) => ({
            ...atual,
            conditions: [...atual.conditions, condicaoVazia()]
        }));
    }

    function removerCondicao(indice) {
        setFormulario((atual) => ({
            ...atual,
            conditions: atual.conditions.filter((_, i) => i !== indice)
        }));
    }

    async function salvarRegra(event) {
        event.preventDefault();
        if (!hobbyId || salvando) return;

        setErroCadastro("");
        setSucesso("");

        const conditions = formulario.conditions.map((condicao) => ({
            osmKey: condicao.osmKey.trim(),
            osmValue: condicao.matchType === "EXISTE" ? null : condicao.osmValue.trim(),
            matchType: condicao.matchType
        }));

        if (conditions.length === 0) {
            setErroCadastro("Adicione pelo menos uma condição OSM.");
            return;
        }

        const tokenOSM = /^[a-zA-Z0-9_:.-]+$/;
        if (conditions.some((condicao) =>
            !tokenOSM.test(condicao.osmKey) ||
            (condicao.matchType === "IGUAL" && !tokenOSM.test(condicao.osmValue))
        )) {
            setErroCadastro("Informe chaves e valores OSM válidos, sem espaços.");
            return;
        }

        const assinaturas = conditions.map((condicao) =>
            `${condicao.matchType}|${condicao.osmKey}|${condicao.osmValue ?? ""}`
        );
        if (new Set(assinaturas).size !== assinaturas.length) {
            setErroCadastro("Existem condições OSM duplicadas neste formulário.");
            return;
        }

        const hobbyDoEnvio = hobbyId;
        setSalvando(true);
        try {
            const url = `/admin/hobbies/${hobbyDoEnvio}/location-rules`;
            const dados = { relevancia: formulario.relevancia, ativo: formulario.ativo, conditions };
            if (regraEmEdicao !== null) {
                await api.put(`${url}/${regraEmEdicao}`, dados, { headers: headers() });
            } else {
                await api.post(url, dados, { headers: headers() });
            }

            // Fecha o formulário e atualiza as regras após a gravação.
            setFormularioAberto(false);
            setFormulario(formularioInicial());
            setSucesso(regraEmEdicao !== null
                ? "Regra geográfica atualizada com sucesso."
                : "Regra geográfica cadastrada com sucesso.");
            setRegraEmEdicao(null);
            setVersaoListagem((versao) => versao + 1);
        } catch (error) {
            setErroCadastro(mensagemErro(error, "Não foi possível salvar a regra."));
        } finally {
            setSalvando(false);
        }
    }

    return (
        <div className="admin-location-rules">
            <div className="admin-page-heading">
                <h1>Regras geográficas</h1>
                <p>Consulte e cadastre as configurações OSM utilizadas na busca de locais para cada hobby.</p>
            </div>

            <div className="admin-card">
                <h2>Selecionar hobby</h2>
                <label htmlFor="hobby-location-select">Hobby</label>
                <select
                    id="hobby-location-select"
                    value={hobbyId}
                    disabled={carregandoHobbies || salvando}
                    onChange={(e) => selecionarHobby(e.target.value)}
                >
                    <option value="">Selecione um hobby</option>
                    {hobbies.map((hobby) => (
                        <option key={hobby.id} value={hobby.id}>{hobby.nome}</option>
                    ))}
                </select>
                {carregandoHobbies && <p>Carregando hobbies...</p>}
                {erroHobbies && <p role="alert">{erroHobbies}</p>}
            </div>

            {hobbyId && (
                <>
                    <section className="admin-card">
                        <h2>{regraEmEdicao !== null ? `Editar regra #${regraEmEdicao}` : "Cadastrar regra geográfica"}</h2>
                        {!formularioAberto ? (
                            <button type="button" disabled={operacaoId !== null} onClick={abrirCadastro}>
                                Nova regra
                            </button>
                        ) : (
                            <form onSubmit={salvarRegra}>
                                <div>
                                    <label htmlFor="regra-relevancia">Relevância</label>
                                    <select
                                        id="regra-relevancia"
                                        value={formulario.relevancia}
                                        disabled={salvando}
                                        onChange={(e) => setFormulario((atual) => ({
                                            ...atual, relevancia: e.target.value
                                        }))}
                                    >
                                        <option value="ALTA">Alta</option>
                                        <option value="MEDIA">Média</option>
                                        <option value="COMPLEMENTAR">Complementar</option>
                                    </select>
                                </div>

                                <div>
                                    <label htmlFor="regra-ativo">
                                        <input
                                            id="regra-ativo"
                                            type="checkbox"
                                            checked={formulario.ativo}
                                            disabled={salvando}
                                            onChange={(e) => setFormulario((atual) => ({
                                                ...atual, ativo: e.target.checked
                                            }))}
                                        />
                                        Regra ativa
                                    </label>
                                </div>

                                <section className="osm-catalogo" aria-label="Biblioteca de tags OSM">
                                    <h3>Biblioteca de locais em português</h3>
                                    <p>Encontre tags conhecidas e adicione-as às condições da regra. Cada item corresponde a uma condição técnica.</p>
                                    <button type="button" disabled={salvando} onClick={() => setCatalogoAberto((atual) => !atual)}>
                                        {catalogoAberto ? "Ocultar biblioteca" : "Mostrar biblioteca"}
                                    </button>
                                    {catalogoAberto && (
                                        <div>
                                            <label htmlFor="osm-catalogo-busca">Pesquisar por nome, sinônimo ou tag</label>
                                            <input id="osm-catalogo-busca" type="search" value={buscaCatalogo}
                                                   disabled={salvando} placeholder="Ex.: praça, natação, leisure=park..."
                                                   onChange={(e) => { setBuscaCatalogo(e.target.value); setPaginaCatalogo(1); }} />
                                            <label htmlFor="osm-catalogo-categoria">Categoria</label>
                                            <select id="osm-catalogo-categoria" value={categoriaCatalogo}
                                                    disabled={salvando} onChange={(e) => { setCategoriaCatalogo(e.target.value); setPaginaCatalogo(1); }}>
                                                {categoriasCatalogo.map((categoria) => (
                                                    <option key={categoria} value={categoria}>{categoria}</option>
                                                ))}
                                            </select>
                                            <p>{resultadosCatalogo.length} de {catalogoOSM.length} opções encontradas. Página {paginaAtualCatalogo} de {totalPaginasCatalogo}.</p>
                                            <div className="osm-catalogo-resultados" style={{ maxHeight: 300, overflowY: "auto" }}>
                                                {resultadosPaginaCatalogo.map((item) => {
                                                    const adicionado = formulario.conditions.some((condicao) =>
                                                        condicao.osmKey === item.osmKey && condicao.osmValue === item.osmValue && condicao.matchType === item.matchType
                                                    );
                                                    return (
                                                        <div key={item.id} className="osm-catalogo-item" style={{ padding: "0.65rem 0", borderBottom: "1px solid #8884" }}>
                                                            <strong>{item.nome}</strong> <small>({item.categoria})</small>
                                                            <div><code>{item.matchType === "EXISTE" ? `${item.osmKey} (chave existente)` : `${item.osmKey}=${item.osmValue}`}</code></div>
                                                            <button type="button" disabled={salvando || adicionado}
                                                                    onClick={() => adicionarDoCatalogo(item)}>
                                                                {adicionado ? "Adicionada" : "Adicionar condição"}
                                                            </button>
                                                        </div>
                                                    );
                                                })}
                                                {resultadosCatalogo.length === 0 && <p>Nenhuma opção encontrada. Use os campos técnicos abaixo.</p>}
                                            </div>
                                            {totalPaginasCatalogo > 1 && (
                                                <div className="osm-catalogo-paginacao" style={{ display: "flex", gap: "0.75rem", alignItems: "center", marginTop: "0.75rem" }}>
                                                    <button type="button" disabled={salvando || paginaAtualCatalogo === 1}
                                                            onClick={() => setPaginaCatalogo((atual) => Math.max(1, atual - 1))}>Anterior</button>
                                                    <span>Página {paginaAtualCatalogo} de {totalPaginasCatalogo}</span>
                                                    <button type="button" disabled={salvando || paginaAtualCatalogo === totalPaginasCatalogo}
                                                            onClick={() => setPaginaCatalogo((atual) => Math.min(totalPaginasCatalogo, atual + 1))}>Próxima</button>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                    <p><small>Atenção: condições na mesma regra são combinadas conforme a lógica do backend. Para locais alternativos, cadastre regras separadas.</small></p>
                                </section>

                                <h3>Condições OSM (edição técnica)</h3>
                                {formulario.conditions.map((condicao, indice) => (
                                    <fieldset key={indice} disabled={salvando}>
                                        <legend>Condição {indice + 1}</legend>
                                        <div>
                                            <label htmlFor={`osm-key-${indice}`}>Chave OSM</label>
                                            <input
                                                id={`osm-key-${indice}`}
                                                value={condicao.osmKey}
                                                onChange={(e) => alterarCondicao(indice, "osmKey", e.target.value)}
                                                placeholder="Ex.: leisure"
                                                required
                                                maxLength={100}
                                            />
                                        </div>
                                        <div>
                                            <label htmlFor={`osm-match-${indice}`}>Comparação</label>
                                            <select
                                                id={`osm-match-${indice}`}
                                                value={condicao.matchType}
                                                onChange={(e) => alterarCondicao(indice, "matchType", e.target.value)}
                                            >
                                                <option value="IGUAL">Igual</option>
                                                <option value="EXISTE">Existe</option>
                                            </select>
                                        </div>
                                        {condicao.matchType === "IGUAL" && (
                                            <div>
                                                <label htmlFor={`osm-value-${indice}`}>Valor OSM</label>
                                                <input
                                                    id={`osm-value-${indice}`}
                                                    value={condicao.osmValue}
                                                    onChange={(e) => alterarCondicao(indice, "osmValue", e.target.value)}
                                                    placeholder="Ex.: park"
                                                    required
                                                    maxLength={150}
                                                />
                                            </div>
                                        )}
                                        <button
                                            type="button"
                                            disabled={formulario.conditions.length === 1}
                                            onClick={() => removerCondicao(indice)}
                                        >
                                            Remover condição
                                        </button>
                                    </fieldset>
                                ))}
                                <button type="button" disabled={salvando} onClick={adicionarCondicao}>
                                    Adicionar condição
                                </button>

                                {erroCadastro && <p role="alert">{erroCadastro}</p>}
                                <div>
                                    <button type="submit" disabled={salvando}>
                                        {salvando ? "Salvando..." : regraEmEdicao !== null ? "Salvar alterações" : "Salvar regra"}
                                    </button>
                                    <button
                                        type="button"
                                        disabled={salvando}
                                        onClick={() => {
                                            setFormularioAberto(false);
                                            setFormulario(formularioInicial());
                                            setRegraEmEdicao(null);
                                            setErroCadastro("");
                                        }}
                                    >
                                        Cancelar
                                    </button>
                                </div>
                            </form>
                        )}
                        {sucesso && <p role="status">{sucesso}</p>}
                        {erroOperacao && <p role="alert">{erroOperacao}</p>}
                    </section>

                    <section className="admin-location-rules-list">
                        <h2>Regras configuradas</h2>
                        {carregandoRegras && <p>Carregando regras...</p>}
                        {erroRegras && <p role="alert">{erroRegras}</p>}
                        {!carregandoRegras && !erroRegras && regras.length === 0 && (
                            <p>Este hobby ainda não possui regras geográficas cadastradas.</p>
                        )}
                        {!carregandoRegras && !erroRegras && regras.map((regra) => (
                            <article key={regra.id} className="admin-card">
                                <h3>Regra #{regra.id}</h3>
                                <p><strong>Relevância: </strong>{rotulo(regra.relevancia)}</p>
                                <p><strong>Status: </strong>{regra.ativo ? "Ativa" : "Inativa"}</p>
                                <h4>Condições OSM</h4>
                                <ul>
                                    {regra.conditions.map((condicao) => (
                                        <li key={condicao.id}>
                                            <code>{condicao.osmKey}</code>
                                            {" — "}
                                            {condicao.matchType === "EXISTE"
                                                ? "Existe"
                                                : `Igual a ${condicao.osmValue}`}
                                        </li>
                                    ))}
                                </ul>
                                <div className="admin-rule-actions">
                                    <button
                                        type="button"
                                        disabled={salvando || operacaoId !== null}
                                        onClick={() => abrirEdicao(regra)}
                                    >
                                        Editar
                                    </button>
                                    <button
                                        type="button"
                                        disabled={salvando || operacaoId !== null}
                                        onClick={() => alterarStatus(regra)}
                                    >
                                        {operacaoId === regra.id ? "Processando..." : regra.ativo ? "Desativar" : "Ativar"}
                                    </button>
                                    <button
                                        type="button"
                                        disabled={salvando || operacaoId !== null}
                                        onClick={() => excluirRegra(regra)}
                                    >
                                        Excluir
                                    </button>
                                </div>
                            </article>
                        ))}
                    </section>
                </>
            )}
        </div>
    );
}
