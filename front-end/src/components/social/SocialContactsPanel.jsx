import RecommendationBanner from "../recommendations/RecommendationBanner";
import { useEffect, useRef, useState } from "react";
import { api } from "../../api/api";
import { useNavigate } from "react-router";
import "./SocialContacts.css";
import "./PrivateConversation.css";

const CAIXAS = { RECEBIDAS: "Recebidas", ENVIADAS: "Enviadas", ACEITAS: "Aceitas", HISTORICO: "Histórico", BLOQUEADOS: "Bloqueados" };
const ROTULOS = { PENDENTE: "Aguardando resposta", ACEITA: "Contato aceito", RECUSADA: "Solicitação não aceita", CANCELADA: "Solicitação cancelada", ENCERRADA: "Contato encerrado" };
const dataLocal = (valor) => valor ? new Date(valor).toLocaleString("pt-BR") : "";

function PerfilContato({ perfil }) {
    if (!perfil) return <h3>Participante indisponível</h3>;
    const afinidade = perfil.afinidade;
    return (
        <>
            <h3>{perfil.nome}</h3>
            {perfil.proximidade && <p className="social-location-badge" title="Localização declarada pelo participante.">
                {perfil.proximidade === "MESMA_CIDADE" ? "É da mesma cidade que você" : "É do mesmo estado que você"}
            </p>}
            <p>{perfil.resumo}</p>
            {[
                ["Vocês praticam", afinidade?.praticandoEmComum],
                ["Hobbies de interesse em comum", afinidade?.hobbiesDeInteresseEmComum],
                ["Interesses em comum", afinidade?.interessesEmComum]
            ].map(([titulo, itens]) => itens?.length > 0 && (
                <div className="social-affinity-group" key={titulo}><span>{titulo}</span>
                    <div className="social-affinity-tags">{itens.map((item) => <span key={item}>{item}</span>)}</div>
                </div>
            ))}
        </>
    );
}

export default function SocialContactsPanel({ caixa, onCaixaChange, onBloqueioAlterado, naoLidas = { total: 0, porContato: [] } }) {
    const navigate = useNavigate();
    const [pagina, setPagina] = useState(0);
    const [dados, setDados] = useState(null);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState("");
    const [mensagem, setMensagem] = useState("");
    const [revisao, setRevisao] = useState(0);
    const [processando, setProcessando] = useState(false);
    const ocupado = useRef(false);

    useEffect(() => {
        const controller = new AbortController();
        setCarregando(true);
        setErro("");
        setDados(null);
        api.get(caixa === "BLOQUEADOS" ? "/community/contacts/blocks" : "/community/contacts", {
            params: { caixa: caixa === "BLOQUEADOS" ? undefined : caixa, pagina },
            signal: controller.signal
        }).then((resposta) => {
            if (controller.signal.aborted) return;
            if (pagina > 0 && pagina >= resposta.data.totalPages) {
                setPagina(Math.max(0, resposta.data.totalPages - 1));
                return;
            }
            setDados(resposta.data);
        }).catch((error) => {
            if (!controller.signal.aborted) setErro(error.response?.data?.error || "Não foi possível carregar seus contatos.");
        }).finally(() => {
            if (!controller.signal.aborted) setCarregando(false);
        });
        return () => controller.abort();
    }, [caixa, pagina, revisao]);

    function mudarCaixa(valor) {
        if (ocupado.current) return;
        setPagina(0);
        setMensagem("");
        onCaixaChange(valor);
    }

    async function agir(acao, item) {
        if (ocupado.current) return;
        ocupado.current = true;
        setProcessando(true);
        setErro("");
        setMensagem("");
        try {
            if (acao === "block") {
                await api.post(`/community/contacts/blocks/${item.outroUsuarioId}`);
                setMensagem("Participante bloqueado. O contato foi encerrado.");
                onBloqueioAlterado();
            } else if (acao === "unblock") {
                await api.delete(`/community/contacts/blocks/${item.userId}`);
                setMensagem("Bloqueio removido. Contatos anteriores continuam encerrados.");
                onBloqueioAlterado();
            } else {
                await api.post(`/community/contacts/${item.id}/${acao}`);
                setMensagem(acao === "accept" ? "Solicitação aceita." : acao === "decline" ? "Solicitação recusada." : "Solicitação cancelada.");
            }
            setRevisao((atual) => atual + 1);
        } catch (error) {
            setErro(error.response?.data?.error || "Não foi possível concluir a operação. Atualize a lista e tente novamente.");
        } finally {
            ocupado.current = false;
            setProcessando(false);
        }
    }

    return (
        <section className="social-contacts-panel" aria-labelledby="titulo-contatos">
            <RecommendationBanner id="titulo-contatos" title="Solicitações de conversa"
                description="Confira quem deseja conversar com você e escolha quais contatos aceitar.">
                <button className="social-contact-refresh" type="button" disabled={carregando || processando}
                    onClick={() => setRevisao(atual => atual + 1)}>Atualizar</button>
            </RecommendationBanner>
            <nav className="social-contact-boxes" aria-label="Organizar contatos">
                {Object.entries(CAIXAS).map(([valor, rotulo]) => (
                    <button type="button" key={valor} aria-pressed={caixa === valor}
                        disabled={processando} onClick={() => mudarCaixa(valor)}>{rotulo}{valor === "ACEITAS" && naoLidas.total > 0 ? ` (${naoLidas.total} não lidas)` : ""}</button>
                ))}
            </nav>
            <p className="social-contact-help">Bloquear encerra o contato e impede novas solicitações entre vocês. Os indicadores regionais usam a localização declarada no perfil.</p>
            {mensagem && <p className="social-contact-success" role="status">{mensagem}</p>}
            {erro && <p className="social-contact-error" role="alert">{erro}</p>}
            {naoLidas.erro && <p className="social-contact-help" role="status">{naoLidas.erro}</p>}
            {carregando ? <p role="status">Carregando contatos...</p> : !dados ? (
                <button type="button" disabled={processando} onClick={() => setRevisao((atual) => atual + 1)}>Tentar novamente</button>
            ) : dados.content.length === 0 ? <p>Nenhum contato nesta seção.</p> : (
                <div className="community-grid">
                    {dados.content.map((item) => caixa === "BLOQUEADOS" ? (
                        <article className="social-contact-card" key={item.userId}>
                            <h3>{item.nome}</h3><p>Bloqueado em {dataLocal(item.bloqueadoEm)}</p>
                            <button type="button" disabled={processando} onClick={() => agir("unblock", item)}>Desbloquear</button>
                        </article>
                    ) : (
                        <article className="social-contact-card" key={item.id}>
                            <span className="social-contact-status">{ROTULOS[item.status]}</span>
                            <PerfilContato perfil={item.outraPessoa} />
                            <p className="social-contact-date">{item.direcao === "RECEBIDA" ? "Recebida" : "Enviada"} em {dataLocal(item.solicitadaEm)}</p>
                            {item.apresentacao && <blockquote><strong>{item.direcao === "RECEBIDA" ? "Apresentação recebida" : "Sua apresentação"}</strong><p>{item.apresentacao}</p></blockquote>}
                            {item.novaSolicitacaoApos && <p className="social-contact-help">Nova solicitação disponível após {dataLocal(item.novaSolicitacaoApos)}.</p>}
                            <div className="social-contact-actions">
                                {item.status === "ACEITA" && item.disponivel && (
                                    <button type="button" className="social-contact-primary" disabled={processando}
                                        onClick={() => navigate(`/community/conversations/${item.id}`)}>
                                        Abrir conversa
                                        {(naoLidas.porContato.find((contagem) => contagem.contatoId === item.id)?.quantidade ?? 0) > 0 && (
                                            <span className="private-unread-badge">{naoLidas.porContato.find((contagem) => contagem.contatoId === item.id).quantidade}</span>
                                        )}
                                    </button>
                                )}
                                {item.status === "PENDENTE" && item.direcao === "RECEBIDA" && item.disponivel && <>
                                    <button type="button" className="social-contact-primary" disabled={processando} onClick={() => agir("accept", item)}>Aceitar conversa</button>
                                    <button type="button" disabled={processando} onClick={() => agir("decline", item)}>Recusar</button>
                                </>}
                                {item.status === "PENDENTE" && item.direcao === "ENVIADA" && <button type="button" disabled={processando} onClick={() => agir("cancel", item)}>Cancelar solicitação</button>}
                                <button type="button" className="social-contact-block" disabled={processando} onClick={() => agir("block", item)}>Bloquear contato</button>
                            </div>
                        </article>
                    ))}
                </div>
            )}
            {dados && dados.totalPages > 1 && <div className="social-contact-pagination">
                <button type="button" disabled={pagina === 0 || processando} onClick={() => setPagina((atual) => atual - 1)}>Anterior</button>
                <span>Página {pagina + 1} de {dados.totalPages}</span>
                <button type="button" disabled={pagina + 1 >= dados.totalPages || processando} onClick={() => setPagina((atual) => atual + 1)}>Próxima</button>
            </div>}
        </section>
    );
}
