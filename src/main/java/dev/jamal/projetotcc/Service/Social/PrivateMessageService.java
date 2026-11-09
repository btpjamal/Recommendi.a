package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Entities.PrivateMessage;
import dev.jamal.projetotcc.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Transactional
public class PrivateMessageService {
    private final SocialContactService contacts;
    private final PrivateMessageRepository messages;
    private final PrivateMessageReadRepository reads;
    private final UserRepository users;

    private ResponseStatusException invalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    public SocialContactDTO detalhes(Long atorId, Long contatoId) {
        return contacts.detalhesConversa(atorId, contatoId);
    }

    public PrivateMessagePageDTO listar(Long atorId, Long contatoId, Long antesDe, Long aposId, int limite) {
        // Mantem os locks do par ate o termino desta transacao, junto com bloqueios e envios.
        contacts.carregarConversaAceitaBloqueada(atorId, contatoId);
        if (antesDe != null && aposId != null) throw invalido("Use somente um cursor por consulta.");
        if ((antesDe != null && antesDe <= 0) || (aposId != null && aposId < 0) || limite < 1 || limite > 50) {
            throw invalido("Cursor inválido ou limite fora do intervalo de 1 a 50.");
        }
        var pagina = PageRequest.of(0, limite + 1);
        var encontradas = aposId != null ? messages.novas(contatoId, aposId, pagina)
                : messages.anteriores(contatoId, antesDe == null ? Long.MAX_VALUE : antesDe, pagina);
        var lista = encontradas.stream().limit(limite).sorted(Comparator.comparing(PrivateMessage::getId))
                .map(m -> converter(m, atorId)).toList();
        return new PrivateMessagePageDTO(lista, encontradas.size() > limite,
                lista.isEmpty() ? null : lista.get(0).id(),
                lista.isEmpty() ? null : lista.get(lista.size() - 1).id());
    }

    public PrivateMessageDTO enviar(Long atorId, Long contatoId, SendPrivateMessageDTO pedido) {
        var contato = contacts.carregarConversaAceitaBloqueada(atorId, contatoId);
        String texto = pedido == null || pedido.conteudo() == null ? "" : pedido.conteudo().strip();
        if (texto.isBlank() || texto.length() > 1000 || pedido.clientMessageId() == null) {
            throw invalido("Informe uma mensagem de até 1000 caracteres e um identificador de envio válido.");
        }
        var repetida = messages.findByContactIdAndSenderIdAndClientMessageId(contatoId, atorId, pedido.clientMessageId());
        if (repetida.isPresent()) {
            if (!texto.equals(repetida.get().getConteudo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este identificador já foi usado para outra mensagem.");
            }
            return converter(repetida.get(), atorId);
        }
        var mensagem = new PrivateMessage();
        mensagem.setContactId(contatoId); mensagem.setSenderId(atorId);
        mensagem.setConteudo(texto); mensagem.setClientMessageId(pedido.clientMessageId());
        mensagem.setCreatedAt(Instant.now());
        contato.setUpdatedAt(mensagem.getCreatedAt());
        return converter(messages.saveAndFlush(mensagem), atorId);
    }

    public void marcarLidas(Long atorId, Long contatoId, Long ateId) {
        contacts.carregarConversaAceitaBloqueada(atorId, contatoId);
        if (ateId == null || ateId <= 0 || !messages.existsByIdAndContactId(ateId, contatoId)) {
            throw invalido("A mensagem informada não pertence a esta conversa.");
        }
        reads.avancar(contatoId, atorId, ateId);
    }

    @Transactional(readOnly = true)
    public PrivateUnreadDTO naoLidas(Long atorId) {
        contacts.validarParticipacaoSocial(atorId);
        var contagens = messages.contarNaoLidas(atorId, LocalDate.now().minusYears(18)).stream()
                .map(item -> new PrivateUnreadDTO.ContactCount(item.getContatoId(), item.getQuantidade())).toList();
        return new PrivateUnreadDTO(contagens.stream().mapToLong(PrivateUnreadDTO.ContactCount::quantidade).sum(), contagens);
    }

    private PrivateMessageDTO converter(PrivateMessage mensagem, Long atorId) {
        return new PrivateMessageDTO(mensagem.getId(), mensagem.getSenderId(),
                users.findById(mensagem.getSenderId()).orElseThrow().getNome(), mensagem.getConteudo(),
                mensagem.getCreatedAt(), mensagem.getSenderId().equals(atorId));
    }
}
