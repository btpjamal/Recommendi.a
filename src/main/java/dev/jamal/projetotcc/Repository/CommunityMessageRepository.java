package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.CommunityMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityMessageRepository
        extends JpaRepository<CommunityMessage, Long> {

    @org.springframework.data.jpa.repository.Query("""
        SELECT m FROM CommunityMessage m JOIN FETCH m.user
        WHERE m.community.id = :communityId AND m.id < :antesDe ORDER BY m.id DESC
        """)
    List<CommunityMessage> buscarAnteriores(
            @org.springframework.data.repository.query.Param("communityId") Long communityId,
            @org.springframework.data.repository.query.Param("antesDe") Long antesDe,
            org.springframework.data.domain.Pageable limite);
    @org.springframework.data.jpa.repository.Query("""
        SELECT m FROM CommunityMessage m JOIN FETCH m.user
        WHERE m.community.id = :communityId AND m.id > :aposId ORDER BY m.id ASC
        """)
    List<CommunityMessage> buscarNovas(
            @org.springframework.data.repository.query.Param("communityId") Long communityId,
            @org.springframework.data.repository.query.Param("aposId") Long aposId,
            org.springframework.data.domain.Pageable limite);

    List<CommunityMessage>
    findByCommunity_IdOrderByDataEnvioAsc(
            Long communityId
    );
}
