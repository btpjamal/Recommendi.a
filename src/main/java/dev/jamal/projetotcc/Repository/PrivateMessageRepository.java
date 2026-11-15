package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.PrivateMessage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
import java.time.LocalDate;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {
    Optional<PrivateMessage> findByContactIdAndSenderIdAndClientMessageId(Long contactId, Long senderId, UUID clientMessageId);
    boolean existsByIdAndContactId(Long id, Long contactId);

    @Query("SELECT m FROM PrivateMessage m WHERE m.contactId = :id AND m.id < :cursor ORDER BY m.id DESC")
    List<PrivateMessage> anteriores(@Param("id") Long id, @Param("cursor") Long cursor, Pageable limite);
    @Query("SELECT m FROM PrivateMessage m WHERE m.contactId = :id AND m.id > :cursor ORDER BY m.id ASC")
    List<PrivateMessage> novas(@Param("id") Long id, @Param("cursor") Long cursor, Pageable limite);

    interface UnreadCount { Long getContatoId(); long getQuantidade(); }

    interface InboxRow {
        Long getContatoId();
        Long getUserId();
        String getNome();
        boolean getPossuiFoto();
        String getUltimaMensagem();
        boolean getPropria();
        java.time.Instant getAtividade();
        long getNaoLidas();
    }

    @Query(value = """
        SELECT c.id AS "contatoId", u.id AS "userId", u.nome,
               (sp.foto IS NOT NULL AND sp.foto_visibilidade IN ('PARTICIPANTES', 'CONTATOS')) AS "possuiFoto",
               ultima.conteudo AS "ultimaMensagem",
               COALESCE(ultima.sender_id = :userId, false) AS propria,
               COALESCE(ultima.created_at, c.updated_at) AS atividade,
               (SELECT count(*) FROM social_private_messages m
                WHERE m.contact_id = c.id AND m.sender_id <> :userId
                  AND m.id > COALESCE(r.last_message_id, 0)) AS "naoLidas"
        FROM social_contacts c
        JOIN social_profiles sp ON sp.user_id = CASE WHEN c.low_user_id = :userId THEN c.high_user_id ELSE c.low_user_id END
            AND sp.enabled = true
        JOIN users u ON u.id = sp.user_id AND u.data_nascimento <= :maioridade
        LEFT JOIN social_private_reads r ON r.contact_id = c.id AND r.user_id = :userId
        LEFT JOIN LATERAL (
            SELECT m.conteudo, m.sender_id, m.created_at
            FROM social_private_messages m WHERE m.contact_id = c.id ORDER BY m.id DESC LIMIT 1
        ) ultima ON true
        WHERE (c.low_user_id = :userId OR c.high_user_id = :userId) AND c.status = 'ACEITA'
          AND NOT EXISTS (SELECT 1 FROM social_user_blocks b
              WHERE (b.blocker_id = c.low_user_id AND b.blocked_id = c.high_user_id)
                 OR (b.blocker_id = c.high_user_id AND b.blocked_id = c.low_user_id))
        ORDER BY atividade DESC, c.id DESC LIMIT :limite OFFSET :offset
        """, nativeQuery = true)
    List<InboxRow> caixaEntrada(Long userId, LocalDate maioridade, int limite, long offset);

    @Query(value = """
        SELECT count(*) FROM social_contacts c
        WHERE c.recipient_id = :userId AND c.status = 'PENDENTE'
          AND NOT EXISTS (SELECT 1 FROM social_user_blocks b
              WHERE (b.blocker_id = c.low_user_id AND b.blocked_id = c.high_user_id)
                 OR (b.blocker_id = c.high_user_id AND b.blocked_id = c.low_user_id))
        """, nativeQuery = true)
    long solicitacoesPendentes(Long userId);

    @Query(value = """
        SELECT c.id AS "contatoId", count(m.id) AS quantidade
        FROM social_contacts c
        JOIN social_private_messages m ON m.contact_id = c.id AND m.sender_id <> :userId
        JOIN social_profiles sp ON sp.user_id = CASE WHEN c.low_user_id = :userId THEN c.high_user_id ELSE c.low_user_id END
            AND sp.enabled = true
        JOIN users u ON u.id = sp.user_id AND u.data_nascimento <= :maioridade
        LEFT JOIN social_private_reads r ON r.contact_id = c.id AND r.user_id = :userId
        WHERE (c.low_user_id = :userId OR c.high_user_id = :userId)
          AND c.status = 'ACEITA' AND m.id > COALESCE(r.last_message_id, 0)
          AND NOT EXISTS (SELECT 1 FROM social_user_blocks b
              WHERE (b.blocker_id = c.low_user_id AND b.blocked_id = c.high_user_id)
                 OR (b.blocker_id = c.high_user_id AND b.blocked_id = c.low_user_id))
        GROUP BY c.id ORDER BY c.id
        """, nativeQuery = true)
    List<UnreadCount> contarNaoLidas(@Param("userId") Long userId, @Param("maioridade") LocalDate maioridade);
}
