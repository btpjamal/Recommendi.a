package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Entities.CommunityPost;
import dev.jamal.projetotcc.Repository.SocialFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SocialFeedService {
    private final SocialFeedRepository repository;
    private final SocialFollowService follows;

    @Transactional(readOnly = true)
    public SocialFeedPageDTO listar(Long viewer, boolean following, Long before, int limit) {
        follows.validarParticipante(viewer, false);
        if ((before != null && before <= 0) || limit < 1 || limit > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use um cursor válido e limite de 1 a 30.");
        }
        var rows = repository.buscar(viewer, before == null ? Long.MAX_VALUE : before,
                LocalDate.now().minusYears(18), following, PageRequest.of(0, limit + 1));
        var items = rows.stream().limit(limit).map(p -> item(p, viewer)).toList();
        Long cursor = items.isEmpty() ? null : items.get(items.size() - 1).publicacao().id();
        return new SocialFeedPageDTO(items, rows.size() > limit, cursor);
    }

    private SocialFeedItemDTO item(CommunityPost p, Long viewer) {
        var community = p.getCommunity();
        var post = new CommunityPostDTO(p.getId(), community.getId(), p.getUser().getId(), p.getUser().getNome(),
                p.getConteudo(), p.isPossuiImagem(), p.getCreatedAt(), p.getEditedAt(), p.getVersion(),
                p.getUser().getId().equals(viewer));
        return new SocialFeedItemDTO(post, community.getHobby().getNome(), community.getTipo().name(),
                community.getEstado(), community.getCidade());
    }
}
