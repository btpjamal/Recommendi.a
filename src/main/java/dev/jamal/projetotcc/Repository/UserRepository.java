package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository <User, Long> {

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.tokenVersion = u.tokenVersion + 1, u.version = u.version + 1 WHERE u.id = :id")
    int revokeTokens(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("SELECT u.tokenVersion FROM User u WHERE u.id = :id")
    Optional<Long> findTokenVersionById(@org.springframework.data.repository.query.Param("id") Long id);

    // Scalar SQL bypasses the Open-in-View entity cache and locks until transaction completion.
    @org.springframework.data.jpa.repository.Query(value = "SELECT token_version FROM users WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockTokenVersion(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
