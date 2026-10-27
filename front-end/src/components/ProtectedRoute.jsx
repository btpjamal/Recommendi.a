import { Navigate, Outlet } from "react-router";
import { session } from "../auth/session.js";

export default function ProtectedRoute() {
    return session.getItem("token") ? <Outlet /> : <Navigate to="/login" replace />;
}
