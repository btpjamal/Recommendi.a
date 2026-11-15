import { Navigate } from "react-router";
import { session } from "../auth/session.js";

import { SocialNotificationsProvider } from "./social/SocialNotifications.jsx";
import MobileNavigation from "./MobileNavigation.jsx";

export default function ProtectedRoute() {
    const token = session.getItem("token");
    if (!token) return <Navigate to="/login" replace />;

    return (
        <SocialNotificationsProvider key={token}>
            <MobileNavigation />
        </SocialNotificationsProvider>
    );
}
