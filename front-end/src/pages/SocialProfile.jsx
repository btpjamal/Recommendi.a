import SocialFollowButton from "../components/social/SocialFollowButton.jsx";
import { useLayoutEffect, useRef, useState, useSyncExternalStore } from "react";
import { useNavigate, useParams } from "react-router";
import axios from "axios";
import { API_BASE_URL } from "../api/config.js";
import { isCurrentSession, sessionSnapshot, subscribeSessionToken } from "../auth/session.js";
import SocialAvatar from "../components/social/SocialAvatar.jsx";
import { invalidarMiniaturas } from "../components/social/socialAvatarRequest.js";
import SocialContactRequestButton from "../components/social/SocialContactRequestButton.jsx";
import "./css/Community.css";
import "./css/SocialProfile.css";

const fields = [["fotoVisibilidade", "Foto"], ["biografiaVisibilidade", "Biografia"],
    ["interessesVisibilidade", "Interesses"], ["hobbiesVisibilidade", "Hobbies"]];
const options = [["PARTICIPANTES", "Participantes"], ["CONTATOS", "Contatos aceitos"], ["SOMENTE_EU", "Somente eu"]];
const errorText = (error) => error.response?.data?.detail || error.response?.data?.error || "Não foi possível concluir. Tente novamente.";

export default function SocialProfile({ embedded = false }) {
    const { userId } = useParams();
    const generation = useSyncExternalStore(subscribeSessionToken, getSessionGeneration);
    return <SocialProfileContent key={JSON.stringify([generation, userId])}
        generation={generation} userId={userId} embedded={embedded} />;
}

function getSessionGeneration() {
    return sessionSnapshot().generation;
}

function SocialProfileContent({ generation, userId, embedded }) {
    const own = !userId;
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [form, setForm] = useState(null);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const [busy, setBusy] = useState(false);
    const [revision, setRevision] = useState(0);
    const [loaded, setLoaded] = useState(false);
    const [participa, setParticipa] = useState(false);
    const lifetime = useRef(null);
    const mutation = useRef(null);
    const { token } = sessionSnapshot();

    useLayoutEffect(() => {
        const controller = new AbortController();
        lifetime.current = controller;
        const active = () => !controller.signal.aborted && isCurrentSession(generation);
        const config = { headers: { Authorization: `Bearer ${token}` }, signal: controller.signal };

        async function load() {
            try {
                const [p, f] = await Promise.all([
                    axios.get(API_BASE_URL + (own ? "/social/me/profile/preview" : `/community/users/${userId}`), config),
                    own ? axios.get(API_BASE_URL + "/social/me/profile", config) : Promise.resolve(null)
                ]);
                if (!active()) return;
                setProfile(p.data);
                setForm(f?.data);
            } catch (e) {
                if (active() && !axios.isCancel(e)) setError(errorText(e));
            } finally {
                if (active()) setLoaded(true);
            }
        }

        load();
        if (own) {
            axios.get(API_BASE_URL + "/social/me/status", config).then(({ data }) => {
                if (active()) setParticipa(data.participa);
            }).catch(e => {
                if (active() && !axios.isCancel(e)) setError(errorText(e));
            });
        }

        return () => {
            lifetime.current = null;
            mutation.current = null;
            controller.abort();
        };
    }, [generation, own, token, userId]);

    async function mutate(action) {
        const controller = lifetime.current;
        if (!controller || mutation.current || !isCurrentSession(generation)) return;

        const operation = {};
        mutation.current = operation;
        const active = () => mutation.current === operation && !controller.signal.aborted &&
            isCurrentSession(generation);
        const config = { headers: { Authorization: `Bearer ${token}` }, signal: controller.signal };

        setBusy(true);
        setError("");
        setMessage("");
        try {
            await action(config, active);
        } catch (e) {
            if (active() && !axios.isCancel(e)) setError(errorText(e));
        } finally {
            if (active()) {
                mutation.current = null;
                setBusy(false);
            }
        }
    }

    function sairDaComunidade() {
        if (mutation.current || !window.confirm("Deseja sair da comunidade? Seu perfil deixará de aparecer para outros participantes.")) return;
        return mutate(async (config, active) => {
            await axios.post(API_BASE_URL + "/social/me/disable", {}, config);
            if (!active()) return;
            window.dispatchEvent(new Event("social-participation-changed"));
            if (active()) navigate("/community", { replace: true });
        });
    }

    async function refresh(config, active, successMessage) {
        const { data } = await axios.get(API_BASE_URL + "/social/me/profile/preview", config);
        if (!active()) return;
        setProfile(data);
        setRevision(v => v + 1);
        setMessage(successMessage);
    }

    function save(event) {
        event.preventDefault();
        return mutate(async (config, active) => {
            const { data } = await axios.put(API_BASE_URL + "/social/me/profile", form, config);
            if (!active()) return;
            setForm(data);
            invalidarMiniaturas();
            if (active()) await refresh(config, active, "Perfil e visibilidade salvos.");
        });
    }

    function photo(file, remove = false) {
        if (mutation.current || (!file && !remove)) return;
        if (file && (file.size > 1024 * 1024 || !["image/png", "image/jpeg"].includes(file.type))) {
            setError("Use uma foto PNG ou JPEG de até 1 MB.");
            return;
        }
        return mutate(async (config, active) => {
            if (remove) {
                await axios.delete(API_BASE_URL + "/social/me/profile/photo", config);
            } else {
                const body = new FormData();
                body.append("file", file);
                await axios.put(API_BASE_URL + "/social/me/profile/photo", body, config);
            }
            if (!active()) return;
            invalidarMiniaturas();
            if (active()) await refresh(config, active, remove ? "Foto removida." : "Foto atualizada.");
        });
    }
    return <main className="community-page"><section className="social-profile-page">
        {!embedded && <button className="community-secondary-button" onClick={() => navigate("/community")}>Voltar ao Hub Social</button>}
        <h1>{own ? "Meu perfil social" : "Perfil social"}</h1>
        {error && <p role="alert" className="community-error">{error}</p>}
        {message && <p role="status">{message}</p>}
        {!loaded && <p role="status">Carregando perfil...</p>}
        {profile && <div className="social-profile-columns">
            {own && form && <form onSubmit={save} className="social-profile-editor">
                <h2>Apresente-se à comunidade</h2>
                <label>Foto de perfil<input type="file" accept="image/png,image/jpeg" disabled={busy}
                    onChange={e => { photo(e.target.files[0]); e.target.value = ""; }} /></label>
                <p>PNG ou JPEG, até 1 MB e 16 megapixels. A foto é salva ao enviar.</p>
                {profile.possuiFoto && <button type="button" disabled={busy} onClick={() => photo(null, true)}>Remover foto</button>}
                <label>Biografia<textarea rows={5} maxLength={500} value={form.biografia || ""} disabled={busy}
                    onChange={e => setForm({ ...form, biografia: e.target.value })} /></label>
                <small>{form.biografia?.length || 0}/500 caracteres</small>
                <fieldset disabled={busy}><legend>Quem pode ver</legend>
                    {fields.map(([key, label]) => <label key={key}>{label}<select value={form[key]}
                        onChange={e => setForm({ ...form, [key]: e.target.value })}>
                        {options.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
                    </select></label>)}
                </fieldset>
                <p>Participantes são pessoas com acesso ao Hub Social. Hobbies e interesses vêm das suas atividades no Recommendi.a. Campos ocultos também saem do resumo e dos motivos de afinidade.</p>
                <button className="community-primary-button" disabled={busy}>{busy ? "Salvando..." : "Salvar perfil e visibilidade"}</button>
            </form>}
            <article className="social-user-card">
                {own && <><h2>Prévia dos dados salvos</h2><p>Esta é a sua visão completa. Outras pessoas veem os campos conforme a visibilidade escolhida.</p></>}
                <SocialAvatar usuario={profile} revision={revision} ampliavel={own} />
                <h2>{profile.nome}</h2>
                {profile.biografia && <p className="social-profile-bio">{profile.biografia}</p>}
                <p>{profile.resumo}</p>
                {profile.proximidade && <p>{profile.proximidade === "MESMA_CIDADE" ? "Da mesma cidade que você" : "Do mesmo estado que você"}</p>}
                {[["Interesses", profile.interesses], ["Praticando", profile.praticando?.map(h => h.nome)],
                    ["Quero experimentar", profile.interessados?.map(h => h.nome)]].map(([title, items]) => items?.length > 0 &&
                        <section key={title}><h3>{title}</h3><div className="social-affinity-tags">{items.map(item => <span key={item}>{item}</span>)}</div></section>)}
                {!own && <SocialFollowButton userId={profile.userId} />}
                {!own && <SocialContactRequestButton usuario={profile} onSolicitada={() => navigate("/community?aba=conversas")}
                    onBloqueado={() => navigate("/community?aba=conversas&caixa=BLOQUEADOS")} />}
            </article>
        </div>}
        {own && participa && (
            <section className="social-profile-editor social-profile-participation">
                <h2>Participação na comunidade</h2>
                <button type="button" className="community-leave-button" disabled={busy} onClick={sairDaComunidade}>
                    Sair da comunidade
                </button>
            </section>
        )}
    </section></main>;
}

