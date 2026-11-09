package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.*;
import dev.jamal.projetotcc.Repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialContactService {
    private final SocialContactRepository contacts;
    private final SocialUserBlockRepository blocks;
    private final SocialContactLockRepository locks;
    private final UserRepository users;
    private final SocialProfileRepository profiles;
    private final SocialProfileService social;
    private final SocialHubService hub;
    private final SocialAffinityService affinity;
    private final SocialLocationService location;

    @PersistenceContext private EntityManager entityManager;

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }

    private void validarAtor(Long id) {
        if (id == null || !social.elegivel(id) || !social.participa(id)) {
            throw erro(HttpStatus.FORBIDDEN, "Ative sua participação social para gerenciar contatos. O recurso é exclusivo para maiores de 18 anos.");
        }
    }

    private boolean disponivel(Long id) {
        return users.existsById(id) && social.participa(id) && social.elegivel(id);
    }

    private void bloquearPar(Long a, Long b) {
        if (b == null || b <= 0 || a.equals(b)) {
            throw erro(HttpStatus.BAD_REQUEST, "Escolha outro participante.");
        }
        if (locks.lockUser(Math.min(a, b)).isEmpty() || locks.lockUser(Math.max(a, b)).isEmpty()) {
            throw erro(HttpStatus.NOT_FOUND, "Participante indisponível.");
        }
    }

    private void validarDisponibilidade(Long a, Long b) {
        if (blocks.bloqueados(a, b) || !disponivel(b)) {
            throw erro(HttpStatus.FORBIDDEN, "Este contato está indisponível.");
        }
    }

    private SocialContact carregarBloqueado(Long atorId, Long contatoId) {
        validarAtor(atorId);
        SocialContact contato = contacts.findById(contatoId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));
        if (!atorId.equals(contato.getLowUserId()) && !atorId.equals(contato.getHighUserId())) {
            throw erro(HttpStatus.NOT_FOUND, "Solicitação não encontrada.");
        }
        bloquearPar(contato.getLowUserId(), contato.getHighUserId());
        // O registro pode ter mudado enquanto os bloqueios do par eram aguardados.
        entityManager.refresh(contato);
        return contato;
    }

    private Instant novaSolicitacaoApos(SocialContact contato) {
        return switch (contato.getStatus()) {
            case RECUSADA -> contato.getUpdatedAt().plus(7, ChronoUnit.DAYS);
            case CANCELADA, ENCERRADA -> contato.getUpdatedAt().plus(1, ChronoUnit.DAYS);
            default -> null;
        };
    }

    public SocialContactDTO solicitar(Long atorId, SocialContactRequestDTO pedido) {
        validarAtor(atorId);
        Long destino = pedido.destinatarioId();
        bloquearPar(atorId, destino);
        validarDisponibilidade(atorId, destino);
        String texto = pedido.apresentacao() == null ? "" : pedido.apresentacao().strip();
        if (texto.length() > 300) throw erro(HttpStatus.BAD_REQUEST, "A apresentação deve ter até 300 caracteres.");

        SocialContact contato = contacts.findByLowUserIdAndHighUserId(
                Math.min(atorId, destino), Math.max(atorId, destino)).orElse(null);
        if (contato != null) {
            if (contato.getStatus() == SocialContactStatus.PENDENTE || contato.getStatus() == SocialContactStatus.ACEITA) {
                return converter(atorId, contato);
            }
            Instant limite = novaSolicitacaoApos(contato);
            if (limite != null && Instant.now().isBefore(limite)) {
                throw erro(HttpStatus.CONFLICT, "Aguarde até " + limite + " para solicitar contato novamente.");
            }
        }
        if (contacts.countByRequesterIdAndRequestedAtGreaterThanEqual(atorId, Instant.now().minus(1, ChronoUnit.DAYS)) >= 10) {
            throw erro(HttpStatus.TOO_MANY_REQUESTS, "Você atingiu o limite de 10 solicitações em 24 horas.");
        }
        if (contato == null) {
            contato = new SocialContact();
            contato.setLowUserId(Math.min(atorId, destino));
            contato.setHighUserId(Math.max(atorId, destino));
        }
        contato.setRequesterId(atorId);
        contato.setRecipientId(destino);
        contato.setApresentacao(texto);
        contato.setStatus(SocialContactStatus.PENDENTE);
        contato.setRequestedAt(Instant.now());
        contato.setUpdatedAt(contato.getRequestedAt());
        return converter(atorId, contacts.saveAndFlush(contato));
    }

    public SocialContactDTO responder(Long atorId, Long contatoId, boolean aceitar) {
        SocialContact contato = carregarBloqueado(atorId, contatoId);
        if (!atorId.equals(contato.getRecipientId())) {
            throw erro(HttpStatus.FORBIDDEN, "Somente o destinatário pode responder à solicitação.");
        }
        validarDisponibilidade(atorId, contato.getRequesterId());
        if (aceitar && contato.getStatus() == SocialContactStatus.ACEITA) return converter(atorId, contato);
        if (!aceitar && contato.getStatus() == SocialContactStatus.RECUSADA) return converter(atorId, contato);
        if (contato.getStatus() != SocialContactStatus.PENDENTE) {
            throw erro(HttpStatus.CONFLICT, "Esta solicitação não está mais pendente. Atualize a lista.");
        }
        contato.setStatus(aceitar ? SocialContactStatus.ACEITA : SocialContactStatus.RECUSADA);
        contato.setUpdatedAt(Instant.now());
        return converter(atorId, contato);
    }

    public SocialContactDTO cancelar(Long atorId, Long contatoId) {
        SocialContact contato = carregarBloqueado(atorId, contatoId);
        if (!atorId.equals(contato.getRequesterId())) {
            throw erro(HttpStatus.FORBIDDEN, "Somente o remetente pode cancelar a solicitação.");
        }
        if (contato.getStatus() == SocialContactStatus.CANCELADA) return converter(atorId, contato);
        if (contato.getStatus() != SocialContactStatus.PENDENTE) {
            throw erro(HttpStatus.CONFLICT, "Somente solicitações pendentes podem ser canceladas.");
        }
        contato.setStatus(SocialContactStatus.CANCELADA);
        contato.setUpdatedAt(Instant.now());
        return converter(atorId, contato);
    }

    public void bloquear(Long atorId, Long outroId) {
        validarAtor(atorId);
        bloquearPar(atorId, outroId);
        if (blocks.findByBlockerIdAndBlockedId(atorId, outroId).isEmpty()) {
            var bloqueio = new SocialUserBlock();
            bloqueio.setBlockerId(atorId);
            bloqueio.setBlockedId(outroId);
            bloqueio.setCreatedAt(Instant.now());
            blocks.saveAndFlush(bloqueio);
        }
        contacts.findByLowUserIdAndHighUserId(Math.min(atorId, outroId), Math.max(atorId, outroId))
                .ifPresent(contato -> {
                    if (contato.getStatus() != SocialContactStatus.ENCERRADA) {
                        contato.setStatus(SocialContactStatus.ENCERRADA);
                        contato.setUpdatedAt(Instant.now());
                    }
                });
    }

    public void desbloquear(Long atorId, Long outroId) {
        validarAtor(atorId);
        bloquearPar(atorId, outroId);
        blocks.findByBlockerIdAndBlockedId(atorId, outroId).ifPresent(blocks::delete);
    }

    @Transactional(readOnly = true)
    public Page<SocialContactDTO> listar(Long atorId, SocialContactBox caixa, int pagina) {
        validarAtor(atorId);
        return contacts.buscarCaixa(atorId, caixa.name(), PageRequest.of(Math.max(0, pagina), 12))
                .map(contato -> converter(atorId, contato));
    }

    @Transactional(readOnly = true)
    public Page<SocialBlockDTO> bloqueados(Long atorId, int pagina) {
        validarAtor(atorId);
        return blocks.findByBlockerIdOrderByCreatedAtDescIdDesc(atorId, PageRequest.of(Math.max(0, pagina), 12))
                .map(bloqueio -> new SocialBlockDTO(bloqueio.getBlockedId(),
                        users.findById(bloqueio.getBlockedId()).map(User::getNome).orElse("Participante"),
                        bloqueio.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public void validarConversaAceita(Long atorId, Long contatoId) {
        validarAtor(atorId);
        SocialContact contato = contacts.findById(contatoId)
                .filter(c -> atorId.equals(c.getLowUserId()) || atorId.equals(c.getHighUserId()))
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Contato não encontrado."));
        Long outro = atorId.equals(contato.getLowUserId()) ? contato.getHighUserId() : contato.getLowUserId();
        validarDisponibilidade(atorId, outro);
        if (contato.getStatus() != SocialContactStatus.ACEITA) {
            throw erro(HttpStatus.FORBIDDEN, "A conversa precisa ser aceita antes de trocar mensagens.");
        }
    }

    public SocialContact carregarConversaAceitaBloqueada(Long atorId, Long contatoId) {
        SocialContact contato = carregarBloqueado(atorId, contatoId);
        for (Long participante : java.util.List.of(contato.getLowUserId(), contato.getHighUserId())) {
            // Reconfere dados que podem ter sido carregados antes de aguardar os locks.
            entityManager.refresh(users.findById(participante).orElseThrow());
            profiles.findByUser_Id(participante).ifPresent(entityManager::refresh);
        }
        validarAtor(atorId);
        Long outro = atorId.equals(contato.getLowUserId()) ? contato.getHighUserId() : contato.getLowUserId();
        validarDisponibilidade(atorId, outro);
        if (contato.getStatus() != SocialContactStatus.ACEITA) {
            throw erro(HttpStatus.FORBIDDEN, "A conversa precisa estar aceita para acessar as mensagens.");
        }
        return contato;
    }

    public SocialContactDTO detalhesConversa(Long atorId, Long contatoId) {
        return converter(atorId, carregarConversaAceitaBloqueada(atorId, contatoId));
    }

    @Transactional(readOnly = true)
    public void validarParticipacaoSocial(Long atorId) { validarAtor(atorId); }

    private SocialContactDTO converter(Long atorId, SocialContact contato) {
        Long outro = atorId.equals(contato.getLowUserId()) ? contato.getHighUserId() : contato.getLowUserId();
        boolean ativo = disponivel(outro) && !blocks.bloqueados(atorId, outro);
        SocialProfileDTO perfil = null;
        if (ativo) {
            SocialProfileDTO publico = hub.buscarPerfilPublico(outro);
            String proximidade = profiles.findByUser_Id(outro)
                    .map(p -> location.proximidade(location.buscar(atorId), p)).orElse(null);
            perfil = new SocialProfileDTO(publico.userId(), publico.nome(), publico.resumo(),
                    publico.interesses(), publico.praticando(), publico.interessados(),
                    affinity.calcular(hub.buscarPerfilPublico(atorId), publico), proximidade);
        }
        return new SocialContactDTO(contato.getId(), contato.getStatus(),
                atorId.equals(contato.getRecipientId()) ? "RECEBIDA" : "ENVIADA",
                contato.getRequestedAt(), contato.getUpdatedAt(), novaSolicitacaoApos(contato),
                contato.getApresentacao(), outro, perfil, ativo);
    }
}
