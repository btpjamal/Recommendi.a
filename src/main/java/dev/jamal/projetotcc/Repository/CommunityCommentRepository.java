package dev.jamal.projetotcc.Repository;
import dev.jamal.projetotcc.Entities.CommunityComment;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;import java.util.*;import java.time.LocalDate;
public interface CommunityCommentRepository extends JpaRepository<CommunityComment,Long> {
 String FILTER="""
  WHERE c.post.id=:post AND c.deletedAt IS NULL AND c.hiddenAt IS NULL
  AND c.user.dataNascimento <= :cutoff
  AND EXISTS (SELECT s.id FROM SocialProfile s WHERE s.user.id=c.user.id AND s.enabled=true)
  AND NOT EXISTS (SELECT b.id FROM SocialUserBlock b WHERE
   (b.blockerId=:viewer AND b.blockedId=c.user.id) OR (b.blockerId=c.user.id AND b.blockedId=:viewer))
  """;
 Optional<CommunityComment> findByUser_IdAndClientId(Long user,UUID id);
 Optional<CommunityComment> findByIdAndPost_Id(Long id,Long post);
 @Query("SELECT c FROM CommunityComment c JOIN FETCH c.user "+FILTER+" AND c.id < :before ORDER BY c.id DESC")
 List<CommunityComment> visiveis(@Param("post") Long post,@Param("viewer") Long viewer,@Param("cutoff") LocalDate cutoff,@Param("before") Long before,Pageable page);
 @Query("SELECT count(c) FROM CommunityComment c "+FILTER)
 long contar(@Param("post") Long post,@Param("viewer") Long viewer,@Param("cutoff") LocalDate cutoff);
}
