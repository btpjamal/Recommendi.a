import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import SafeMarkdown from "../components/SafeMarkdown.jsx";

import "./css/GeneralPlan.css";

import { api } from "../api/api.js";

function GeneralPlan() {

    const navigate = useNavigate();

    const [plano, setPlano] = useState(null);
    const [carregando, setCarregando] = useState(true);
    const [gerando, setGerando] = useState(false);
    const [erro, setErro] = useState("");

    useEffect(() => {
        carregarPlano();
    }, []);

    async function carregarPlano() {
        try {
            setCarregando(true);
            setErro("");

            const response = await api.get("/ai/general-plan");

            setPlano(response.data);

        } catch (error) {

            if (error.response?.status === 404) {
                setPlano(null);
                return;
            }

            setErro(
                error.response?.data?.error ||
                "Não foi possível carregar o plano geral."
            );

        } finally {
            setCarregando(false);
        }
    }

    async function gerarPlano() {
        try {
            setGerando(true);
            setErro("");

            const response =
                await api.post("/ai/general-plan");

            setPlano(response.data);

        } catch (error) {

            setErro(
                error.response?.data?.error ||
                "Não foi possível gerar o plano geral."
            );

        } finally {
            setGerando(false);
        }
    }

    async function regenerarPlano() {
        try {
            setGerando(true);
            setErro("");

            const response =
                await api.post(
                    "/ai/general-plan/regenerate"
                );

            setPlano(response.data);

        } catch (error) {

            setErro(
                error.response?.data?.error ||
                "Não foi possível atualizar o plano geral."
            );

        } finally {
            setGerando(false);
        }
    }

    if (carregando) {
        return (
            <div className="general-plan-page">
                <p>Carregando plano...</p>
            </div>
        );
    }

    return (
        <div className="general-plan-page">

            <button
                type="button"
                className="general-plan-back-button"
                onClick={() => navigate(-1)}
            >
                ← Voltar
            </button>

            <header className="general-plan-header">
                <h1>Minha rotina de hobbies</h1>

                <p>
                    Organize seus hobbies de forma equilibrada
                    considerando seu tempo, interesses e rotina.
                </p>
            </header>

            {erro && (
                <div className="general-plan-error">
                    {erro}
                </div>
            )}

            {!plano && (
                <section className="general-plan-empty">

                    <h2>Crie sua rotina personalizada</h2>

                    <p>
                        A inteligência artificial analisará seus
                        hobbies de interesse e organizará uma rotina
                        sustentável com base no seu perfil.
                    </p>

                    <button
                        type="button"
                        className="general-plan-generate-button"
                        onClick={gerarPlano}
                        disabled={gerando}
                    >
                        {gerando
                            ? "Criando plano..."
                            : "✨ Criar plano geral"}
                    </button>

                </section>
            )}

            {plano && (
                <>
                    {plano.stale && (
                        <div className="general-plan-stale-warning">

                            <div>
                                <strong>
                                    Seu perfil ou seus hobbies mudaram.
                                </strong>

                                <p>
                                    Este plano foi criado com informações
                                    anteriores. Atualize para receber uma
                                    rotina compatível com seu contexto atual.
                                </p>
                            </div>

                            <button
                                type="button"
                                className="general-plan-regenerate-button"
                                onClick={regenerarPlano}
                                disabled={gerando}
                            >
                                {gerando
                                    ? "Atualizando..."
                                    : "✨ Atualizar plano"}
                            </button>

                        </div>
                    )}

                    <section className="general-plan-content">

                        <SafeMarkdown>
                            {plano.conteudo}
                        </SafeMarkdown>

                    </section>
                </>
            )}

        </div>
    );
}

export default GeneralPlan;