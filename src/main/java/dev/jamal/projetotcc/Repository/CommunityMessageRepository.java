package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.CommunityMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityMessageRepository
        extends JpaRepository<CommunityMessage, Long> {

    java.util.Optional<CommunityMessage> findByCommunity_IdAndUser_IdAndClientMessageId(
            Long communityId, Long userId, java.util.UUID clientMessageId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT m FROM CommunityMessage m JOIN FETCH m.user
        WHERE m.community.id = :communityId AND m.id < :antesDe
          AND m.user.dataNascimento <= :adultoEm
          AND EXISTS (SELECT s.id FROM SocialProfile s WHERE s.user.id = m.user.id AND s.enabled = true)
          AND NOT EXISTS (SELECT b.id FROM SocialUserBlock b WHERE
              (b.blockerId = :leitor AND b.blockedId = m.user.id)
              OR (b.blockerId = m.user.id AND b.blockedId = :leitor))
        ORDER BY m.id DESC
        """)
    List<CommunityMessage> buscarAnteriores(
            @org.springframework.data.repository.query.Param("communityId") Long communityId,
            @org.springframework.data.repository.query.Param("antesDe") Long antesDe,
            @org.springframework.data.repository.query.Param("leitor") Long leitor,
            @org.springframework.data.repository.query.Param("adultoEm") java.time.LocalDate adultoEm,
            org.springframework.data.domain.Pageable limite);
    @org.springframework.data.jpa.repository.Query("""
        SELECT m FROM CommunityMessage m JOIN FETCH m.user
        WHERE m.community.id = :communityId AND m.id > :aposId
          AND m.user.dataNascimento <= :adultoEm
          AND EXISTS (SELECT s.id FROM SocialProfile s WHERE s.user.id = m.user.id AND s.enabled = true)
          AND NOT EXISTS (SELECT b.id FROM SocialUserBlock b WHERE
              (b.blockerId = :leitor AND b.blockedId = m.user.id)
              OR (b.blockerId = m.user.id AND b.blockedId = :leitor))
        ORDER BY m.id ASC
        """)
    List<CommunityMessage> buscarNovas(
            @org.springframework.data.repository.query.Param("communityId") Long communityId,
            @org.springframework.data.repository.query.Param("aposId") Long aposId,
            @org.springframework.data.repository.query.Param("leitor") Long leitor,
            @org.springframework.data.repository.query.Param("adultoEm") java.time.LocalDate adultoEm,
            org.springframework.data.domain.Pageable limite);

    List<CommunityMessage>
    findByCommunity_IdOrderByDataEnvioAsc(
            Long communityId
    );
}
