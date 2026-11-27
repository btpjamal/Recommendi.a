package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.DTO.Social.CommunityPostDTO;
import dev.jamal.projetotcc.Entities.CommunityPost;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
import java.time.LocalDate;

public interface CommunityPostRepository extends JpaRepository<CommunityPost,Long> {
    Optional<CommunityPost> findByUser_IdAndClientId(Long userId, UUID clientId);
    Optional<CommunityPost> findByIdAndCommunity_Id(Long id, Long communityId);
    @Query("""
        SELECT new dev.jamal.projetotcc.DTO.Social.CommunityPostDTO(
            p.id, p.community.id, u.id, u.nome, p.conteudo, p.possuiImagem,
            p.createdAt, p.editedAt, p.version, CASE WHEN u.id = :viewer THEN true ELSE false END)
        FROM CommunityPost p JOIN p.user u
        WHERE p.community.id = :community AND p.id < :before AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
          AND u.dataNascimento <= :cutoff
          AND EXISTS (SELECT s.id FROM SocialProfile s WHERE s.user.id = u.id AND s.enabled = true)
          AND NOT EXISTS (SELECT b.id FROM SocialUserBlock b WHERE
            (b.blockerId = :viewer AND b.blockedId = u.id) OR (b.blockerId = u.id AND b.blockedId = :viewer))
        ORDER BY p.id DESC
        """)
    List<CommunityPostDTO> buscarVisiveis(@Param("community") Long community, @Param("viewer") Long viewer,
        @Param("before") Long before, @Param("cutoff") LocalDate cutoff, Pageable page);
}

