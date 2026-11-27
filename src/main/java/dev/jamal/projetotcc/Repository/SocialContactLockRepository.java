package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.User;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface SocialContactLockRepository extends Repository<User, Long> {
    @Query(value = "SELECT id FROM users WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockUser(@Param("id") Long id);
    @Query(value = "SELECT id FROM users WHERE id IN (:low, :high) ORDER BY id FOR UPDATE", nativeQuery = true)
    List<Long> lockPair(@Param("low") Long low, @Param("high") Long high);
}
