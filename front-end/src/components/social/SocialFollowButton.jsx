import { useEffect, useRef, useState } from "react";
import { api } from "../../api/api.js";

export default function SocialFollowButton({ userId, initialFollowing = null }) {
    const [following, setFollowing] = useState(initialFollowing);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [revision, setRevision] = useState(0);
    const current = useRef(null);
    const occupied = useRef(false);
    const base = `/community/users/${userId}/follow`;

    useEffect(() => {
        const controller = new AbortController();
        current.current = controller;
        occupied.current = false;
        if (initialFollowing !== null) return () => controller.abort();
        api.get(base, { signal: controller.signal }).then(({ data }) => {
            if (!controller.signal.aborted) setFollowing(data.seguindo);
        }).catch(e => {
            if (!controller.signal.aborted) setError(e.response?.data?.error || "Não foi possível consultar o acompanhamento.");
        });
        return () => controller.abort();
    }, [base, initialFollowing, revision]);

    async function toggle() {
        const controller = current.current;
        if (!controller || controller.signal.aborted || occupied.current || following === null) return;
        occupied.current = true;
        setBusy(true);
        setError("");
        try {
            const { data } = following
                ? await api.delete(base, { signal: controller.signal })
                : await api.put(base, null, { signal: controller.signal });
            if (!controller.signal.aborted) {
                setFollowing(data.seguindo);
                window.dispatchEvent(new Event("social-follow-changed"));
            }
        } catch (e) {
            if (!controller.signal.aborted) setError(e.response?.data?.error || "Não foi possível salvar. Você pode tentar novamente.");
        } finally {
            if (!controller.signal.aborted) {
                occupied.current = false;
                setBusy(false);
            }
        }
    }

    return <div className="social-follow-control">
        <button className="community-secondary-button" type="button" disabled={busy || following === null}
            aria-pressed={following === true} onClick={toggle}>
            {busy ? "Salvando..." : following === null ? "Carregando..." : following ? "Deixar de seguir" : "Seguir"}
        </button>
        {error && <p role="alert">{error}</p>}
        {error && following === null && <button type="button" onClick={() => { setError(""); setRevision(v => v + 1); }}>Tentar novamente</button>}
    </div>;
}
