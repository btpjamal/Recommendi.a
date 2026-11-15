import { useEffect, useId, useRef, useState } from "react";
import axios from "axios";
import { API_BASE_URL } from "../../api/config.js";
import { session } from "../../auth/session.js";
import "./SocialAvatar.css";

function ExpandablePhoto({ src, nome, thumbnail }) {
    const dialog = useRef(null);
    const titleId = useId();

    return (
        <>
            <button type="button" className="social-user-avatar social-avatar-trigger"
                aria-label={`Ampliar foto de ${nome}`} aria-haspopup="dialog"
                onClick={() => dialog.current.showModal()}>
                {thumbnail}
            </button>
            <dialog ref={dialog} className="social-avatar-dialog" aria-labelledby={titleId}
                onCancel={event => event.stopPropagation()}>
                <header>
                    <h2 id={titleId}>Foto de {nome}</h2>
                    <button type="button" className="community-secondary-button"
                        onClick={() => dialog.current.close()}>Fechar</button>
                </header>
                <img src={src} alt={`Foto de ${nome}`} />
            </dialog>
        </>
    );
}

export default function SocialAvatar({ usuario, revision = 0, ampliavel = false }) {
    const [image, setImage] = useState(null);
    const token = session.getItem("token");
    useEffect(() => {
        if (!usuario?.possuiFoto) return;
        const controller = new AbortController();
        let url;
        axios.get(`${API_BASE_URL}/social/profiles/${usuario.userId}/photo`, {
            headers: { Authorization: `Bearer ${token}` }, responseType: "blob", signal: controller.signal
        }).then(({ data }) => {
            if (controller.signal.aborted) return;
            url = URL.createObjectURL(data); setImage({ url, id: usuario.userId, revision });
        }).catch(() => { if (!controller.signal.aborted) setImage(null); });
        return () => { controller.abort(); if (url) URL.revokeObjectURL(url); };
    }, [usuario?.userId, usuario?.possuiFoto, revision, token]);
    const src = usuario?.possuiFoto && image?.id === usuario.userId && image.revision === revision
        ? image.url : null;
    const thumbnail = src && <img src={src} alt={`Foto de ${usuario.nome}`}
        style={{ width: "100%", height: "100%", objectFit: "cover", borderRadius: "inherit" }} />;

    if (ampliavel && src) {
        return <ExpandablePhoto key={src} src={src} nome={usuario.nome} thumbnail={thumbnail} />;
    }

    return <div className="social-user-avatar">{thumbnail || usuario?.nome?.charAt(0)?.toUpperCase()}</div>;
}
