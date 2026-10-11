import { useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../api/api";

import "./css/Auth.css";

export default function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState("");

  async function handleLogin(e) {
    e.preventDefault();
    setErro("");

    try {
      const response = await api.post("/auth/login", {
        email,
        senha,
      });

      console.log("Resposta do login:", response.data);

      const { token, userId, nome, email: emailUsuario, role } = response.data;

      localStorage.setItem("token", token);
      localStorage.setItem("userId", userId);
      localStorage.setItem("nome", nome);
      localStorage.setItem("email", emailUsuario);
      localStorage.setItem("role", role);


      const statusResponse = await api.get(
          `/recommendation-profiles/${userId}/status`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
      );

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
      console.error(error);

      setErro(
          error.response?.data?.message ||
          "Não foi possível realizar o login."
      );
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
                          <p className="auth-error">
                              {erro}
                          </p>
                      )}

                      <button
                          className="auth-primary-button"
                          type="submit"
                      >
                          Entrar
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

                  <p className="auth-footer">
                      Descubra novos hobbies e encontre atividades
                      compatíveis com seu perfil.
                  </p>

              </section>

          </main>
      );
    }
