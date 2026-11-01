import { session } from "../auth/session.js";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { api } from "../api/api";
import "./css/Onboarding.css";

const perguntas = [
    {
        campo: "tempoDisponivelSemanal",
        titulo: "Quanto tempo livre você possui por semana?",
        opcoes: [
            { label: "Até 2 horas", valor: 2 },
            { label: "De 2 a 5 horas", valor: 5 },
            { label: "De 5 a 10 horas", valor: 10 },
            { label: "Mais de 10 horas", valor: 15 },
        ],
    },
    {
        campo: "orcamentoInicial",
        titulo: "Quanto pretende investir para começar?",
        opcoes: [
            { label: "Até R$ 50", valor: 50 },
            { label: "Até R$ 100", valor: 100 },
            { label: "Até R$ 300", valor: 300 },
            { label: "Mais de R$ 300", valor: 500 },
        ],
    },
    {
        campo: "tipoSocializacao",
        titulo: "Como você prefere praticar um hobby?",
        opcoes: [
            {
                label: "Prefiro atividades individuais",
                valor: "INDIVIDUAL"
            },
            {
                label: "Prefiro atividades com outras pessoas",
                valor: "SOCIAL"
            },
            {
                label: "Não tenho preferência",
                valor: "INDIFERENTE"
            },
        ],
    },
    {
        campo: "nivelAtividadeFisicaDesejada",
        titulo: "Qual intensidade física você prefere?",
        opcoes: [
            { label: "Baixa", valor: "BAIXO" },
            { label: "Moderada", valor: "MODERADO" },
            { label: "Alta", valor: "ALTO" },
            { label: "Indiferente", valor: "INDIFERENTE" },
        ],
    },
    {
        campo: "ambientePreferido",
        titulo: "Onde você prefere praticar?",
        opcoes: [
            { label: "Em casa", valor: "CASA" },
            { label: "Ao ar livre", valor: "AO_AR_LIVRE" },
            { label: "Em ambiente fechado", valor: "AMBIENTE_FECHADO" },
            { label: "Tanto faz", valor: "INDIFERENTE" },
        ],
    },
    {
        campo: "localizacao",
        titulo: "Onde você mora atualmente?",
        localizacao: true
    },
    {
        campo: "interestIds",
        titulo: "Quais assuntos despertam mais seu interesse?",
        multiplaEscolha: true
    },
    {
        campo: "objectiveIds",
        titulo: "O que você busca em um hobby?",
        multiplaEscolha: true
    }
];

const estados = [
    { uf: "AC", nome: "Acre" },
    { uf: "AL", nome: "Alagoas" },
    { uf: "AP", nome: "Amapá" },
    { uf: "AM", nome: "Amazonas" },
    { uf: "BA", nome: "Bahia" },
    { uf: "CE", nome: "Ceará" },
    { uf: "DF", nome: "Distrito Federal" },
    { uf: "ES", nome: "Espírito Santo" },
    { uf: "GO", nome: "Goiás" },
    { uf: "MA", nome: "Maranhão" },
    { uf: "MT", nome: "Mato Grosso" },
    { uf: "MS", nome: "Mato Grosso do Sul" },
    { uf: "MG", nome: "Minas Gerais" },
    { uf: "PA", nome: "Pará" },
    { uf: "PB", nome: "Paraíba" },
    { uf: "PR", nome: "Paraná" },
    { uf: "PE", nome: "Pernambuco" },
    { uf: "PI", nome: "Piauí" },
    { uf: "RJ", nome: "Rio de Janeiro" },
    { uf: "RN", nome: "Rio Grande do Norte" },
    { uf: "RS", nome: "Rio Grande do Sul" },
    { uf: "RO", nome: "Rondônia" },
    { uf: "RR", nome: "Roraima" },
    { uf: "SC", nome: "Santa Catarina" },
    { uf: "SP", nome: "São Paulo" },
    { uf: "SE", nome: "Sergipe" },
    { uf: "TO", nome: "Tocantins" }
];



export default function Onboarding() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const modoEdicao =
     searchParams.get("editar") === "true";

    const [cidades, setCidades] = useState([]);
    const [carregandoCidades, setCarregandoCidades] = useState(false);
    const [erroCidades, setErroCidades] = useState(false);

     //Dados carregados do backend
    const [objetivos, setObjetivos] = useState([]);
    const [interesses, setInteresses] = useState([]);

    //Seleções múltiplas
    const [objetivosSelecionados, setObjetivosSelecionados] = useState([]);
    const [interessesSelecionados,setInteressesSelecionados] = useState([]);

    // Estado da interface
        const [etapa, setEtapa] = useState(0);
        const [enviando, setEnviando] = useState(false);
        const [erro, setErro] = useState("");

        // Perfil
        const [profile, setProfile] = useState({
            tempoDisponivelSemanal: null,
            orcamentoInicial: null,
            tipoSocializacao: null,
            nivelAtividadeFisicaDesejada: null,
            ambientePreferido: null,
            cidade: "",
            estado: "",
            interestIds: [],
            objectiveIds: []
        });

    useEffect(() => {
        async function carregarDados() {
            try {
                const token = session.getItem("token");
                const userId = session.getItem("userId");

                if(!token || !userId) {
                    navigate("/login");
                    return;
                }

                const [objetivosResponse, interessesResponse] =
                                await Promise.all([
                                    api.get("/objectives", {
                                        headers: {
                                            Authorization: `Bearer ${token}`,
                                        },
                                    }),


                                    api.get("/interests", {
                                        headers: {
                                            Authorization: `Bearer ${token}`,
                                        },
                                    }),
                                ]);


                            setObjetivos(objetivosResponse.data);
                            setInteresses(interessesResponse.data);

                if (modoEdicao) {
                                const profileResponse = await api.get(
                                    `/recommendation-profiles/${userId}`,
                                    {
                                        headers: {
                                            Authorization: `Bearer ${token}`,
                                        },
                                    }
                                );

                                const dados = profileResponse.data;

                                setProfile({
                                    tempoDisponivelSemanal:
                                        dados.tempoDisponivelSemanal,

                                    orcamentoInicial:
                                        dados.orcamentoInicial,

                                    tipoSocializacao:
                                    dados.tipoSocializacao,

                                    nivelAtividadeFisicaDesejada:
                                        dados.nivelAtividadeFisicaDesejada,

                                    ambientePreferido:
                                        dados.ambientePreferido,

                                    cidade:
                                        dados.cidade ?? "",

                                    estado:
                                        dados.estado ?? "",

                                    interestIds:
                                        dados.interestIds ?? [],

                                    objectiveIds:
                                        dados.objectiveIds ?? []
                                });

                                setInteressesSelecionados(
                                    dados.interestIds ?? []
                                );

                                setObjetivosSelecionados(
                                    dados.objectiveIds ?? []
                                );
                            }

            } catch {


                setErro(
                    "Não foi possível carregar as suas preferências"
                );
            }
        }

        carregarDados();
    }, [modoEdicao, navigate]);

    useEffect(() => {
        if (!profile.estado) {
            setCidades([]);
            setErroCidades(false);
            return;
        }

        const controller = new AbortController();

        async function carregarCidades() {
            setCarregandoCidades(true);
            setErroCidades(false);
            setCidades([]);

            try {
                const response = await fetch(
                    `https://servicodados.ibge.gov.br/api/v1/localidades/estados/${profile.estado}/municipios`,
                    { signal: controller.signal }
                );

                if (!response.ok) {
                    throw new Error("Falha ao consultar municípios.");
                }

                const dados = await response.json();

                const municipios = dados
                    .map((municipio) => municipio.nome)
                    .sort((a, b) => a.localeCompare(b, "pt-BR"));

                setCidades(municipios);
            } catch (error) {
                if (error.name !== "AbortError") {
                    setErroCidades(true);
                }
            } finally {
                if (!controller.signal.aborted) {
                    setCarregandoCidades(false);
                }
            }
        }

        carregarCidades();

        return () => controller.abort();
    }, [profile.estado]);

    function alternarObjetivo(id) {
        setObjetivosSelecionados((atuais) => {

            if (atuais.includes(id)) {
                return atuais.filter(
                    (objectiveId) => objectiveId !== id
                );
            }

            return [...atuais, id];
        });
    }

    function alternarInteresse(id) {
        setInteressesSelecionados((atuais) => {
            if (atuais.includes(id)) {
                return atuais.filter(
                    (interestId) => interestId !== id
                );
            }

            return [...atuais, id];
        });
    }


    const perguntaAtual = perguntas[etapa];
    const progresso = ((etapa + 1) / perguntas.length) * 100;

    async function selecionarOpcao(valor) {
        const profileAtualizado = {
            ...profile,
            [perguntaAtual.campo]: valor,
        };

        setProfile(profileAtualizado);
        setErro("");

        const ultimaEtapa = etapa === perguntas.length - 1;

        if (ultimaEtapa) {
            await finalizarOnboarding(profileAtualizado);
            return;
        }

        setEtapa((etapaAtual) => etapaAtual + 1);
    }

    async function finalizarOnboarding(dados) {
        try {
            setEnviando(true);
            setErro("");

            const userId = session.getItem("userId");
            const token = session.getItem("token");

            if (!userId || !token) {
                navigate("/login");
                return;
            }


            await api.post(
                `/recommendation-profiles/${userId}`,
                dados,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            navigate("/MyProfile");


        } catch (error) {
            const status = error.response?.status;
            const dados = error.response?.data;

            let mensagem = "Não foi possível salvar suas preferências. Tente novamente.";

            if (!error.response) {
                mensagem = "Não foi possível conectar ao servidor. Verifique sua conexão.";
            } else if (status === 400) {
                mensagem = "Algumas informações são inválidas. Revise suas preferências e tente novamente.";
            } else if (status === 401) {
                mensagem = "Sua sessão expirou. Faça login novamente.";
            } else if (status === 403) {
                mensagem = "Você não tem permissão para realizar esta operação.";
            } else if (status === 429) {
                mensagem = "Muitas tentativas em pouco tempo. Aguarde e tente novamente.";
            } else if (status >= 500) {
                mensagem = "O servidor apresentou um problema. Tente novamente mais tarde.";
            } else if (
                typeof dados?.message === "string" &&
                dados.message.trim()
            ) {
                mensagem = dados.message;
            }

            setErro(mensagem);
        }

        finally {
            setEnviando(false);
        }
    }

    function voltar() {
        if (etapa === 0) {

            if (modoEdicao) {
                navigate("/MyProfile");
            } else {
                navigate("/login");
            }

            return;
        }

        setEtapa((etapaAtual) => etapaAtual - 1);
    }

    return (
        <main className="onboarding-page">
            <section className="onboarding-card">
                <div className="onboarding-progress">
                    <div
                        className="onboarding-progress-value"
                        style={{ width: `${progresso}%` }}
                    />
                </div>

                <span className="onboarding-step">
          Etapa {etapa + 1} de {perguntas.length}
        </span>

                <h1>{perguntaAtual.titulo}</h1>

                <p className="onboarding-description">
                    {perguntaAtual.multiplaEscolha
                        ? "Você pode selecionar mais de uma opção."
                        : "Escolha a opção que mais combina com você."
                    }
                </p>

                {perguntaAtual.campo === "interestIds" && (
                    <>
                        <div className="onboarding-options">

                            {interesses.map((interesse) => {

                                const selecionado =
                                    interessesSelecionados.includes(
                                        interesse.id
                                    );

                                return (
                                    <button
                                        type="button"
                                        key={interesse.id}
                                        className={`onboarding-option ${
                                            selecionado ? "selected" : ""
                                        }`}
                                        onClick={() =>
                                            alternarInteresse(interesse.id)
                                        }
                                        disabled={enviando}
                                    >
                                        {interesse.nome}
                                    </button>
                                );
                            })}

                        </div>

                        <button
                            type="button"
                            className="onboarding-continue"
                            disabled={enviando}
                            onClick={() => {

                                if (interessesSelecionados.length === 0) {
                                    setErro(
                                        "Selecione pelo menos um interesse."
                                    );
                                    return;
                                }

                                const atualizado = {
                                    ...profile,
                                    interestIds:
                                        interessesSelecionados
                                };

                                setProfile(atualizado);
                                setErro("");

                                setEtapa((atual) => atual + 1);
                            }}
                        >
                            Continuar
                        </button>
                    </>
                )}

                {!perguntaAtual.multiplaEscolha &&
                    !perguntaAtual.localizacao && (
                    <div className="onboarding-options">
                        {perguntaAtual.opcoes.map((opcao) => {
                            const selecionada =
                                profile[perguntaAtual.campo] === opcao.valor;

                            return (
                                <button
                                    type="button"
                                    key={opcao.label}
                                    className={`onboarding-option ${
                                        selecionada ? "selected" : ""
                                    }`}
                                    onClick={() => selecionarOpcao(opcao.valor)}
                                    disabled={enviando}
                                >
                                    {opcao.label}
                                </button>
                            );
                        })}
                    </div>
                )}

                {perguntaAtual.campo === "objectiveIds" && (
                    <>
                        <div className="onboarding-options">
                            {objetivos.map((objetivo) => {
                                const selecionado =
                                    objetivosSelecionados.includes(objetivo.id);

                                return (
                                    <button
                                        type="button"
                                        key={objetivo.id}
                                        className={`onboarding-option ${
                                            selecionado ? "selected" : ""
                                        }`}
                                        onClick={() =>
                                            alternarObjetivo(objetivo.id)
                                        }
                                    >
                                        {objetivo.nome}
                                    </button>
                                );
                            })}
                        </div>

                        <button
                            type="button"
                            className="onboarding-continue"
                            onClick={() => {

                                if (objetivosSelecionados.length === 0) {
                                    setErro(
                                        "Selecione pelo menos um objetivo."
                                    );
                                    return;
                                }

                                const atualizado = {
                                    ...profile,
                                    objectiveIds:
                                        objetivosSelecionados
                                };

                                setProfile(atualizado);
                                setErro("");


                                finalizarOnboarding(atualizado);

                            }}
                        >
                            Continuar
                        </button>
                    </>
                )}

                {perguntaAtual.localizacao && (
                    <>
                        <div className="onboarding-location">
                            <label>
                                Estado

                                <select
                                    value={profile.estado}
                                    onChange={(e) => {
                                        setProfile((atual) => ({
                                            ...atual,
                                            estado: e.target.value,
                                            cidade: ""
                                        }));
                                        setErro("");
                                    }}
                                    disabled={enviando}
                                >
                                    <option value="">Selecione seu estado</option>

                                    {estados.map((estado) => (
                                        <option key={estado.uf} value={estado.uf}>
                                            {estado.nome} ({estado.uf})
                                        </option>
                                    ))}
                                </select>
                            </label>

                            <label>
                                Cidade

                                <select
                                    value={profile.cidade}
                                    onChange={(e) => {
                                        setProfile((atual) => ({
                                            ...atual,
                                            cidade: e.target.value
                                        }));
                                        setErro("");
                                    }}
                                    disabled={
                                        enviando ||
                                        !profile.estado ||
                                        carregandoCidades ||
                                        erroCidades
                                    }
                                >
                                    <option value="">
                                        {carregandoCidades
                                            ? "Carregando cidades..."
                                            : "Selecione sua cidade"}
                                    </option>

                                    {cidades.map((cidade) => (
                                        <option key={cidade} value={cidade}>
                                            {cidade}
                                        </option>
                                    ))}
                                </select>
                            </label>
                        </div>

                        {erroCidades && (
                            <p className="onboarding-error">
                                Não foi possível carregar as cidades.
                                Tente selecionar o estado novamente.
                            </p>
                        )}

                        <button
                            type="button"
                            className="onboarding-continue"
                            disabled={
                                enviando ||
                                carregandoCidades ||
                                erroCidades
                            }
                            onClick={() => {
                                if (
                                    !profile.estado ||
                                    !profile.cidade ||
                                    !cidades.includes(profile.cidade)
                                ) {
                                    setErro(
                                        "Selecione um estado e uma cidade válidos."
                                    );
                                    return;
                                }

                                setErro("");
                                setEtapa((atual) => atual + 1);
                            }}
                        >
                            Continuar
                        </button>
                    </>
                )}


                {enviando && (
                    <p className="onboarding-message">
                        {modoEdicao
                            ? "Atualizando suas preferências..."
                            : "Preparando suas recomendações..."
                        }
                    </p>
                )}

                {erro && (
                    <p className="onboarding-error">
                        {erro}
                    </p>
                )}

                <button
                    type="button"
                    className="onboarding-back"
                    onClick={voltar}
                    disabled={enviando}
                >
                    Voltar
                </button>
            </section>
        </main>
    );
}