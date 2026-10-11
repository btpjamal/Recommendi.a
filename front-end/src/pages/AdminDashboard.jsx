
import { Link } from "react-router";

export default function AdminDashboard() {
    const secoes = [
        {
            titulo: "Sugestões de hobbies",
            descricao: "Analise e aprove novos hobbies enviados pela comunidade.",
            icone: "☑",
            caminho: "/admin/suggestions"
        },
        {
            titulo: "Catálogo de hobbies",
            descricao: "Consulte os hobbies disponíveis no sistema.",
            icone: "◈",
            caminho: "/admin/hobbies"
        },
        {
            titulo: "Regras geográficas",
            descricao: "Configure as regras OSM utilizadas na busca de locais.",
            icone: "⌖",
            caminho: "/admin/location-rules"
        }
    ];

    return (
        <div className="admin-dashboard">
            <div className="admin-page-heading">
                <h1>Visão geral</h1>
                <p>
                    Gerencie o catálogo de hobbies e as funcionalidades
                    administrativas do Recommendi.a.
                </p>
            </div>

            <div className="admin-card-grid">
                {secoes.map((secao) => (
                    <Link
                        key={secao.caminho}
                        to={secao.caminho}
                        className="admin-card"
                    >
                        <span className="admin-card-icon">
                            {secao.icone}
                        </span>

                        <h2>{secao.titulo}</h2>
                        <p>{secao.descricao}</p>

                        <span className="admin-card-link">
                            Acessar seção →
                        </span>
                    </Link>
                ))}
            </div>
        </div>
    );
}
