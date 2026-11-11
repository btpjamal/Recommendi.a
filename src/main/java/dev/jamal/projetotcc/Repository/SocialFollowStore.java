package dev.jamal.projetotcc.Repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SocialFollowStore {
    private final JdbcTemplate jdbc;

    public boolean seguindo(Long follower, Long followed) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM social_follows WHERE follower_id = ? AND followed_id = ?)
            """, Boolean.class, follower, followed));
    }

    public void seguir(Long follower, Long followed) {
        jdbc.update("""
            INSERT INTO social_follows (follower_id, followed_id) VALUES (?, ?)
            ON CONFLICT (follower_id, followed_id) DO NOTHING
            """, follower, followed);
    }

    public void deixarDeSeguir(Long follower, Long followed) {
        jdbc.update("DELETE FROM social_follows WHERE follower_id = ? AND followed_id = ?", follower, followed);
    }
}
