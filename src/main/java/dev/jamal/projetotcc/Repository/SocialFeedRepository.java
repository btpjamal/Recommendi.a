package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.CommunityPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SocialFeedRepository extends Repository<CommunityPost, Long> {
    interface FeedRow {
        Long getId();
        boolean getSeguindo();
    }

    @Query("""
        SELECT p FROM CommunityPost p JOIN FETCH p.user JOIN FETCH p.community c JOIN FETCH c.hobby
        WHERE p.id IN :ids
        """)
    List<CommunityPost> carregar(@Param("ids") List<Long> ids);

    @Query(value = """
        SELECT p.id, EXISTS (SELECT 1 FROM social_follows f
            WHERE f.follower_id = :viewer AND f.followed_id = p.user_id) AS seguindo
        FROM community_posts p
        JOIN users u ON u.id = p.user_id
        JOIN social_profiles s ON s.user_id = u.id
        WHERE p.id < :before AND p.deleted_at IS NULL AND p.hidden_at IS NULL
          AND s.enabled = true AND u.data_nascimento <= :cutoff
          AND EXISTS (SELECT 1 FROM community_members m
                      WHERE m.community_id = p.community_id AND m.user_id = :viewer)
          AND NOT EXISTS (SELECT 1 FROM social_user_blocks b WHERE
              (b.blocker_id = :viewer AND b.blocked_id = p.user_id)
              OR (b.blocked_id = :viewer AND b.blocker_id = p.user_id))
          AND (:following = false OR EXISTS (SELECT 1 FROM social_follows f
                       WHERE f.follower_id = :viewer AND f.followed_id = p.user_id))
        ORDER BY p.id DESC
        """, nativeQuery = true)
    List<FeedRow> buscar(@Param("viewer") Long viewer, @Param("before") Long before,
                              @Param("cutoff") LocalDate cutoff, @Param("following") boolean following,
                              Pageable page);
}
