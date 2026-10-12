
import { useCallback, useEffect, useState } from "react";
import { api } from "../api/api";

export default function AdminHobbies() {
    const [hobbies, setHobbies] = useState([]);
    const [busca, setBusca] = useState("");
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState("");

    const carregarHobbies = useCallback(async () => {
        try {
            setCarregando(true);
            setErro("");

            const response = await api.get("/hobbies", {
                headers: {
                    Authorization: `Bearer ${localStorage.getItem("token")}`
                }
            });

            setHobbies(response.data);
        } catch (error) {
            console.error("Erro ao carregar hobbies:", error);

            setHobbies([]);
            setErro(
                error.response?.data?.message ||
                "Não foi possível carregar os hobbies."
            );
        } finally {
            setCarregando(false);
        }
    }, []);

    useEffect(() => {
        carregarHobbies();
    }, [carregarHobbies]);

    const hobbiesFiltrados = hobbies.filter((hobby) =>
        hobby.nome?.toLowerCase().includes(busca.trim().toLowerCase())
    );

    function formatarMoeda(valor) {
        if (valor == null) return "Não informado";

        return Number(valor).toLocaleString("pt-BR", {
            style: "currency",
            currency: "BRL"
        });
    }

    return (
        <div className="admin-hobbies">
            <div className="admin-page-heading">
                <h1>Catálogo de hobbies</h1>
                <p>
                    Consulte e gerencie os hobbies cadastrados no Recommendi.a.
                </p>
            </div>

            <div className="admin-hobbies-toolbar">
                <input
                    type="search"
                    placeholder="Buscar hobby pelo nome..."
                    value={busca}
                    onChange={(event) => setBusca(event.target.value)}
                    aria-label="Buscar hobby pelo nome"
                />

                <button
                    type="button"
                    className="admin-refresh-button"
                    onClick={carregarHobbies}
                    disabled={carregando}
                >
                    Atualizar lista
                </button>
            </div>

            {!carregando && !erro && (
                <p className="admin-hobbies-count">
                    {hobbiesFiltrados.length} hobby(s) encontrado(s)
                </p>
            )}

            {carregando && (
                <p className="admin-loading">Carregando hobbies...</p>
            )}

            {erro && (
                <p className="admin-error" role="alert">{erro}</p>
            )}

            {!carregando && !erro && hobbiesFiltrados.length === 0 && (
                <div className="admin-empty-state">
                    <h2>Nenhum hobby encontrado</h2>
                    <p>
                        {busca
                            ? "Tente pesquisar por outro nome."
                            : "Ainda não existem hobbies cadastrados."}
                    </p>
                </div>
            )}

            {!carregando && !erro && hobbiesFiltrados.length > 0 && (
                <div className="admin-hobbies-list">
                    {hobbiesFiltrados.map((hobby) => (
                        <article className="admin-hobby-card" key={hobby.id}>
                            <div className="admin-suggestion-header">
                                <div>
                                    <h2>{hobby.nome}</h2>
                                    <span className="admin-suggestion-category">
                                        {hobby.category?.nome || "Sem categoria"}
                                    </span>
                                </div>

                                <span className="admin-hobby-id">
                                    #{hobby.id}
                                </span>
                            </div>

                            <p className="admin-suggestion-description">
                                {hobby.descricao}
                            </p>

                            <div className="admin-suggestion-details">
                                <div>
                                    <small>Custo inicial</small>
                                    <strong>
                                        {formatarMoeda(hobby.custoEstimado)}
                                    </strong>
                                </div>

                                <div>
                                    <small>Custo recorrente</small>
                                    <strong>
                                        {formatarMoeda(hobby.custoRecorrenteEstimado)}
                                    </strong>
                                </div>

                                <div>
                                    <small>Dificuldade</small>
                                    <strong>
                                        {hobby.nivelDificuldade ?? "—"}
                                    </strong>
                                </div>

                                <div>
                                    <small>Tempo necessário</small>
                                    <strong>
                                        {hobby.tempoNecessario != null
                                            ? `${hobby.tempoNecessario} h`
                                            : "Não informado"}
                                    </strong>
                                </div>
                            </div>
                        </article>
                    ))}
                </div>
            )}
        </div>
    );
}
