package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Entities.CommunityPost;
import dev.jamal.projetotcc.Repository.SocialFeedRepository;
import dev.jamal.projetotcc.Repository.CommunityCommentRepository;
import dev.jamal.projetotcc.Repository.CommunityPostLikeRepository;
import java.util.HashMap;
import java.util.List;
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

    private final CommunityCommentRepository comments;
    private final CommunityPostLikeRepository likes;

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public SocialFeedPageDTO listar(Long viewer, boolean following, Long before, int limit) {
        follows.validarParticipante(viewer, false);
        if ((before != null && before <= 0) || limit < 1 || limit > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use um cursor válido e limite de 1 a 30.");
        }
        var cutoff = LocalDate.now().minusYears(18);
        var rows = repository.buscar(viewer, before == null ? Long.MAX_VALUE : before,
                cutoff, following, PageRequest.of(0, limit + 1));
        if (rows.isEmpty()) return new SocialFeedPageDTO(List.of(), false, null);

        var page = rows.stream().limit(limit).toList();
        var ids = page.stream().map(SocialFeedRepository.FeedRow::getId).toList();
        var posts = new HashMap<Long, CommunityPost>();
        repository.carregar(ids).forEach(p -> posts.put(p.getId(), p));
        var commentCounts = new HashMap<Long, Long>();
        comments.contarPagina(ids, viewer, cutoff).forEach(c -> commentCounts.put(c.getPostId(), c.getTotal()));
        var likeCounts = new HashMap<Long, CommunityPostLikeRepository.PostLikes>();
        likes.contarPagina(ids, viewer, cutoff).forEach(l -> likeCounts.put(l.getPostId(), l));

        var items = page.stream().map(row -> {
            var count = likeCounts.get(row.getId());
            var interactions = new CommunityInteractionDTO(count == null ? 0 : count.getTotal(),
                    count != null && count.getMine() > 0, commentCounts.getOrDefault(row.getId(), 0L));
            return item(posts.get(row.getId()), viewer, row.getSeguindo(), interactions);
        }).toList();
        Long cursor = items.isEmpty() ? null : items.get(items.size() - 1).publicacao().id();
        return new SocialFeedPageDTO(items, rows.size() > limit, cursor);
    }

    private SocialFeedItemDTO item(CommunityPost p, Long viewer, boolean following, CommunityInteractionDTO interactions) {
        var community = p.getCommunity();
        var post = new CommunityPostDTO(p.getId(), community.getId(), p.getUser().getId(), p.getUser().getNome(),
                p.getConteudo(), p.isPossuiImagem(), p.getCreatedAt(), p.getEditedAt(), p.getVersion(),
                p.getUser().getId().equals(viewer));
        return new SocialFeedItemDTO(post, community.getHobby().getNome(), community.getTipo().name(),
                community.getEstado(), community.getCidade(), following, interactions);
    }
}
