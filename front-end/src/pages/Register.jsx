import { useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../api/api";

import "./css/Auth.css";


export default function Register() {

    const [form, setForm] = useState({
        nome: "",
        email: "",
        senha: "",
        dataNascimento: ""
    });

    const [erro, setErro] = useState("");

    const navigate = useNavigate();


    async function submit(e) {

        e.preventDefault();

        try {

            await api.post("/users", form);

            navigate("/login");

        } catch (err) {


            setErro(
                err.response?.data?.error ||
                JSON.stringify(err.response?.data) ||
                "Não foi possível criar a conta."
            );
        }
    }


    function atualizarCampo(campo, valor) {

        setForm((atual) => ({
            ...atual,
            [campo]: valor
        }));
    }


    return (
        <main className="auth-page">

            <section className="auth-card">

                <div className="auth-brand">
                    <span className="auth-brand-icon">✦</span>
                    <span className="auth-brand-name">
                        Recommendi.a
                    </span>
                </div>

                <header className="auth-header">

                    <h1>Crie sua conta</h1>

                    <p>
                        Descubra hobbies compatíveis com seus
                        interesses, rotina e objetivos.
                    </p>

                </header>


                <form
                    className="auth-form"
                    onSubmit={submit}
                >

                    <div className="auth-field">

                        <label htmlFor="nome">
                            Nome
                        </label>

                        <input
                            id="nome"
                            type="text"
                            placeholder="Como podemos chamar você?"
                            value={form.nome}
                            onChange={(e) =>
                                atualizarCampo(
                                    "nome",
                                    e.target.value
                                )
                            }
                            required
                        />

                    </div>


                    <div className="auth-field">

                        <label htmlFor="email">
                            E-mail
                        </label>

                        <input
                            id="email"
                            type="email"
                            placeholder="seuemail@exemplo.com"
                            value={form.email}
                            onChange={(e) =>
                                atualizarCampo(
                                    "email",
                                    e.target.value
                                )
                            }
                            required
                        />

                    </div>


                    <div className="auth-field">

                        <label htmlFor="senha">
                            Senha
                        </label>

                        <input
                            id="senha"
                            type="password"
                            placeholder="Use pelo menos 12 caracteres"
                            minLength={12}
                            maxLength={72}
                            autoComplete="new-password"
                            value={form.senha}
                            onChange={(e) =>
                                atualizarCampo(
                                    "senha",
                                    e.target.value
                                )
                            }
                            required
                        />

                    </div>


                    <div className="auth-field">

                        <label htmlFor="dataNascimento">
                            Data de nascimento
                        </label>

                        <input
                            id="dataNascimento"
                            type="date"
                            value={form.dataNascimento}
                            onChange={(e) =>
                                atualizarCampo(
                                    "dataNascimento",
                                    e.target.value
                                )
                            }
                            required
                        />

                    </div>


                    {erro && (
                        <p className="auth-error">
                            {erro}
                        </p>
                    )}


                    <button
                        className="auth-primary-button"
                        type="submit"
                    >
                        Criar conta
                    </button>


                    <div className="auth-divider">
                        <span>ou</span>
                    </div>


                    <button
                        className="auth-secondary-button"
                        type="button"
                        onClick={() => navigate("/login")}
                    >
                        Já tenho uma conta
                    </button>

                </form>


                <p className="auth-footer">
                    Depois do cadastro, faremos algumas perguntas
                    para personalizar suas recomendações.
                </p>

            </section>

        </main>
    );
}