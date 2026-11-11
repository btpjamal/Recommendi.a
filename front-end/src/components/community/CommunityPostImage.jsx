import { useEffect, useState } from "react";
import { api } from "../../api/api.js";

export default function CommunityPostImage({ communityId, post, onAccessLost }) {
    const [image, setImage] = useState(null);
    const [failed, setFailed] = useState(false);
    const [retry, setRetry] = useState(0);
    useEffect(() => {
        const controller = new AbortController();
        let url;
        api.get(`/community/communities/${communityId}/posts/${post.id}/image`, {
            responseType: "blob", signal: controller.signal
        }).then(({ data }) => {
            if (controller.signal.aborted) return;
            url = URL.createObjectURL(data); setImage({ url, id: post.id, version: post.versao }); setFailed(false);
        }).catch(error => {
            if (controller.signal.aborted) return;
            setImage(null); setFailed(true);
            if ([401, 403].includes(error.response?.status)) onAccessLost();
        });
        return () => { controller.abort(); if (url) URL.revokeObjectURL(url); };
    }, [communityId, post.id, post.versao, retry, onAccessLost]);
    if (failed) return <div className="community-post-image-error"><p>Imagem indisponível. A publicação pode ter mudado.</p>
        <button type="button" onClick={() => { setFailed(false); setRetry(v => v + 1); }}>Tentar carregar imagem</button></div>;
    return image?.id === post.id && image.version === post.versao
        ? <img className="community-post-image" src={image.url} alt={`Imagem da publicação de ${post.autorNome}`} loading="lazy" />
        : <p role="status">Carregando imagem...</p>;
}
