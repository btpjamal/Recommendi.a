import { useEffect, useRef, useState } from "react";
import { api } from "../api/api.js";
import { createChatSession } from "../components/community/communityChatState.mjs";
import "./css/AdminCommunityReports.css";

const reasons = { SPAM: "Spam", OFENSA: "Ofensa ou assédio", CONTEUDO_INADEQUADO: "Conteúdo inadequado", OUTRO: "Outro" };

function ReportImage({ id, evidence = false }) {
    const [image, setImage] = useState(null);
    const [failed, setFailed] = useState(false);
    useEffect(() => {
        const controller = new AbortController(); let url;
        api.get(`/admin/community-reports/${id}${evidence ? "/evidence" : ""}/image`, { signal: controller.signal, responseType: "blob" }).then(({ data }) => {
            if (controller.signal.aborted) return;
            url = URL.createObjectURL(data); setImage(url);
        }).catch(() => { if (!controller.signal.aborted) setFailed(true); });
        return () => { controller.abort(); if (url) URL.revokeObjectURL(url); };
    }, [id, evidence]);
    if (failed) return <p role="alert">Imagem indisponível ou acesso revogado.</p>;
    return image ? <img src={image} alt="Imagem da publicação denunciada" className="moderation-image" /> : <p role="status">Carregando imagem...</p>;
}

function ReportCard({ report, busy, onReview }) {
    const [decision, setDecision] = useState("MANTER");
    const [notes, setNotes] = useState("");
    const [showImage, setShowImage] = useState(false);
    const [showEvidenceImage, setShowEvidenceImage] = useState(false);
    const evidence = report.evidencia;
    return <article className="moderation-card">
        <header><div><span>DENÚNCIA #{report.id} · {report.commentId == null ? "PUBLICAÇÃO" : "COMENTÁRIO"}</span>
            <h2>{report.comunidade}</h2><time dateTime={report.criadoEm}>{new Date(report.criadoEm).toLocaleString("pt-BR")}</time></div>
            <strong>{report.status === "ABERTA" ? "Em análise" : "Analisada"}</strong></header>
        <p><strong>Motivo:</strong> {reasons[report.motivo]}</p>
        {report.detalhes && <p className="moderation-text">{report.detalhes}</p>}
        {report.versaoDenunciada !== report.versaoConteudo && <p className="moderation-warning">O conteúdo mudou desde a denúncia. Confira a versão atual antes de decidir.</p>}
        <section className="moderation-context">
            <h3>Registro da denúncia</h3>
            {evidence ? <>
                <p className="moderation-text">{evidence.publicacao || "Publicação sem texto."}</p>
                {evidence.comentario != null && <><h4>Comentário denunciado</h4>
                    <p className="moderation-text">{evidence.comentario}</p></>}
                {evidence.possuiImagem && <>
                    <button type="button" onClick={() => setShowEvidenceImage(v => !v)}>
                        {showEvidenceImage ? "Fechar imagem registrada" : "Ver imagem registrada"}
                    </button>
                    {showEvidenceImage && <ReportImage id={report.id} evidence />}
                </>}
            </> : <p>O registro original expirou ou não foi coletado.</p>}
        </section>
        {report.excluido && <p>O conteúdo foi excluído pelo autor. Você pode encerrar mantendo o estado atual.</p>}
        {report.oculto && <p>Este conteúdo ou sua publicação já está oculto. Manter não restaura a visibilidade.</p>}
        <section className="moderation-context"><h3>Publicação atual</h3><p className="moderation-text">{report.publicacao || "Sem texto disponível."}</p>
            {report.possuiImagem && <><button type="button" onClick={() => setShowImage(v => !v)}>{showImage ? "Fechar imagem" : "Ver imagem da publicação"}</button>
                {showImage && <ReportImage id={report.id} />}</>}
            {report.commentId != null && <><h3>Comentário atual</h3><p className="moderation-text">{report.comentario || "Sem texto disponível."}</p></>}
        </section>
        {report.status === "ABERTA" ? <form onSubmit={e => { e.preventDefault(); onReview(report, { decisao: decision, notas: notes, versaoDenuncia: report.versaoDenuncia, versaoConteudo: report.versaoConteudo }); }}>
            <label>Decisão<select value={decision} disabled={busy} onChange={e => setDecision(e.target.value)}>
                <option value="MANTER">Manter estado atual e encerrar denúncia</option>
                <option value="OCULTAR" disabled={report.excluido}>Ocultar o conteúdo denunciado</option>
            </select></label>
            <label>Justificativa<textarea rows={3} maxLength={1000} required value={notes} disabled={busy} onChange={e => setNotes(e.target.value)} /></label>
            <small>{notes.length}/1000</small><button className="moderation-primary" disabled={busy || !notes.trim()}>{busy ? "Salvando..." : "Registrar decisão"}</button>
        </form> : <section><p><strong>Decisão:</strong> {report.decisao === "OCULTAR" ? "Ocultar conteúdo" : "Manter estado atual"}</p>
            <p className="moderation-text">{report.notas}</p><p>Analisada em {new Date(report.analisadoEm).toLocaleString("pt-BR")}</p></section>}
    </article>;
}

export default function AdminCommunityReports() {
    const [status, setStatus] = useState("ABERTA");
    const [rows, setRows] = useState([]);
    const [cursor, setCursor] = useState(null);
    const [more, setMore] = useState(false);
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const [revision, setRevision] = useState(0);
    const current = useRef(null);
    const occupied = useRef(false);
    useEffect(() => {
        const session = createChatSession(); current.current = session; occupied.current = false;
        async function load() {
            setLoading(true); setRows([]); setError(""); setBusy(false); setMore(false);
            try {
                const { data } = await api.get("/admin/community-reports", { params: { status, limite: 20 }, signal: session.signal });
                session.commit(() => { setRows(data.denuncias); setMore(data.temMais); setCursor(data.proximoCursor); });
            } catch (e) { session.commit(() => setError(e.response?.data?.error || "Não foi possível carregar as denúncias.")); }
            finally { session.commit(() => setLoading(false)); }
        }
        load(); return () => session.close();
    }, [status, revision]);
    async function review(report, payload) {
        const session = current.current; if (!session?.isActive() || occupied.current) return;
        if (!window.confirm(payload.decisao === "OCULTAR" ? "Ocultar este conteúdo para todos os participantes e registrar a decisão?" : "Encerrar esta denúncia mantendo o estado atual do conteúdo?")) return;
        occupied.current = true; setBusy(true); setError(""); setNotice("");
        try {
            const { data } = await api.put(`/admin/community-reports/${report.id}/review`, payload, { signal: session.signal });
            session.commit(() => { setRows(old => old.filter(r => r.id !== data.id)); setNotice("Decisão registrada."); });
        } catch (e) { session.commit(() => setError(e.response?.data?.error || "Não foi possível registrar a decisão. Atualize a lista se o conteúdo mudou.")); }
        finally { session.commit(() => { occupied.current = false; setBusy(false); }); }
    }
    async function loadMore() {
        const session = current.current; if (!session?.isActive() || occupied.current || !more || !cursor) return;
        occupied.current = true; setBusy(true); setError("");
        try {
            const { data } = await api.get("/admin/community-reports", { params: { status, antesDe: cursor, limite: 20 }, signal: session.signal });
            session.commit(() => { setRows(old => [...new Map([...old, ...data.denuncias].map(r => [r.id, r])).values()]); setMore(data.temMais); setCursor(data.proximoCursor); });
        } catch (e) { session.commit(() => setError(e.response?.data?.error || "Não foi possível carregar mais denúncias.")); }
        finally { session.commit(() => { occupied.current = false; setBusy(false); }); }
    }
    return <section className="moderation-page"><h1>Denúncias da comunidade</h1><p>Analise publicações e comentários. As decisões e justificativas ficam registradas.</p>
        <div className="moderation-filters"><label>Status<select value={status} disabled={busy} onChange={e => setStatus(e.target.value)}><option value="ABERTA">Abertas</option><option value="ANALISADA">Analisadas</option></select></label>
            <button type="button" disabled={busy || loading} onClick={() => setRevision(v => v + 1)}>Atualizar lista</button></div>
        {error && <p role="alert" className="interaction-error">{error}</p>}{notice && <p role="status">{notice}</p>}
        {loading ? <p role="status">Carregando denúncias...</p> : rows.length === 0 ? <p>Nenhuma denúncia neste filtro.</p>
            : rows.map(report => <ReportCard key={`${report.id}-${report.versaoDenuncia}-${report.versaoConteudo}`} report={report} busy={busy} onReview={review} />)}
        {more && <button type="button" disabled={busy} onClick={loadMore}>Carregar denúncias anteriores</button>}
    </section>;
}
