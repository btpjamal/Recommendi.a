import NotificationBell from "../components/social/SocialNotifications.jsx";
import { session, revokeSession } from "../auth/session.js";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { api } from "../api/api";
import { createChatSession, mergeMessages } from "../components/community/communityChatState.mjs";
import "./css/CommunityDetails.css";
import "./css/RecommendationsLayout.css";
import { pendingSend, shouldMarkRead } from "../components/social/privateChatState.mjs";
import "../components/social/PrivateConversation.css";

export default function PrivateConversation() {
    const { contactId } = useParams();
    // Trocar de contato desmonta a sessao e limpa o rascunho.
    return <PrivateChat key={contactId} contactId={contactId} />;
}

function PrivateChat({ contactId }) {
    const navigate = useNavigate();
    const nome = session.getItem("nome");
    const [contato, setContato] = useState(null);
    const [mensagens, setMensagens] = useState([]);
    const [conteudo, setConteudo] = useState("");
    const [carregando, setCarregando] = useState(true);
    const [enviando, setEnviando] = useState(false);
    const [carregandoAnteriores, setCarregandoAnteriores] = useState(false);
    const [temAnteriores, setTemAnteriores] = useState(false);
    const [acesso, setAcesso] = useState(false);
    const [erro, setErro] = useState("");
    const [erroAtualizacao, setErroAtualizacao] = useState("");
    const [revisao, setRevisao] = useState(0);
    const [bloqueando, setBloqueando] = useState(false);
    const [visivel, setVisivel] = useState(() => !document.hidden);
    const [lendoFim, setLendoFim] = useState(true);
    const [ultimaLeitura, setUltimaLeitura] = useState(0);
    const [tentativaLeitura, setTentativaLeitura] = useState(0);
    const [avisoLeitura, setAvisoLeitura] = useState("");
    const envioPendenteRef = useRef(null);
    const leituraOcupadaRef = useRef(false);
    const erroLeituraEmRef = useRef(0);
    const sessaoRef = useRef(null);
    const atualizarRef = useRef(null);
    const cursorNovasRef = useRef(0);
    const cursorAnterioresRef = useRef(null);
    const envioOcupadoRef = useRef(false);
    const anterioresOcupadoRef = useRef(false);
    const listaRef = useRef(null);
    const seguirFinalRef = useRef(true);
    const ancoraRef = useRef(null);
    const base = `/community/private-messages/${contactId}`;

    function tratarFalha(error, sessao, mensagem) {
        if (!sessao.isActive()) return;
        const status = error.response?.status;
        if (status === 401 || status === 403 || status === 404) {
            sessao.commit(() => {
                setAcesso(false);
                setMensagens([]);
                setContato(null);
                setTemAnteriores(false);
                setCarregando(false);
                setEnviando(false);
                setCarregandoAnteriores(false);
                setErroAtualizacao("");
                setErro(error.response?.data?.error || "Esta conversa está indisponível. Verifique o aceite e sua participação social antes de tentar novamente.");
            });
            sessao.close();
        } else {
            sessao.commit(() => setErro(error.response?.data?.error || mensagem));
        }
    }

    useEffect(() => {
        const sessao = createChatSession();
        sessaoRef.current = sessao;
        let timer;
        let atualizando = false;
        let inicializada = false;
        cursorNovasRef.current = 0;
        cursorAnterioresRef.current = null;
        envioOcupadoRef.current = false;
        leituraOcupadaRef.current = false;
        erroLeituraEmRef.current = 0;
        setUltimaLeitura(0);
        setLendoFim(true);
        setAvisoLeitura("");
        anterioresOcupadoRef.current = false;
        seguirFinalRef.current = true;
        setCarregando(true);
        setAcesso(false);
        setMensagens([]);
        setContato(null);
        setErro("");
        setErroAtualizacao("");

        async function atualizar() {
            if (!sessao.isActive() || !inicializada || atualizando) return;
            clearTimeout(timer);
            if (document.hidden) { timer = setTimeout(atualizar, 5000); return; }
            atualizando = true;
            let restamNovas = false;
            try {
                // Cada ciclo e limitado; se houver mais paginas, continua em seguida.
                for (let pagina = 0; pagina < 3; pagina++) {
                    const resposta = await api.get(base + "/messages", {
                        params: { aposId: cursorNovasRef.current, limite: 50 }, signal: sessao.signal
                    });
                    if (!sessao.isActive()) return;
                    const lote = resposta.data;
                    sessao.commit(() => {
                        setMensagens((atuais) => mergeMessages(atuais, lote.mensagens));
                        if (lote.maiorId != null) cursorNovasRef.current = lote.maiorId;
                        setErroAtualizacao("");
                        setTentativaLeitura((atual) => atual + 1);
                    });
                    restamNovas = lote.temMais;
                    if (!restamNovas) break;
                }
            } catch (error) {
                if ([401, 403, 404].includes(error.response?.status)) tratarFalha(error, sessao, "Acesso indisponível.");
                else sessao.commit(() => setErroAtualizacao("Não foi possível atualizar as mensagens. Tentaremos novamente."));
            } finally {
                atualizando = false;
                if (sessao.isActive()) timer = setTimeout(atualizar, restamNovas ? 250 : 5000);
            }
        }
        atualizarRef.current = atualizar;

        async function iniciar() {
            try {
                const [detalhes, historico] = await Promise.all([
                    api.get(base, { signal: sessao.signal }),
                    api.get(base + "/messages", { params: { limite: 30 }, signal: sessao.signal })
                ]);
                sessao.commit(() => {
                    setContato(detalhes.data);
                    setMensagens(historico.data.mensagens);
                    setTemAnteriores(historico.data.temMais);
                    cursorAnterioresRef.current = historico.data.menorId;
                    cursorNovasRef.current = historico.data.maiorId ?? 0;
                    setAcesso(true);
                    inicializada = true;
                    timer = setTimeout(atualizar, 5000);
                });
            } catch (error) {
                tratarFalha(error, sessao, "Não foi possível carregar a conversa. Tente novamente.");
            } finally { sessao.commit(() => setCarregando(false)); }
        }
        function aoVoltar() { setVisivel(!document.hidden); if (!document.hidden) atualizar(); }
        document.addEventListener("visibilitychange", aoVoltar);
        iniciar();
        return () => {
            clearTimeout(timer);
            sessao.close();
            document.removeEventListener("visibilitychange", aoVoltar);
        };
    }, [base, revisao]);

    useLayoutEffect(() => {
        const lista = listaRef.current;
        if (!lista) return;
        const ancora = ancoraRef.current;
        if (ancora) {
            const elemento = lista.querySelector(`[data-message-id="${ancora.id}"]`);
            if (elemento) lista.scrollTop += elemento.getBoundingClientRect().top - ancora.top;
            ancoraRef.current = null;
        } else if (seguirFinalRef.current) lista.scrollTop = lista.scrollHeight;
    }, [mensagens, carregando]);

    async function carregarAnteriores() {
        const sessao = sessaoRef.current;
        if (!sessao?.isActive() || !acesso || anterioresOcupadoRef.current || !cursorAnterioresRef.current) return;
        anterioresOcupadoRef.current = true;
        setCarregandoAnteriores(true);
        setErro("");
        try {
            const resposta = await api.get(base + "/messages", {
                params: { antesDe: cursorAnterioresRef.current, limite: 30 }, signal: sessao.signal
            });
            sessao.commit(() => {
                const lista = listaRef.current;
                const topo = lista?.getBoundingClientRect().top ?? 0;
                const visivel = [...(lista?.querySelectorAll("[data-message-id]") ?? [])]
                    .find((elemento) => elemento.getBoundingClientRect().bottom > topo);
                if (visivel && resposta.data.mensagens.length > 0) ancoraRef.current = { id: visivel.dataset.messageId, top: visivel.getBoundingClientRect().top };
                setMensagens((atuais) => mergeMessages(atuais, resposta.data.mensagens));
                setTemAnteriores(resposta.data.temMais);
                if (resposta.data.menorId != null) cursorAnterioresRef.current = resposta.data.menorId;
            });
        } catch (error) { tratarFalha(error, sessao, "Não foi possível carregar as mensagens anteriores."); }
        finally { sessao.commit(() => { anterioresOcupadoRef.current = false; setCarregandoAnteriores(false); }); }
    }

    async function enviarMensagem(event) {
        event.preventDefault();
        const sessao = sessaoRef.current;
        const mensagem = conteudo.trim();
        if (!sessao?.isActive() || !acesso || bloqueando || envioOcupadoRef.current || !mensagem || mensagem.length > 1000) return;
        envioOcupadoRef.current = true;
        setEnviando(true);
        setErro("");
        try {
            envioPendenteRef.current = pendingSend(envioPendenteRef.current, mensagem);
            const resposta = await api.post(base + "/messages", envioPendenteRef.current, { signal: sessao.signal });
            sessao.commit(() => {
                seguirFinalRef.current = true;
                setMensagens((atuais) => mergeMessages(atuais, [resposta.data]));
                setConteudo("");
                envioPendenteRef.current = null;
                // O POST nao avanca o cursor: pode haver mensagens intermediarias ainda nao lidas.
                atualizarRef.current?.();
            });
        } catch (error) { tratarFalha(error, sessao, "Não foi possível enviar a mensagem. Seu texto foi mantido."); }
        finally { sessao.commit(() => { envioOcupadoRef.current = false; setEnviando(false); }); }
    }

    async function logout() { await revokeSession(); navigate("/login"); }

    useEffect(() => {
        const sessao = sessaoRef.current;
        const ateId = cursorNovasRef.current;
        if (!sessao?.isActive() || !acesso || carregando || leituraOcupadaRef.current
            || Date.now() - erroLeituraEmRef.current < 5000
            || !shouldMarkRead({ visible: visivel, following: lendoFim, received: ateId, confirmed: ultimaLeitura })) return;
        leituraOcupadaRef.current = true;
        api.post(base + "/read", { ateId }, { signal: sessao.signal })
            .then(() => sessao.commit(() => {
                leituraOcupadaRef.current = false;
                setUltimaLeitura((anterior) => Math.max(anterior, ateId));
                setAvisoLeitura("");
            })).catch((error) => {
                sessao.commit(() => { leituraOcupadaRef.current = false; });
                if ([401, 403, 404].includes(error.response?.status)) tratarFalha(error, sessao, "Conversa indisponível.");
                else sessao.commit(() => {
                    erroLeituraEmRef.current = Date.now();
                    setAvisoLeitura("Não foi possível confirmar a leitura. Tentaremos novamente.");
                });
            });
    }, [base, mensagens, acesso, carregando, visivel, lendoFim, ultimaLeitura, tentativaLeitura]);

    async function bloquearContato() {
        const sessao = sessaoRef.current;
        if (!sessao?.isActive() || bloqueando || !contato?.outroUsuarioId) return;
        setBloqueando(true);
        try {
            await api.post(`/community/contacts/blocks/${contato.outroUsuarioId}`, {}, { signal: sessao.signal });
            sessao.commit(() => {
                setAcesso(false);
                setMensagens([]);
                navigate("/community?aba=conversas&caixa=BLOQUEADOS");
            });
            sessao.close();
        } catch (error) { tratarFalha(error, sessao, "Não foi possível bloquear o contato."); }
        finally { sessao.commit(() => setBloqueando(false)); }
    }

    return (
        <main className="community-details-page">
            <header className="app-header"><div className="app-header-content">
                <div className="app-brand"><span className="app-brand-icon">✦</span><div><span className="app-brand-name">Recommendi.a</span><span className="app-brand-subtitle">Descubra seu próximo hobby</span></div></div>
                <div className="app-header-actions"><NotificationBell /><span className="app-user-greeting">Olá, {nome}!</span>
                    <button className="app-header-button" onClick={() => navigate("/community?aba=conversas&caixa=ACEITAS")}>Conversas</button>
                    <button className="app-logout-button" onClick={logout}>Sair</button>
                </div>
            </div></header>
            <section className="community-details-container">
                <button className="community-back-button" onClick={() => navigate("/community?aba=conversas&caixa=ACEITAS")}>← Voltar às conversas</button>
                <div className="community-chat private-conversation">
                    <div className="community-chat-header private-conversation-heading">
                        <div><span>CONVERSA PRIVADA</span><h1>{contato?.outraPessoa?.nome || "Conversa"}</h1>
                            {contato?.outraPessoa?.proximidade && <p>{contato.outraPessoa.proximidade === "MESMA_CIDADE" ? "É da mesma cidade que você" : "É do mesmo estado que você"}</p>}
                        </div>
                        {acesso && <button type="button" className="social-contact-block" disabled={bloqueando || enviando} onClick={bloquearContato}>Bloquear contato</button>}
                    </div>
                    {erro && <div className="community-chat-error" role="alert">{erro}</div>}
                    {erroAtualizacao && <p className="community-chat-update-note" role="status">{erroAtualizacao}</p>}
                    {avisoLeitura && <p className="community-chat-update-note" role="status">{avisoLeitura}</p>}
                    {!carregando && !acesso && <div className="community-chat-tools"><button type="button" onClick={() => setRevisao((atual) => atual + 1)}>Verificar acesso novamente</button></div>}
                    {acesso && <div className="community-chat-tools">
                        <button type="button" disabled={!temAnteriores || carregandoAnteriores || bloqueando} onClick={carregarAnteriores}>{carregandoAnteriores ? "Carregando..." : "Carregar anteriores"}</button>
                        <button type="button" disabled={bloqueando} onClick={() => atualizarRef.current?.()}>Atualizar mensagens</button>
                    </div>}
                    <div className="community-messages" ref={listaRef} aria-label="Mensagens privadas"
                        onScroll={() => { const lista = listaRef.current; const fim = lista.scrollHeight - lista.scrollTop - lista.clientHeight < 80; seguirFinalRef.current = fim; setLendoFim(fim); }}>
                        {carregando ? <div className="community-messages-empty">Carregando mensagens...</div>
                            : !acesso ? <div className="community-messages-empty">Conversa indisponível.</div>
                                : mensagens.length === 0 ? <div className="community-messages-empty"><strong>Inicie a conversa</strong><span>O contato foi aceito. Envie sua primeira mensagem.</span></div>
                                    : mensagens.map((mensagem) => <Message key={mensagem.id} mensagem={mensagem} />)}
                    </div>
                    <form className="community-message-form" onSubmit={enviarMensagem}>
                        <label className="community-message-label" htmlFor="mensagem-privada">Sua mensagem</label>
                        <textarea id="mensagem-privada" value={conteudo} onChange={(event) => setConteudo(event.target.value)}
                            placeholder="Escreva uma mensagem..." maxLength={1000} rows={3} disabled={!acesso || carregando || enviando || bloqueando} />
                        <div className="community-message-form-footer"><span>{conteudo.length}/1000</span>
                            <button type="submit" disabled={!acesso || carregando || enviando || bloqueando || !conteudo.trim()}>{enviando ? "Enviando..." : "Enviar"}</button>
                        </div>
                    </form>
                </div>
            </section>
        </main>
    );
}

function Message({ mensagem }) {
    const data = new Date(mensagem.dataEnvio);
    return (
        <div data-message-id={mensagem.id} className={mensagem.propria ? "community-message own" : "community-message"}>
            <div className="community-message-meta"><strong>{mensagem.propria ? "Você" : mensagem.autorNome}</strong>
                <time dateTime={mensagem.dataEnvio} title={data.toLocaleString("pt-BR")}>{data.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</time>
            </div>
            <p>{mensagem.conteudo}</p>
        </div>
    );
}
