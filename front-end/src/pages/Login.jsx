import { isCurrentSession, session, sessionSnapshot } from "../auth/session.js";
import { useEffect, useId, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../api/api";

import "./css/Auth.css";

const AVISO_DEMORA_MS = 5000;

export default function Login() {
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [senha, setSenha] = useState("");
    const [erro, setErro] = useState("");

    const [etapa, setEtapa] = useState(null);
    const [demorando, setDemorando] = useState(false);
    const loginEmAndamento = useRef(false);
    const montado = useRef(false);
    const dialogo = useRef(null);
    const botaoAjuda = useRef(null);
    const tituloDialogoId = useId();
    const descricaoDialogoId = useId();

    const carregando = etapa !== null;

    useEffect(() => {
        montado.current = true;
        return () => {
            montado.current = false;
        };
    }, []);

    async function handleLogin(e) {
        e.preventDefault();
        if (loginEmAndamento.current) return;

        loginEmAndamento.current = true;
        setErro("");
        setDemorando(false);
        setEtapa("login");

        const avisoDeDemora = setTimeout(() => {
            if (montado.current) setDemorando(true);
        }, AVISO_DEMORA_MS);

        let consultandoPerfil = false;
        let generation = sessionSnapshot().generation;

        try {
            const response = await api.post("/auth/login", { email, senha });
            clearTimeout(avisoDeDemora);
            if (!montado.current || !isCurrentSession(generation)) return;

            const { token, userId, nome, email: emailUsuario, role } = response.data;

            generation = session.start({ token, userId, nome, email: emailUsuario, role });

            consultandoPerfil = true;
            setDemorando(false);
            setEtapa("perfil");

            const statusResponse = await api.get(
                `/recommendation-profiles/${userId}/status`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            );
            if (!montado.current || !isCurrentSession(generation)) return;

            if (role === "ADMIN") {
                navigate("/admin", { replace: true });
                return;
            }

            if (statusResponse.data === true) {
                navigate("/recommendations");
            } else {
                navigate("/onboarding");
            }
        } catch (error) {
            if (!montado.current || error?.code === "ERR_CANCELED") return;
            if (!isCurrentSession(generation) && !(
                consultandoPerfil && error?.response?.status === 401 && !session.getItem("token")
            )) return;

            const mensagemApi = error?.response?.data?.error;
            let mensagem = consultandoPerfil
                ? "Não foi possível consultar o status do seu perfil. Tente novamente."
                : "Não foi possível realizar o login.";

            if (typeof mensagemApi === "string" && mensagemApi.trim()) {
                mensagem = mensagemApi;
            } else if (
                error?.code === "ECONNABORTED" ||
                error?.code === "ETIMEDOUT"
            ) {
                mensagem = "O servidor demorou demais para responder. Tente novamente.";
            } else if (error?.request && !error?.response) {
                mensagem = "Não foi possível obter uma resposta do servidor. Tente novamente.";
            }

            setErro(mensagem);
        } finally {
            clearTimeout(avisoDeDemora);
            loginEmAndamento.current = false;
            if (montado.current) {
                setDemorando(false);
                setEtapa(null);
            }
        }
    }

    return (
        <main className="auth-page">

            <section className="auth-card">

                <div className="auth-brand">
                    <span className="auth-brand-icon">✦</span>
                    <span className="auth-brand-name">Recommendi.a</span>
                </div>

                <header className="auth-header">
                    <h1>Bem-vindo de volta</h1>
                    <p>
                        Entre na sua conta para continuar descobrindo
                        hobbies que combinam com você.
                    </p>
                </header>

                <form className="auth-form" onSubmit={handleLogin}>

                    <div className="auth-field">
                        <label htmlFor="email">E-mail</label>

                        <input
                            id="email"
                            type="email"
                            placeholder="seuemail@exemplo.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>

                    <div className="auth-field">
                        <label htmlFor="senha">Senha</label>

                        <input
                            id="senha"
                            type="password"
                            placeholder="Digite sua senha"
                            value={senha}
                            onChange={(e) => setSenha(e.target.value)}
                            required
                        />
                    </div>

                    {erro && (
                        <p className="auth-error" role="alert">
                            {erro}
                        </p>
                    )}

                    <button
                        className="auth-primary-button"
                        type="submit"
                        disabled={carregando}
                    >
                        {carregando ? "Entrando…" : "Entrar"}
                    </button>

                    <div role="status" aria-atomic="true">
                        {etapa === "login" && (
                            <p>
                                {demorando
                                    ? "O servidor ainda não respondeu. Ele pode estar voltando da suspensão por inatividade."
                                    : "Aguardando a resposta do servidor para entrar…"}
                            </p>
                        )}
                        {etapa === "perfil" && <p>Consultando o status do seu perfil…</p>}
                    </div>

                    {carregando && (
                        <progress aria-label="Progresso do login" style={{ width: "100%" }} />
                    )}

                    <button
                        ref={botaoAjuda}
                        className="auth-secondary-button"
                        type="button"
                        onClick={() => dialogo.current.showModal()}
                    >
                        Por que o login pode demorar?
                    </button>

                    <div className="auth-divider">
                        <span>ou</span>
                    </div>

                    <button
                        className="auth-secondary-button"
                        type="button"
                        onClick={() => navigate("/register")}
                    >
                        Criar uma conta
                    </button>

                </form>

                <dialog
                    ref={dialogo}
                    aria-labelledby={tituloDialogoId}
                    aria-describedby={descricaoDialogoId}
                    onClose={() => botaoAjuda.current?.focus()}
                    style={{ maxWidth: "min(28rem, calc(100% - 2rem))" }}
                >
                    <h2 id={tituloDialogoId}>Por que o login pode demorar?</h2>
                    <p id={descricaoDialogoId}>
                        A API usa o plano gratuito do Render. Após um período sem
                        acessos, o serviço pode ser suspenso. A próxima requisição
                        pode reativá-lo, o que costuma levar cerca de um minuto.
                    </p>
                    <p>
                        Se o login ainda estiver em andamento, aguarde a resposta.
                        Você pode fechar esta janela enquanto espera.
                    </p>
                    <form method="dialog">
                        <button className="auth-secondary-button" type="submit">
                            Fechar
                        </button>
                    </form>
                </dialog>

                <p className="auth-footer">
                    Descubra novos hobbies e encontre atividades
                    compatíveis com seu perfil.
                </p>

            </section>

        </main>
    );
}
