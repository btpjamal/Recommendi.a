import { useRef, useState } from "react";
import { api } from "../../api/api";
import "./SocialContacts.css";

export default function SocialContactRequestButton({ usuario, onSolicitada, onBloqueado }) {
    const [aberto, setAberto] = useState(false);
    const [apresentacao, setApresentacao] = useState("");
    const [processando, setProcessando] = useState(false);
    const [erro, setErro] = useState("");
    const ocupado = useRef(false);

    async function executar(bloquear, event) {
        event?.preventDefault();
        if (ocupado.current) return;
        ocupado.current = true;
        setProcessando(true);
        setErro("");
        try {
            if (bloquear) {
                await api.post(`/community/contacts/blocks/${usuario.userId}`);
                onBloqueado();
            } else {
                const resposta = await api.post("/community/contacts/requests", {
                    destinatarioId: usuario.userId, apresentacao: apresentacao.trim()
                });
                onSolicitada(resposta.data);
            }
        } catch (error) {
            setErro(error.response?.data?.error || error.response?.data?.message || "Não foi possível concluir a operação.");
        } finally {
            ocupado.current = false;
            setProcessando(false);
        }
    }

    return (
        <div className="social-contact-actions-area">
            {!aberto ? <button type="button" className="social-contact-primary" onClick={() => setAberto(true)}>Solicitar conversa</button> : (
                <form className="social-contact-compose" onSubmit={(event) => executar(false, event)}>
                    <label htmlFor={`contato-apresentacao-${usuario.userId}`}>Apresentação para {usuario.nome} (opcional)</label>
                    <textarea id={`contato-apresentacao-${usuario.userId}`} value={apresentacao}
                        onChange={(event) => setApresentacao(event.target.value)} maxLength={300} rows={3} disabled={processando} />
                    <small>{apresentacao.length}/300 caracteres. A outra pessoa poderá aceitar, recusar ou bloquear o contato.</small>
                    <div className="social-contact-actions">
                        <button type="submit" className="social-contact-primary" disabled={processando}>{processando ? "Enviando..." : "Enviar solicitação"}</button>
                        <button type="button" disabled={processando} onClick={() => setAberto(false)}>Cancelar</button>
                    </div>
                </form>
            )}
            <button type="button" className="social-contact-block" disabled={processando} onClick={() => executar(true)}>Bloquear participante</button>
            {erro && <p className="social-contact-error" role="alert">{erro}</p>}
        </div>
    );
}
