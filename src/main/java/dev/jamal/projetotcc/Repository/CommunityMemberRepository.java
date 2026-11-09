package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.CommunityMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityMemberRepository
        extends JpaRepository<CommunityMember, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = """
        INSERT INTO community_members (community_id, user_id, data_entrada)
        VALUES (:communityId, :userId, :entrada)
        ON CONFLICT (community_id, user_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@org.springframework.data.repository.query.Param("communityId") Long communityId,
                       @org.springframework.data.repository.query.Param("userId") Long userId,
                       @org.springframework.data.repository.query.Param("entrada") java.time.LocalDateTime entrada);

    boolean existsByCommunity_IdAndUser_Id(
            Long communityId,
            Long userId
    );

    Optional<CommunityMember>
    findByCommunity_IdAndUser_Id(
            Long communityId,
            Long userId
    );

    List<CommunityMember> findByUser_Id(
            Long userId
    );

    long countByCommunity_Id(
            Long communityId
    );
}
