import NotificationBell from "../components/social/SocialNotifications.jsx";
import { session, revokeSession } from "../auth/session.js";
import { useNavigate, useSearchParams } from "react-router";

import RecommendationTabs from "../components/recommendations/RecommendationTabs";
import DiscoverTab from "../components/recommendations/DiscoverTab";
import InterestedTab from "../components/recommendations/InterestedTab";
import NotInterestedTab from "../components/recommendations/NotInterestedTab";
import "./css/RecommendationsLayout.css";
import "./css/DiscoverTheme.css";


export default function Recommendations() {

    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const aba = searchParams.get("aba");
    const abaAtiva = ["interessados", "nao-interessados"].includes(aba) ? aba : "descobrir";

    function setAbaAtiva(value) {
        setSearchParams(current => {
            const next = new URLSearchParams(current);
            if (value === "descobrir") next.delete("aba");
            else next.set("aba", value);
            return next;
        }, { replace: true });
    }
    const nome = session.getItem("nome");

    async function logout() {
              await revokeSession();
              navigate("/login");
          }

    return (
        <main className={abaAtiva === "descobrir" ? "recommendations-theme discover-theme" : "recommendations-theme"}>
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

                    <div className="app-header-actions"><NotificationBell />

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
