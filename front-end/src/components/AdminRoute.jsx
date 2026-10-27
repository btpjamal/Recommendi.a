import { session } from "../auth/session.js";

import { Navigate, Outlet } from "react-router";

export default function AdminRoute() {
    const token = session.getItem("token");
    const role = session.getItem("role");

    if (!token) {
        return <Navigate to="/login" replace />;
    }

    if (role !== "ADMIN") {
        return <Navigate to="/recommendations" replace />;
    }

    return <Outlet />;
}
