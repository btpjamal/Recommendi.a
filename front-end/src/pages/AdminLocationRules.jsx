
import { useEffect, useState } from "react";
import { api } from "../api/api";

const headers = () => ({
    Authorization: `Bearer ${localStorage.getItem("token")}`
});

const rotulo = (valor) =>
    valor?.replaceAll("_", " ").toLowerCase()
        .replace(/^./, (letra) => letra.toUpperCase()) || "—";

export default function AdminLocationRules() {
    const [hobbies, setHobbies] = useState([]);
    const [hobbyId, setHobbyId] = useState("");
    const [regras, setRegras] = useState([]);

    const [carregandoHobbies, setCarregandoHobbies] = useState(true);
    const [carregandoRegras, setCarregandoRegras] = useState(false);
    const [erroHobbies, setErroHobbies] = useState("");
    const [erroRegras, setErroRegras] = useState("");

    useEffect(() => {
        let ativo = true;

        async function carregarHobbies() {
            try {
                const response = await api.get("/hobbies", {
                    headers: headers()
                });

                if (ativo) {
                    setHobbies(response.data);
                }
            } catch (error) {
                if (ativo) {
                    setErroHobbies(
                        error.response?.data?.message ||
                        "Não foi possível carregar os hobbies."
                    );
                }
            } finally {
                if (ativo) setCarregandoHobbies(false);
            }
        }

        carregarHobbies();

        return () => {
            ativo = false;
        };
    }, []);

    useEffect(() => {
        if (!hobbyId) {
            setRegras([]);
            setErroRegras("");
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

                if (ativo) {
                    setRegras(response.data);
                }
            } catch (error) {
                if (ativo) {
                    setErroRegras(
                        error.response?.data?.message ||
                        "Não foi possível carregar as regras."
                    );
                }
            } finally {
                if (ativo) setCarregandoRegras(false);
            }
        }

        carregarRegras();

        return () => {
            ativo = false;
        };
    }, [hobbyId]);

    return (
        <div className="admin-location-rules">
            <div className="admin-page-heading">
                <h1>Regras geográficas</h1>
                <p>
                    Consulte as configurações OSM utilizadas
                    na busca de locais para cada hobby.
                </p>
            </div>

            <div className="admin-card">
                <h2>Selecionar hobby</h2>

                <label htmlFor="hobby-location-select">
                    Hobby
                </label>

                <select
                    id="hobby-location-select"
                    value={hobbyId}
                    disabled={carregandoHobbies}
                    onChange={(e) => setHobbyId(e.target.value)}
                >
                    <option value="">
                        Selecione um hobby
                    </option>

                    {hobbies.map((hobby) => (
                        <option key={hobby.id} value={hobby.id}>
                            {hobby.nome}
                        </option>
                    ))}
                </select>

                {carregandoHobbies && (
                    <p>Carregando hobbies...</p>
                )}

                {erroHobbies && (
                    <p role="alert">{erroHobbies}</p>
                )}
            </div>

            {hobbyId && (
                <section className="admin-location-rules-list">
                    <h2>Regras configuradas</h2>

                    {carregandoRegras && (
                        <p>Carregando regras...</p>
                    )}

                    {erroRegras && (
                        <p role="alert">{erroRegras}</p>
                    )}

                    {!carregandoRegras &&
                        !erroRegras &&
                        regras.length === 0 && (
                            <p>
                                Este hobby ainda não possui
                                regras geográficas cadastradas.
                            </p>
                        )}

                    {!carregandoRegras &&
                        !erroRegras &&
                        regras.map((regra) => (
                            <article
                                key={regra.id}
                                className="admin-card"
                            >
                                <h3>Regra #{regra.id}</h3>

                                <p>
                                    <strong>Relevância: </strong>
                                    {rotulo(regra.relevancia)}
                                </p>

                                <p>
                                    <strong>Status: </strong>
                                    {regra.ativo
                                        ? "Ativa"
                                        : "Inativa"}
                                </p>

                                <h4>Condições OSM</h4>

                                <ul>
                                    {regra.conditions.map(
                                        (condicao) => (
                                            <li key={condicao.id}>
                                                <code>
                                                    {condicao.osmKey}
                                                </code>

                                                {" — "}

                                                {condicao.matchType ===
                                                "EXISTE"
                                                    ? "Existe"
                                                    : `Igual a ${condicao.osmValue}`}
                                            </li>
                                        )
                                    )}
                                </ul>
                            </article>
                        ))}
                </section>
            )}
        </div>
    );
}
