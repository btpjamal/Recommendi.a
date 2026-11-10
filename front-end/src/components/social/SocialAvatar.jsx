import { useEffect, useState } from "react";
import axios from "axios";
import { API_BASE_URL } from "../../api/config.js";
import { session } from "../../auth/session.js";

export default function SocialAvatar({ usuario, revision = 0 }) {
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
    return <div className="social-user-avatar">
        {usuario?.possuiFoto && image?.id === usuario.userId && image.revision === revision
            ? <img src={image.url} alt={`Foto de ${usuario.nome}`} style={{ width: "100%", height: "100%", objectFit: "cover", borderRadius: "inherit" }} />
            : usuario?.nome?.charAt(0)?.toUpperCase()}
    </div>;
}
