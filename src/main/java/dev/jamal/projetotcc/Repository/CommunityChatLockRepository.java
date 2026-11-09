package dev.jamal.projetotcc.Repository;
import dev.jamal.projetotcc.Entities.Community;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface CommunityChatLockRepository extends Repository<Community, Long> {
    @Query(value = "SELECT id FROM users WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockUser(@Param("id") Long id);
    @Query(value = "SELECT id FROM communities WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockCommunity(@Param("id") Long id);
}
