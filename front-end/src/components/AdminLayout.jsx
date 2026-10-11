
import { NavLink, Outlet, useNavigate } from "react-router";
import "../pages/css/Admin.css";

export default function AdminLayout() {
    const navigate = useNavigate();
    const nome = localStorage.getItem("nome") || "Administrador";

    const menu = [
        { caminho: "/admin", nome: "Visão geral", icone: "▦", fim: true },
        { caminho: "/admin/suggestions", nome: "Sugestões", icone: "☑" },
        { caminho: "/admin/hobbies", nome: "Catálogo de hobbies", icone: "◈" },
        { caminho: "/admin/location-rules", nome: "Regras geográficas", icone: "⌖" }
    ];

    function sair() {
        ["token", "userId", "nome", "email", "role"]
            .forEach((campo) => localStorage.removeItem(campo));

        navigate("/login", { replace: true });
    }

    return (
        <div className="admin-layout">
            <aside className="admin-sidebar">
                <div className="admin-brand">
                    <span className="admin-brand-icon">✦</span>

                    <div>
                        <strong>Recommendi.a</strong>
                        <small>Administração</small>
                    </div>
                </div>

                <nav className="admin-navigation" aria-label="Administração">
                    {menu.map((item) => (
                        <NavLink
                            key={item.caminho}
                            to={item.caminho}
                            end={item.fim}
                            className={({ isActive }) =>
                                `admin-nav-link ${isActive ? "active" : ""}`
                            }
                        >
                            <span aria-hidden="true">{item.icone}</span>
                            {item.nome}
                        </NavLink>
                    ))}
                </nav>

                <div className="admin-sidebar-footer">
                    <span>{nome}</span>

                    <button type="button" onClick={sair}>
                        Sair da conta
                    </button>
                </div>
            </aside>

            <div className="admin-main-area">
                <header className="admin-topbar">
                    <div>
                        <strong>Painel administrativo</strong>
                        <small>Gerenciamento do Recommendi.a</small>
                    </div>

                    <span className="admin-role-badge">ADMIN</span>
                </header>

                <main className="admin-content">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
