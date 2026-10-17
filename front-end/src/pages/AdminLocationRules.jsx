import { useEffect, useState } from "react";
import { api } from "../api/api";

const headers = () => ({
    Authorization: `Bearer ${localStorage.getItem("token")}`
});

const rotulo = (valor) =>
    valor?.replaceAll("_", " ").toLowerCase()
        .replace(/^./, (letra) => letra.toUpperCase()) || "—";

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

    async function cadastrarRegra(event) {
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
            await api.post(
                `/admin/hobbies/${hobbyDoEnvio}/location-rules`,
                { relevancia: formulario.relevancia, ativo: formulario.ativo, conditions },
                { headers: headers() }
            );

            // Evita exibir uma confirmação do hobby anterior após trocar a seleção.
            setFormularioAberto(false);
            setFormulario(formularioInicial());
            setSucesso("Regra geográfica cadastrada com sucesso.");
            setVersaoListagem((versao) => versao + 1);
        } catch (error) {
            setErroCadastro(mensagemErro(error, "Não foi possível cadastrar a regra."));
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
                        <h2>Cadastrar regra geográfica</h2>
                        {!formularioAberto ? (
                            <button type="button" onClick={() => {
                                setFormularioAberto(true);
                                setErroCadastro("");
                                setSucesso("");
                            }}>
                                Nova regra
                            </button>
                        ) : (
                            <form onSubmit={cadastrarRegra}>
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

                                <h3>Condições OSM</h3>
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
                                        {salvando ? "Salvando..." : "Salvar regra"}
                                    </button>
                                    <button
                                        type="button"
                                        disabled={salvando}
                                        onClick={() => {
                                            setFormularioAberto(false);
                                            setFormulario(formularioInicial());
                                            setErroCadastro("");
                                        }}
                                    >
                                        Cancelar
                                    </button>
                                </div>
                            </form>
                        )}
                        {sucesso && <p role="status">{sucesso}</p>}
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
                            </article>
                        ))}
                    </section>
                </>
            )}
        </div>
    );
}
