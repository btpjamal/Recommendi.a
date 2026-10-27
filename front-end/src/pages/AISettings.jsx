import { API_BASE_URL } from "../api/config.js";
import { session } from "../auth/session.js";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import "./css/AISettings.css";

const API_URL = API_BASE_URL + "/ai/configuration";

export default function AISettings() {
    const navigate = useNavigate();

    const [configurada, setConfigurada] = useState(false);
    const [apiKey, setApiKey] = useState("");
    const [mostrarCampo, setMostrarCampo] = useState(false);
    const [mostrarChave, setMostrarChave] = useState(false);

    const [carregando, setCarregando] = useState(true);
    const [processando, setProcessando] = useState(false);
    const [erro, setErro] = useState("");
    const [sucesso, setSucesso] = useState("");

    const headers = () => ({
        Authorization: `Bearer ${session.getItem("token")}`
    });

    useEffect(() => {
        let ativo = true;

        async function carregarStatus() {
            try {
                const response = await axios.get(
                    `${API_URL}/status`,
                    { headers: headers() }
                );

                if (ativo) {
                    setConfigurada(response.data.configurada);
                }
            } catch {


                if (ativo) {
                    setErro("Não foi possível carregar as configurações de IA.");
                }
            } finally {
                if (ativo) setCarregando(false);
            }
        }

        carregarStatus();

        return () => {
            ativo = false;
        };
    }, []);

    async function salvarChave(event) {
        event.preventDefault();

        if (!apiKey.trim()) {
            setErro("Informe sua chave de API.");
            return;
        }

        setProcessando(true);
        setErro("");
        setSucesso("");

        try {
            await axios.put(
                `${API_URL}/api-key`,
                { apiKey: apiKey.trim() },
                { headers: headers() }
            );

            setConfigurada(true);
            setApiKey("");
            setMostrarCampo(false);
            setMostrarChave(false);
            setSucesso("Chave de API salva com sucesso.");
        } catch (error) {
            setErro(
                error.response?.data?.error ||
                "Não foi possível salvar a chave."
            );
        } finally {
            setProcessando(false);
        }
    }

    async function removerChave() {
        const confirmado = window.confirm(
            "Deseja remover sua chave de API? Seus planos já gerados serão preservados."
        );

        if (!confirmado) return;

        setProcessando(true);
        setErro("");
        setSucesso("");

        try {
            await axios.delete(
                `${API_URL}/api-key`,
                { headers: headers() }
            );

            setConfigurada(false);
            setApiKey("");
            setMostrarCampo(false);
            setMostrarChave(false);
            setSucesso("Chave removida com sucesso.");
        } catch (error) {
            setErro(
                error.response?.data?.error ||
                "Não foi possível remover a chave."
            );
        } finally {
            setProcessando(false);
        }
    }

    return (
        <div className="ai-settings-page">
            <main className="ai-settings-container">
                <button
                    type="button"
                    className="ai-settings-back"
                    onClick={() => navigate("/myProfile")}
                >
                    ← Voltar ao perfil
                </button>

                <div className="ai-settings-heading">
                    <span className="ai-settings-label">
                        PERSONALIZAÇÃO
                    </span>

                    <h1>Configurações de IA</h1>

                    <p>
                        Configure sua própria chave do Google Gemini
                        para gerar planos personalizados.
                    </p>
                </div>

                <section className="ai-settings-card">
                    <div className="ai-settings-card-header">
                        <div>
                            <h2>Google Gemini</h2>
                            <p>Provedor de inteligência artificial</p>
                        </div>

                        {!carregando && !erro && (
                            <span
                                className={`ai-settings-status ${
                                    configurada ? "active" : "inactive"
                                }`}
                            >
                                {configurada
                                    ? "Chave configurada"
                                    : "Não configurada"}
                            </span>
                        )}
                    </div>

                    {carregando ? (
                        <p>Carregando configuração...</p>
                    ) : (
                        <>
                            <div className="ai-settings-description">
                                <p>
                                    Sua chave é armazenada de forma
                                    criptografada no servidor e utilizada
                                    para solicitar a geração dos seus planos.
                                </p>

                                <p>
                                    Os planos já gerados continuam
                                    disponíveis mesmo após a remoção da chave.
                                </p>
                            </div>

                            {erro && (
                                <p className="ai-settings-alert error" role="alert">
                                    {erro}
                                </p>
                            )}

                            {sucesso && (
                                <p className="ai-settings-alert success" role="status">
                                    {sucesso}
                                </p>
                            )}

                            {(!configurada || mostrarCampo) && (
                                <form
                                    onSubmit={salvarChave}
                                    className="ai-settings-form"
                                >
                                    <label htmlFor="gemini-api-key">
                                        Chave de API do Gemini
                                    </label>

                                    <div className="ai-settings-input-row">
                                        <input
                                            id="gemini-api-key"
                                            type={mostrarChave ? "text" : "password"}
                                            value={apiKey}
                                            onChange={(event) =>
                                                setApiKey(event.target.value)
                                            }
                                            placeholder="Cole sua chave de API"
                                            autoComplete="off"
                                            spellCheck={false}
                                            maxLength={500}
                                            disabled={processando}
                                        />

                                        <button
                                            type="button"
                                            className="ai-settings-secondary"
                                            onClick={() =>
                                                setMostrarChave(!mostrarChave)
                                            }
                                            disabled={processando}
                                        >
                                            {mostrarChave ? "Ocultar" : "Mostrar"}
                                        </button>
                                    </div>

                                    <div className="ai-settings-actions">
                                        <button
                                            type="submit"
                                            className="ai-settings-primary"
                                            disabled={processando || !apiKey.trim()}
                                        >
                                            {processando
                                                ? "Salvando..."
                                                : configurada
                                                    ? "Substituir chave"
                                                    : "Salvar chave"}
                                        </button>

                                        {configurada && (
                                            <button
                                                type="button"
                                                className="ai-settings-secondary"
                                                disabled={processando}
                                                onClick={() => {
                                                    setMostrarCampo(false);
                                                    setApiKey("");
                                                    setMostrarChave(false);
                                                    setErro("");
                                                }}
                                            >
                                                Cancelar
                                            </button>
                                        )}
                                    </div>
                                </form>
                            )}

                            {configurada && !mostrarCampo && (
                                <div className="ai-settings-configured">
                                    <div>
                                        <strong>Chave cadastrada</strong>
                                        <p>
                                            Por segurança, a chave não pode
                                            ser visualizada novamente.
                                        </p>
                                    </div>

                                    <div className="ai-settings-actions">
                                        <button
                                            type="button"
                                            className="ai-settings-secondary"
                                            disabled={processando}
                                            onClick={() => {
                                                setMostrarCampo(true);
                                                setErro("");
                                                setSucesso("");
                                            }}
                                        >
                                            Alterar chave
                                        </button>

                                        <button
                                            type="button"
                                            className="ai-settings-danger"
                                            disabled={processando}
                                            onClick={removerChave}
                                        >
                                            Remover chave
                                        </button>
                                    </div>
                                </div>
                            )}

                            <div className="ai-settings-help">
                                <h3>Como obter uma chave?</h3>
                                <p>
                                    Acesse o Google AI Studio, crie uma chave
                                    de API e cadastre-a nesta página.
                                    A utilização está sujeita aos limites
                                    e às condições do Google.
                                </p>

                                <a
                                    href="https://aistudio.google.com/apikey"
                                    target="_blank"
                                    rel="noopener noreferrer"
                                >
                                    Acessar Google AI Studio ↗
                                </a>
                            </div>
                        </>
                    )}
                </section>
            </main>
        </div>
    );
}