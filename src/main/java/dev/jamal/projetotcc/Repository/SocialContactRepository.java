package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.SocialContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.Optional;

public interface SocialContactRepository extends JpaRepository<SocialContact, Long> {
    Optional<SocialContact> findByLowUserIdAndHighUserId(Long lowUserId, Long highUserId);
    long countByRequesterIdAndRequestedAtGreaterThanEqual(Long requesterId, Instant desde);

    @Query("""
        SELECT c FROM SocialContact c
        WHERE (c.lowUserId = :id OR c.highUserId = :id)
          AND NOT EXISTS (SELECT b.id FROM SocialUserBlock b WHERE
              (b.blockerId = c.lowUserId AND b.blockedId = c.highUserId)
              OR (b.blockerId = c.highUserId AND b.blockedId = c.lowUserId))
          AND ((:caixa = 'RECEBIDAS' AND c.recipientId = :id AND c.status = dev.jamal.projetotcc.Enum.SocialContactStatus.PENDENTE)
            OR (:caixa = 'ENVIADAS' AND c.requesterId = :id AND c.status = dev.jamal.projetotcc.Enum.SocialContactStatus.PENDENTE)
            OR (:caixa = 'ACEITAS' AND c.status = dev.jamal.projetotcc.Enum.SocialContactStatus.ACEITA)
            OR (:caixa = 'HISTORICO' AND c.status IN (
                dev.jamal.projetotcc.Enum.SocialContactStatus.RECUSADA,
                dev.jamal.projetotcc.Enum.SocialContactStatus.CANCELADA,
                dev.jamal.projetotcc.Enum.SocialContactStatus.ENCERRADA)))
        ORDER BY c.updatedAt DESC, c.id DESC
        """)
    Page<SocialContact> buscarCaixa(@Param("id") Long id, @Param("caixa") String caixa, Pageable pageable);
}
