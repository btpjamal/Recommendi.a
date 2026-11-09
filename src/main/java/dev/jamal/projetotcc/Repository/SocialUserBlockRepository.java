package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.SocialUserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.Set;

public interface SocialUserBlockRepository extends JpaRepository<SocialUserBlock, Long> {
    Optional<SocialUserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    Page<SocialUserBlock> findByBlockerIdOrderByCreatedAtDescIdDesc(Long blockerId, Pageable pageable);

    @Query("SELECT count(b) > 0 FROM SocialUserBlock b WHERE "
            + "(b.blockerId = :a AND b.blockedId = :b) OR (b.blockerId = :b AND b.blockedId = :a)")
    boolean bloqueados(@Param("a") Long a, @Param("b") Long b);

    @Query("SELECT CASE WHEN b.blockerId = :id THEN b.blockedId ELSE b.blockerId END "
            + "FROM SocialUserBlock b WHERE b.blockerId = :id OR b.blockedId = :id")
    Set<Long> buscarIdsIndisponiveis(@Param("id") Long id);
}
