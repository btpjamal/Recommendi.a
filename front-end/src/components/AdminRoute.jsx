
import { Navigate, Outlet } from "react-router";

export default function AdminRoute() {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");

    if (!token) {
        return <Navigate to="/login" replace />;
    }

    if (role !== "ADMIN") {
        return <Navigate to="/recommendations" replace />;
    }

    return <Outlet />;
}
