package dev.jamal.projetotcc.Service.Social;

import jakarta.persistence.EntityManager;

/** Consumes quota in the same transaction as the write, after retry detection. */
final class SocialWriteQuota {
    enum Action {
        COMMUNITY_MESSAGE(120), PRIVATE_MESSAGE(120), POST(30), COMMENT(60), REPORT(10), POST_IMAGE_PROCESSING(12);

        final int limit;

        Action(int limit) {
            this.limit = limit;
        }
    }

    private SocialWriteQuota() {}

    static void consume(EntityManager entityManager, Long userId, Action action) {
        // One bounded row per user/action. PostgreSQL serializes conflicting updates.
        var accepted = entityManager.createNativeQuery("""
                INSERT INTO social_write_quotas (user_id, action, started_at, used)
                VALUES (:user, :action, clock_timestamp(), 1)
                ON CONFLICT (user_id, action) DO UPDATE
                SET started_at = CASE
                        WHEN social_write_quotas.started_at <= clock_timestamp() - interval '1 minute'
                        THEN clock_timestamp() ELSE social_write_quotas.started_at END,
                    used = CASE
                        WHEN social_write_quotas.started_at <= clock_timestamp() - interval '1 minute'
                        THEN 1 ELSE social_write_quotas.used + 1 END
                WHERE social_write_quotas.used < :maximum
                   OR social_write_quotas.started_at <= clock_timestamp() - interval '1 minute'
                RETURNING used
                """)
                .setParameter("user", userId)
                .setParameter("action", action.name())
                .setParameter("maximum", action.limit)
                .getResultList();
        if (accepted.isEmpty()) {
            throw new SocialQuotaExceededException();
        }
    }
}
