package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityMessageService {
    private final CommunityMessageRepository messageRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CommunityChatLockRepository chatLocks;

    @Transactional(readOnly = true)
    public List<CommunityMessageDTO> listar(Long communityId, Long userId) {
        return listarPagina(communityId, userId, null, null, 30).mensagens();
    }

    @Transactional(readOnly = true)
    public CommunityMessagePageDTO listarPagina(Long communityId, Long userId,
                                                Long antesDe, Long aposId, int limite) {
        validarMembro(communityId, userId);
        if (antesDe != null && aposId != null) throw new IllegalArgumentException("Use apenas um cursor por consulta.");
        if ((antesDe != null && antesDe <= 0) || (aposId != null && aposId < 0) || limite < 1 || limite > 50) {
            throw new IllegalArgumentException("Cursor inválido ou limite fora do intervalo de 1 a 50.");
        }
        var pagina = PageRequest.of(0, limite + 1);
        var encontradas = aposId != null
                ? messageRepository.buscarNovas(communityId, aposId, pagina)
                : messageRepository.buscarAnteriores(communityId, antesDe == null ? Long.MAX_VALUE : antesDe, pagina);
        boolean temMais = encontradas.size() > limite;
        var mensagens = encontradas.stream().limit(limite).sorted(Comparator.comparing(CommunityMessage::getId))
                .map(m -> converterParaDTO(m, userId)).toList();
        return new CommunityMessagePageDTO(mensagens, temMais,
                mensagens.isEmpty() ? null : mensagens.get(0).id(),
                mensagens.isEmpty() ? null : mensagens.get(mensagens.size() - 1).id());
    }

    @Transactional
    public CommunityMessageDTO enviar(Long communityId, Long userId, CreateCommunityMessageDTO request) {
        // Ordem uniforme com entrar/sair; IDs seguem a ordem de commit na comunidade.
        if (chatLocks.lockUser(userId).isEmpty() || chatLocks.lockCommunity(communityId).isEmpty()) {
            throw new IllegalStateException("Usuário ou comunidade indisponível.");
        }
        validarMembro(communityId, userId);
        String conteudo = request == null || request.conteudo() == null ? "" : request.conteudo().strip();
        if (conteudo.isBlank()) throw new IllegalArgumentException("A mensagem não pode estar vazia.");
        if (conteudo.length() > 1000) throw new IllegalArgumentException("A mensagem deve possuir no máximo 1000 caracteres.");
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new IllegalStateException("Comunidade não encontrada."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));
        var message = new CommunityMessage();
        message.setCommunity(community);
        message.setUser(user);
        message.setConteudo(conteudo);
        return converterParaDTO(messageRepository.saveAndFlush(message), userId);
    }

    private void validarMembro(Long communityId, Long userId) {
        if (!memberRepository.existsByCommunity_IdAndUser_Id(communityId, userId)) {
            throw new IllegalStateException("Você precisa participar da comunidade para acessar as mensagens.");
        }
    }

    private CommunityMessageDTO converterParaDTO(CommunityMessage message, Long userId) {
        return new CommunityMessageDTO(message.getId(), message.getUser().getId(), message.getUser().getNome(),
                message.getConteudo(), message.getDataEnvio(), message.getUser().getId().equals(userId));
    }
}