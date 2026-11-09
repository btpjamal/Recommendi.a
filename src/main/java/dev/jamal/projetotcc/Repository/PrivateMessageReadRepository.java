package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.PrivateMessageRead;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PrivateMessageReadRepository extends JpaRepository<PrivateMessageRead, Long> {
    @Modifying
    @Query(value = """
        INSERT INTO social_private_reads (contact_id, user_id, last_message_id)
        VALUES (:contactId, :userId, :messageId)
        ON CONFLICT (contact_id, user_id) DO UPDATE
        SET last_message_id = GREATEST(social_private_reads.last_message_id, EXCLUDED.last_message_id)
        """, nativeQuery = true)
    void avancar(@Param("contactId") Long contactId, @Param("userId") Long userId, @Param("messageId") Long messageId);
}
