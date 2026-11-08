import { useState } from "react";
import "./RecommendationImage.css";

export default function RecommendationImage({ imagemUrl, nome }) {
    const src = typeof imagemUrl === "string" ? imagemUrl.trim() : "";
    const [urlComFalha, setUrlComFalha] = useState(null);
    const disponivel = src && src !== urlComFalha;

    return (
        <div className="recommendation-hobby-image">
            {disponivel ? (
                <img
                    src={src}
                    alt={`Imagem de ${nome}`}
                    loading="lazy"
                    decoding="async"
                    width="640"
                    height="400"
                    onError={() => setUrlComFalha(src)}
                />
            ) : (
                <div className="recommendation-hobby-image-placeholder">
                    <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true" focusable="false">
                        <rect x="3" y="3" width="18" height="18" rx="3" />
                        <circle cx="8" cy="8" r="1.5" />
                        <path d="m3 17 5-5 4 4 4-6 5 7" />
                    </svg>
                    <span>{src ? "Imagem indisponível" : "Hobby sem imagem"}</span>
                </div>
            )}
        </div>
    );
}
