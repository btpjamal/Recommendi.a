export default function RecommendationBanner({ id, title, description, detail, children }) {
    return (
        <header className="recommendations-header">
            <div>
                <h1 id={id} className="recommendations-title">{title}</h1>
                <p className="recommendations-subtitle">{description}</p>
                {detail && <p className="recommendations-subtitle">{detail}</p>}
            </div>
            {children}
        </header>
    );
}
