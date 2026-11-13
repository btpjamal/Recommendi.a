import { useSyncExternalStore } from "react";
import { getLocationSearch, startLocationSearch, subscribeLocationSearch } from "./hobbyLocationSearch.js";
import HobbyMap from "./HobbyMap";
import "./HobbyLocations.css";

export default function HobbyLocations({ hobbyId, hobbyNome, modoAdministrador = false }) {
    const { loading: carregando, progress: progresso, attempt: tentativaAtual, result, error: erro } =
        useSyncExternalStore(subscribeLocationSearch, () => getLocationSearch(hobbyId));
    const buscarLocais = () => startLocationSearch(hobbyId, hobbyNome, modoAdministrador);
    const buscaRealizada = result !== null;
    const locais = result?.locais ?? [];
    const cidade = result?.cidade ?? "";
    const estado = result?.estado ?? "";
    const statusBusca = result?.status ?? (locais.length ? "SUCESSO" : "SEM_RESULTADOS");
    const mensagemBusca = result?.mensagem ?? "";
    const centro = result?.latitude != null && result?.longitude != null &&
        Number.isFinite(Number(result.latitude)) && Number.isFinite(Number(result.longitude))
        ? { latitude: Number(result.latitude), longitude: Number(result.longitude) }
        : null;
    const dataResultadoSalvo = result?.dadosDesatualizados && result.consultadoEm
        ? new Date(result.consultadoEm).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" })
        : null;

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
        <div className="hobby-locations" id="locais-proximos">
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
                    <small>Você pode continuar navegando e voltar aqui para ver o resultado.</small>

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
                    {dataResultadoSalvo && (
                        <p className="hobby-locations-status" role="status">
                            Exibindo o resultado salvo em {dataResultadoSalvo}. A busca ao vivo está indisponível.
                        </p>
                    )}
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
