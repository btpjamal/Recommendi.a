import { useEffect, useRef , useState } from "react";
import { api } from "../api/api.js";
import HobbyMap from "./HobbyMap";
import "./HobbyLocations.css";

export default function HobbyLocations({ hobbyId, hobbyNome, modoAdministrador = false }) {
    const [locais, setLocais] = useState([]);
    const [carregando, setCarregando] = useState(false);
    const [erro, setErro] = useState("");
    const [buscaRealizada, setBuscaRealizada] = useState(false);
    const [centro, setCentro] = useState(null);
    const [cidade, setCidade] = useState("");
    const [estado, setEstado] = useState("");
    const [progresso, setProgresso] = useState("");
    const [tentativaAtual, setTentativaAtual] = useState(0);
    const [statusBusca, setStatusBusca] = useState(null);
    const [mensagemBusca, setMensagemBusca] = useState("");

    const requisicaoAtual = useRef(null);

    useEffect(() => {
        return () => {
            requisicaoAtual.current?.abort();
        };
    }, [hobbyId]);

    useEffect(() => {
        setLocais([]);
        setErro("");
        setBuscaRealizada(false);
        setCentro(null);
        setCidade("");
        setEstado("");
        setProgresso("");
        setTentativaAtual(0);
        setCarregando(false);
        setStatusBusca(null);
        setMensagemBusca("");
    }, [hobbyId]);


    const buscarLocais = async () => {
        if (requisicaoAtual.current) {
            return;
        }

        const controller = new AbortController();
        requisicaoAtual.current = controller;

        setCarregando(true);
        setErro("");
        setProgresso("Iniciando busca de locais...");
        setTentativaAtual(0);
        setBuscaRealizada(false);
        setStatusBusca(null);
        setMensagemBusca("");
        setLocais([]);
        setCentro(null);

        let recebeuResultado = false;

        const timeout = setTimeout(() => {
            controller.abort();
        }, 115000);

        try {
            const token = localStorage.getItem("token");

            const response = await fetch(
                `http://localhost:8080/api/v1/hobbies/${hobbyId}/locations/stream`,
                {
                    method: "GET",
                    headers: {
                        Accept: "text/event-stream",
                        ...(token
                            ? { Authorization: `Bearer ${token}` }
                            : {})
                    },
                    signal: controller.signal
                }
            );

            if (!response.ok) {
                throw new Error(
                    `Erro HTTP ${response.status}`
                );
            }

            if (!response.body) {
                throw new Error(
                    "O navegador não disponibilizou o streaming."
                );
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            let buffer = "";

            const processarEvento = (bloco) => {
                const linhas = bloco.split("\n");

                let tipo = "message";
                const dados = [];

                for (const linha of linhas) {
                    if (linha.startsWith("event:")) {
                        tipo = linha.slice(6).trim();
                    } else if (linha.startsWith("data:")) {
                        dados.push(linha.slice(5).trimStart());
                    }
                }

                const conteudo = dados.join("\n");

                if (tipo === "progresso") {
                    setProgresso(conteudo);

                    if (conteudo.includes("tentativa")) {
                        setTentativaAtual((atual) =>
                            Math.min(atual + 1, 2)
                        );
                    }
                }


                if (tipo === "resultado") {
                    const resultado = JSON.parse(conteudo);

                    const status = resultado.status ??
                        ((resultado.locais ?? []).length > 0
                            ? "SUCESSO"
                            : "SEM_RESULTADOS");

                    setStatusBusca(status);
                    setMensagemBusca(resultado.mensagem ?? "");
                    setLocais(resultado.locais ?? []);

                    const coordenadasValidas =
                        resultado.latitude != null &&
                        resultado.longitude != null &&
                        Number.isFinite(Number(resultado.latitude)) &&
                        Number.isFinite(Number(resultado.longitude));

                    setCentro(
                        coordenadasValidas
                            ? {
                                latitude: Number(resultado.latitude),
                                longitude: Number(resultado.longitude)
                            }
                            : null
                    );

                    setCidade(resultado.cidade ?? "");
                    setEstado(resultado.estado ?? "");
                    setBuscaRealizada(true);
                    setProgresso("Busca concluída!");

                    recebeuResultado = true;
                }


                if (tipo === "erro") {
                    throw new Error(conteudo);
                }
            };

            while (true) {
                const { value, done } = await reader.read();

                if (done) {
                    break;
                }

                buffer += decoder.decode(value, {
                    stream: true
                });

                // SSE separa eventos por uma linha vazia.
                // Normaliza quebras CRLF antes de processar.
                buffer = buffer.replace(/\r\n/g, "\n");

                let separador;

                while ((separador = buffer.indexOf("\n\n")) !== -1) {
                    const bloco = buffer.slice(0, separador);
                    buffer = buffer.slice(separador + 2);

                    processarEvento(bloco);
                }
            }

            if (!recebeuResultado) {
                throw new Error(
                    "A conexão terminou sem retornar os locais."
                );
            }

        } catch (error) {
            console.error("Erro ao buscar locais:", error);

            if (controller.signal.aborted) {
                setErro(
                    "A busca foi interrompida ou demorou demais. " +
                    "Tente novamente."
                );
            } else {
                setErro(
                    error.message ||
                    "Não foi possível consultar os locais."
                );
            }

            setLocais([]);
            setCentro(null);
            setBuscaRealizada(false);
            setProgresso("");
            setStatusBusca(null);
            setMensagemBusca("");

        } finally {
            clearTimeout(timeout);

            if (requisicaoAtual.current === controller) {
                requisicaoAtual.current = null;
                setCarregando(false);
            }
        }
    };


    const mensagensStatus = {
        SEM_REGRAS: {
            titulo: "Locais ainda não configurados",
            descricao:
                "Este hobby ainda não possui locais de prática configurados. " +
                "Você pode continuar explorando outras recomendações."
        },

        REGRAS_INATIVAS: {
            titulo: "Busca de locais indisponível",
            descricao:
                "A busca de locais está temporariamente indisponível " +
                "para este hobby."
        },

        SEM_RESULTADOS: {
            titulo: "Nenhum local encontrado",
            descricao:
                "Não encontramos locais cadastrados para este hobby " +
                "na sua região. Você pode tentar novamente mais tarde."
        }
    };

    const informacaoStatus = mensagensStatus[statusBusca];
    const resumoRelevancia = locais.reduce((total, local) => {
        if (Object.prototype.hasOwnProperty.call(total, local.relevancia)) {
            total[local.relevancia] += 1;
        }
        return total;
    }, { ALTA: 0, MEDIA: 0, COMPLEMENTAR: 0 });


    return (
        <div className="hobby-locations">
            <p className="hobby-locations-description">
                Descubra lugares para praticar
                <strong> {hobbyNome}</strong> na sua região.
            </p>

            {!buscaRealizada && (
                <button
                    type="button"
                    className="plan-action-button"
                    onClick={buscarLocais}
                    disabled={carregando}
                >
                    {carregando
                        ? "Buscando locais..."
                        : erro
                            ? "Tentar novamente"
                            : "Encontrar locais próximos"}
                </button>
            )}

            {carregando && (
                <div
                    className="hobby-locations-progress"
                    role="status"
                    aria-live="polite"
                >
                    <div className="hobby-locations-spinner" />

                    <p>{progresso}</p>

                    {tentativaAtual > 0 && (
                        <small>
                            Tentativa {tentativaAtual} de até 2
                        </small>
                    )}

                    <div className="hobby-locations-progress-track">
                        <div
                            className="hobby-locations-progress-fill"
                            style={{
                                width: `${(tentativaAtual / 2) * 100}%`
                            }}
                        />
                    </div>
                </div>
            )}

            {erro && <p className="hobby-locations-error">{erro}</p>}

            {modoAdministrador && buscaRealizada && (
                <button type="button" className="plan-action-button" onClick={buscarLocais} disabled={carregando}>
                    Repetir teste de locais
                </button>
            )}

            {buscaRealizada && (
                <>
                    {informacaoStatus ? (
                        <div
                            className="hobby-locations-status"
                            role="status"
                        >
                <span
                    className="hobby-locations-status-icon"
                    aria-hidden="true"
                >
                    📍
                </span>

                            <h3>{informacaoStatus.titulo}</h3>

                            <p>
                                {mensagemBusca || informacaoStatus.descricao}
                            </p>
                        </div>
                    ) : (
                        <>
                            <p className="hobby-locations-count">
                                {locais.length} locais encontrados
                            </p>
                            {modoAdministrador && (
                                <div className="hobby-locations-summary" aria-label="Resumo de relevância" style={{ display: "flex", flexWrap: "wrap", gap: "0.75rem", marginBottom: "1rem" }}>
                                    <span><strong>Alta:</strong> {resumoRelevancia.ALTA}</span>
                                    <span><strong>Média:</strong> {resumoRelevancia.MEDIA}</span>
                                    <span><strong>Complementar:</strong> {resumoRelevancia.COMPLEMENTAR}</span>
                                </div>
                            )}

                            {locais.length > 0 ? (
                                <>
                                    {cidade && estado && (
                                        <p className="hobby-locations-description">
                                            Locais encontrados em {cidade} - {estado} e arredores.
                                        </p>
                                    )}
                                    {centro && (
                                        <HobbyMap
                                            key={`${centro.latitude}-${centro.longitude}`}
                                            latitude={centro.latitude}
                                            longitude={centro.longitude}
                                            locais={locais}
                                        />
                                    )}


                                    <div className="hobby-locations-list">
                                        {locais.map((local) => {
                                            const relevancia = local.relevancia;

                                            const relevanciaTexto = {
                                                ALTA: "Alta relevância",
                                                MEDIA: "Relevância média",
                                                COMPLEMENTAR: "Local complementar"
                                            }[relevancia];

                                            const distanciaValida =
                                                local.distanciaKm !== null &&
                                                local.distanciaKm !== undefined &&
                                                Number.isFinite(Number(local.distanciaKm));

                                            return (
                                                <div
                                                    className="hobby-location-item"
                                                    key={local.id}
                                                >
                                                    <div className="hobby-location-header">
                                                        <strong>{local.nome}</strong>

                                                        {relevanciaTexto && (
                                                            <span
                                                                className={`hobby-location-badge ${
                                                                    relevancia === "ALTA"
                                                                        ? "hobby-location-badge-high"
                                                                        : relevancia === "MEDIA"
                                                                            ? "hobby-location-badge-medium"
                                                                            : "hobby-location-badge-complementary"
                                                                }`}
                                                            >
                            {relevanciaTexto}
                        </span>
                                                        )}
                                                    </div>

                                                    {distanciaValida && (
                                                        <div className="hobby-location-distance">
                                                            <span aria-hidden="true">📍</span>
                                                            <span>
                            {Number(local.distanciaKm).toLocaleString(
                                "pt-BR",
                                {
                                    minimumFractionDigits: 1,
                                    maximumFractionDigits: 1
                                }
                            )} km do centro da cidade
                        </span>
                                                        </div>
                                                    )}

                                                    <p className="hobby-location-address">
                                                        {local.endereco ||
                                                            "Endereço não informado"}
                                                    </p>

                                                    <a
                                                        href={`https://www.google.com/maps/search/?api=1&query=${local.latitude}%2C${local.longitude}`}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                    >
                                                        Ver no Google Maps ↗
                                                    </a>
                                                </div>
                                            );
                                        })}
                                    </div>

                                </>
                            ) : (
                                <p>Nenhum local encontrado para este hobby.</p>
                            )}
                        </>
                    )}
                </>
            )}
        </div>
    )
}
