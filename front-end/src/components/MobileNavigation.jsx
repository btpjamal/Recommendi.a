import { useContext, useSyncExternalStore } from "react";
import { Link, Outlet, useLocation, useNavigate } from "react-router";
import { revokeSession } from "../auth/session.js";
import { AtmosphereContext } from "./recommendations/atmosphereContext.js";
import NotificationBell from "./social/SocialNotifications.jsx";
import usePrivateUnread from "./social/usePrivateUnread.js";
import "./MobileNavigation.css";

const mobileQuery = "(max-width: 600px)";

function subscribe(listener) {
    const media = window.matchMedia(mobileQuery);
    media.addEventListener("change", listener);
    return () => media.removeEventListener("change", listener);
}

const destinations = [
    { id: "discover", label: "Descobrir", to: "/recommendations", icon: "M12 3l2.4 6.6L21 12l-6.6 2.4L12 21l-2.4-6.6L3 12l6.6-2.4L12 3Z" },
    { id: "feed", label: "Feed", to: "/community?aba=feed", icon: "M5 5h14v14H5zM8 9h8M8 13h8M8 16h5" },
    { id: "community", label: "Comunidade", to: "/community", icon: "M16 20v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M16 4a4 4 0 0 1 0 8m6 8v-2a4 4 0 0 0-3-3.9M9 4a4 4 0 1 1 0 8 4 4 0 0 1 0-8Z" },
    { id: "chat", label: "Conversas", to: "/community?aba=conversas", icon: "M21 11.5a8.5 8.5 0 0 1-8.5 8.5 9 9 0 0 1-4-.9L3 21l1.9-5.5a9 9 0 0 1-.9-4A8.5 8.5 0 0 1 12.5 3h.5a8.5 8.5 0 0 1 8 8v.5Z" },
    { id: "profile", label: "Perfil", to: "/myProfile", icon: "M4 21v-2a4 4 0 0 1 4-4h8a4 4 0 0 1 4 4v2M12 3a4 4 0 1 1 0 8 4 4 0 0 1 0-8Z" },
];

function MobileHeader() {
    const navigate = useNavigate();
    const { paused, toggleLights } = useContext(AtmosphereContext);

    async function sair() {
        await revokeSession();
        navigate("/login", { replace: true });
    }

    return (
        <header className="mobile-app-header">
            <Link className="mobile-app-brand" to="/recommendations">
                <span aria-hidden="true">✦</span> Recommendi.a
            </Link>
            <NotificationBell />
            <details className="mobile-account">
                <summary aria-label="Menu da conta">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16" /></svg>
                </summary>
                <div className="mobile-account-menu">
                    <Link to="/myProfile">Meu perfil</Link>

                    <Link to="/my-suggestions">Minhas sugestões</Link>
                    <Link to="/onboarding?editar=true">Atualizar preferências</Link>
                    <button type="button" className="mobile-lights-control" onClick={toggleLights}>
                        {paused ? "Retomar luzes" : "Pausar luzes"}
                    </button>
                    <button type="button" onClick={sair}>Sair</button>
                </div>
            </details>
        </header>
    );
}

export default function MobileNavigation() {
    const location = useLocation();
    const mobile = useSyncExternalStore(subscribe, () => window.matchMedia(mobileQuery).matches);
    const path = location.pathname.toLowerCase();
    const privateChat = path.startsWith("/community/conversations/");
    const showNavigation = mobile && !privateChat && path !== "/onboarding" && path !== "/profile";
    const showHeader = showNavigation && (path.startsWith("/community/communities/") || ["/recommendations", "/community", "/myprofile", "/my-suggestions", "/suggest-hobby"].includes(path));
    const unread = usePrivateUnread(showNavigation, location.key);
    const aba = new URLSearchParams(location.search).get("aba");
    let active = "discover";
    if (path === "/community") {
        active = aba === "feed" ? "feed" : aba === "conversas" ? "chat" : "community";
    } else if (path.startsWith("/community/")) {
        active = "community";
    } else if (["/myprofile", "/ai-settings", "/my-suggestions", "/suggest-hobby"].includes(path)) {
        active = "profile";
    }

    return (
        <div className={showNavigation ? "mobile-navigation-layout" : undefined} data-mobile-destination={showNavigation ? active : undefined}>
            {showHeader && <MobileHeader key={location.key} />}
            <div className={showHeader ? (active === "feed" || active === "chat" ? "mobile-primary-content mobile-social-destination" : "mobile-primary-content") : undefined}>
                <Outlet context={{ mobile, unread }} />
            </div>
            {showNavigation && (
                <nav className="mobile-bottom-nav" aria-label="Navegação principal">
                    {destinations.map(({ id, label, to, icon }) => (
                        <Link key={id} to={to} aria-current={active === id ? "page" : undefined}>
                            <span className="mobile-nav-icon">
                                <svg viewBox="0 0 24 24" aria-hidden="true"><path d={icon} /></svg>
                                {id === "chat" && unread.total > 0 && (
                                    <span className="mobile-nav-badge" aria-label={`${unread.total} mensagens não lidas`}>
                                        {unread.total > 99 ? "99+" : unread.total}
                                    </span>
                                )}
                            </span>
                            <span>{label}</span>
                        </Link>
                    ))}
                </nav>
            )}
        </div>
    );
}
