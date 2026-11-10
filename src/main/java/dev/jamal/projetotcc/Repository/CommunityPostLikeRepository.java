package dev.jamal.projetotcc.Repository;
import dev.jamal.projetotcc.Entities.CommunityPostLike;import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;import java.time.LocalDate;
public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike,Long> {
 boolean existsByPostIdAndUserId(Long post,Long user);
 void deleteByPostIdAndUserId(Long post,Long user);
 @Query("""
  SELECT count(l) FROM CommunityPostLike l, User u WHERE l.userId=u.id AND l.postId=:post
   AND u.dataNascimento <= :cutoff
   AND EXISTS (SELECT s.id FROM SocialProfile s WHERE s.user.id=u.id AND s.enabled=true)
   AND NOT EXISTS (SELECT b.id FROM SocialUserBlock b WHERE
    (b.blockerId=:viewer AND b.blockedId=u.id) OR (b.blockerId=u.id AND b.blockedId=:viewer))
  """)
 long contar(@Param("post") Long post,@Param("viewer") Long viewer,@Param("cutoff") LocalDate cutoff);
}
