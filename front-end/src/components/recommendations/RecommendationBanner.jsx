export default function RecommendationBanner({ title, description, children }) {
    return (
        <header className="recommendations-header">
            <div>
                <h1 className="recommendations-title">{title}</h1>
                <p className="recommendations-subtitle">{description}</p>
            </div>
            {children}
        </header>
    );
}
