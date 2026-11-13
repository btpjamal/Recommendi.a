package dev.jamal.projetotcc.Repository;
import dev.jamal.projetotcc.Entities.CommunityPostImage;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CommunityPostImageRepository extends JpaRepository<CommunityPostImage,Long> {
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT COALESCE(SUM(octet_length(i.dados)), 0)
            FROM community_post_images i
            JOIN community_posts p ON p.id = i.post_id
            WHERE p.user_id = :userId AND p.id <> :excludedPostId
            """, nativeQuery = true)
    long bytesUsed(@org.springframework.data.repository.query.Param("userId") Long userId,
                   @org.springframework.data.repository.query.Param("excludedPostId") long excludedPostId);
}
