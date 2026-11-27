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


    interface IncrementalPair {
        boolean getAtorElegivel();
        Long getLowId();
        Long getHighId();
    }

    // The initial check preserves the order of failures and excludes outsiders before taking locks.
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM users u JOIN social_profiles p ON p.user_id = u.id AND p.enabled
            WHERE u.id = :actor AND u.data_nascimento <= :cutoff
        ) AS "atorElegivel", c.low_user_id AS "lowId", c.high_user_id AS "highId"
        FROM (SELECT 1) base
        LEFT JOIN social_contacts c ON c.id = :contact AND :actor IN (c.low_user_id, c.high_user_id)
        """, nativeQuery = true)
    IncrementalPair incrementalPair(Long actor, Long contact, LocalDate cutoff);

    interface IncrementalMessage {
        String getAccessStatus();
        Long getMessageId();
        Long getSenderId();
        String getAuthorName();
        String getContent();
        java.time.Instant getSentAt();
    }

    // Called after both user locks. The statement takes a fresh snapshot at READ COMMITTED.
    @Query(value = """
        WITH acesso AS (
            SELECT CASE
                WHEN CAST(:tokenVersion AS bigint) IS NOT NULL AND
                     (:tokenVersion IS DISTINCT FROM a.token_version) THEN 'SESSAO'
                WHEN a.data_nascimento IS NULL OR a.data_nascimento > :cutoff
                     OR NOT COALESCE(ap.enabled, false) THEN 'ATOR'
                WHEN c.id IS NULL THEN 'CONTATO'
                WHEN b.data_nascimento IS NULL OR b.data_nascimento > :cutoff OR NOT COALESCE(bp.enabled, false)
                     OR EXISTS (
                         SELECT 1 FROM social_user_blocks bl
                         WHERE (bl.blocker_id = :low AND bl.blocked_id = :high)
                            OR (bl.blocker_id = :high AND bl.blocked_id = :low)
                     ) THEN 'INDISPONIVEL'
                WHEN c.status <> 'ACEITA' THEN 'PENDENTE'
                ELSE 'PERMITIDO'
            END AS status
            FROM users a
            LEFT JOIN social_profiles ap ON ap.user_id = a.id
            LEFT JOIN social_contacts c ON c.id = :contact AND c.low_user_id = :low AND c.high_user_id = :high
            LEFT JOIN users b ON b.id = CASE WHEN :actor = :low THEN :high ELSE :low END
            LEFT JOIN social_profiles bp ON bp.user_id = b.id
            WHERE a.id = :actor
        )
        SELECT acesso.status AS "accessStatus", m.id AS "messageId", m.sender_id AS "senderId",
               author.nome AS "authorName", m.conteudo AS content, m.created_at AS "sentAt"
        FROM acesso
        LEFT JOIN LATERAL (
            SELECT id, sender_id, conteudo, created_at FROM social_private_messages
            WHERE acesso.status = 'PERMITIDO' AND contact_id = :contact AND id > :cursor
            ORDER BY id ASC LIMIT :maxRows
        ) m ON true
        LEFT JOIN users author ON author.id = m.sender_id
        ORDER BY m.id ASC
        """, nativeQuery = true)
    List<IncrementalMessage> incrementalPage(Long actor, Long contact, Long low, Long high,
            LocalDate cutoff, Long tokenVersion, Long cursor, int maxRows);

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
               (
                   EXISTS (
                       SELECT 1
                       FROM social_profile_photos ph
                       WHERE ph.profile_id = sp.id
                   )
                   AND sp.foto_visibilidade IN ('PARTICIPANTES', 'CONTATOS')
               ) AS "possuiFoto",
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
