import { Navigate, Outlet } from "react-router";
import { session } from "../auth/session.js";

import { SocialNotificationsProvider } from "./social/SocialNotifications.jsx";

export default function ProtectedRoute() {
    return session.getItem("token") ? <SocialNotificationsProvider key={session.getItem("token")}><Outlet /></SocialNotificationsProvider> : <Navigate to="/login" replace />;
}
