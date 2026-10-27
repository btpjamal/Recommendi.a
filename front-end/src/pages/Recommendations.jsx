import { session, revokeSession } from "../auth/session.js";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";

import RecommendationTabs from "../components/recommendations/RecommendationTabs";
import DiscoverTab from "../components/recommendations/DiscoverTab";
import InterestedTab from "../components/recommendations/InterestedTab";
import NotInterestedTab from "../components/recommendations/NotInterestedTab";
import "./css/RecommendationsLayout.css"


export default function Recommendations() {

    const navigate = useNavigate();
    const [abaAtiva, setAbaAtiva] = useState("descobrir");
    const nome = session.getItem("nome");

    async function logout() {
              await revokeSession();
              navigate("/login");
          }

    return (
        <main>
            {/*ESSA É A PARTE DO CABEÇALHO QUE É COMPARTILHADO ENTRE AS 3 ABAS*/}
            <header className="app-header">

                <div className="app-header-content">

                    <div className="app-brand">
                        <span className="app-brand-icon">✦</span>

                        <div>
                            <span className="app-brand-name">
                                Recommendi.a
                            </span>

                            <span className="app-brand-subtitle">
                                Descubra seu próximo hobby
                            </span>
                        </div>
                    </div>

                    <div className="app-header-actions">

                        <span className="app-user-greeting">
                            Olá, {nome}!
                        </span>

                        <button
                            className="app-header-button"
                            onClick={() => navigate("/community")}
                        >
                            Comunidade
                        </button>

                        <button
                            className="app-header-button"
                            onClick={() => navigate("/myProfile")}
                        >
                            Meu Perfil
                        </button>

                        <button
                            className="app-header-button"
                            onClick={() =>
                                navigate("/onboarding?editar=true")
                            }
                        >
                            Atualizar preferências
                        </button>

                        <button
                            className="app-logout-button"
                            onClick={logout}
                        >
                            Sair
                        </button>

                    </div>

                </div>

            </header>

            <RecommendationTabs
                abaAtiva={abaAtiva}
                setAbaAtiva={setAbaAtiva}
            />

            {abaAtiva === "descobrir" && (
                <DiscoverTab />
            )}

            {abaAtiva === "interessados" && (
                <InterestedTab />
            )}

            {abaAtiva === "nao-interessados" && (
                <NotInterestedTab />
            )}
        </main>
    );
}
