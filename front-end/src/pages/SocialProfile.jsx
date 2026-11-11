import SocialFollowButton from "../components/social/SocialFollowButton.jsx";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import axios from "axios";
import { API_BASE_URL } from "../api/config.js";
import { session } from "../auth/session.js";
import SocialAvatar from "../components/social/SocialAvatar.jsx";
import SocialContactRequestButton from "../components/social/SocialContactRequestButton.jsx";
import "./css/Community.css";
import "./css/SocialProfile.css";

const fields = [["fotoVisibilidade", "Foto"], ["biografiaVisibilidade", "Biografia"],
    ["interessesVisibilidade", "Interesses"], ["hobbiesVisibilidade", "Hobbies"]];
const options = [["PARTICIPANTES", "Participantes"], ["CONTATOS", "Contatos aceitos"], ["SOMENTE_EU", "Somente eu"]];
const errorText = (error) => error.response?.data?.detail || error.response?.data?.error || "Não foi possível concluir. Tente novamente.";

export default function SocialProfile() {
    const { userId } = useParams();
    return <SocialProfileContent key={userId || "me"} userId={userId} />;
}

function SocialProfileContent({ userId }) {
    const own = !userId;
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [form, setForm] = useState(null);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const [busy, setBusy] = useState(false);
    const [revision, setRevision] = useState(0);
    const [loaded, setLoaded] = useState(false);
    const token = session.getItem("token");
    const headers = { Authorization: `Bearer ${token}` };

    useEffect(() => {
        const controller = new AbortController();
        async function load() {
            try {
                const config = { headers: { Authorization: `Bearer ${token}` }, signal: controller.signal };
                const [p, f] = await Promise.all([
                    axios.get(API_BASE_URL + (own ? "/social/me/profile/preview" : `/community/users/${userId}`), config),
                    own ? axios.get(API_BASE_URL + "/social/me/profile", config) : Promise.resolve(null)
                ]);
                if (!controller.signal.aborted) { setProfile(p.data); setForm(f?.data); }
            } catch (e) { if (!controller.signal.aborted) setError(errorText(e)); }
            finally { if (!controller.signal.aborted) setLoaded(true); }
        }
        load(); return () => controller.abort();
    }, [own, userId, token]);

    async function refresh() {
        const { data } = await axios.get(API_BASE_URL + "/social/me/profile/preview", { headers });
        setProfile(data); setRevision(v => v + 1);
    }
    async function save(event) {
        event.preventDefault(); if (busy) return;
        setBusy(true); setError(""); setMessage("");
        try {
            const { data } = await axios.put(API_BASE_URL + "/social/me/profile", form, { headers });
            setForm(data); await refresh(); setMessage("Perfil e visibilidade salvos.");
        } catch (e) { setError(errorText(e)); } finally { setBusy(false); }
    }
    async function photo(file, remove = false) {
        if (busy || (!file && !remove)) return;
        if (file && (file.size > 1024 * 1024 || !["image/png", "image/jpeg"].includes(file.type))) {
            setError("Use uma foto PNG ou JPEG de até 1 MB."); return;
        }
        setBusy(true); setError(""); setMessage("");
        try {
            if (remove) await axios.delete(API_BASE_URL + "/social/me/profile/photo", { headers });
            else {
                const body = new FormData(); body.append("file", file);
                await axios.put(API_BASE_URL + "/social/me/profile/photo", body, { headers });
            }
            await refresh(); setMessage(remove ? "Foto removida." : "Foto atualizada.");
        } catch (e) { setError(errorText(e)); } finally { setBusy(false); }
    }
    return <main className="community-page"><section className="social-profile-page">
        <button className="community-secondary-button" onClick={() => navigate("/community")}>Voltar ao Hub Social</button>
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
                <SocialAvatar usuario={profile} revision={revision} />
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
    </section></main>;
}

