package dev.jamal.projetotcc.Repository;
import dev.jamal.projetotcc.Entities.CommunityReport;import dev.jamal.projetotcc.Enum.CommunityReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.domain.Pageable;import java.util.*;
public interface CommunityReportRepository extends JpaRepository<CommunityReport,Long> {
 Optional<CommunityReport> findByReporterIdAndPost_IdAndCommentIdAndTargetVersion(Long reporter,Long post,Long comment,Long version);
 List<CommunityReport> findByStatusAndIdLessThanOrderByIdDesc(CommunityReportStatus status,Long before,Pageable page);

 interface Evidence {
  String getPublicacao();
  String getComentario();
  String getSha256();
  boolean getPossuiImagem();
 }

 @org.springframework.data.jpa.repository.Modifying
 @org.springframework.data.jpa.repository.Query(value = """
     INSERT INTO community_report_evidence (report_id, post_text, comment_text, image_data, sha256, expires_at)
     SELECT r.id, p.conteudo, c.conteudo, i.dados,
            encode(sha256(convert_to(CAST(jsonb_build_array(r.target_version, p.conteudo, c.conteudo,
                encode(sha256(i.dados), 'hex')) AS text), 'UTF8')), 'hex'),
            r.created_at + interval '90 days'
     FROM community_reports r
     JOIN community_posts p ON p.id = r.post_id
     LEFT JOIN community_comments c ON c.id = r.comment_id
     LEFT JOIN community_post_images i ON i.post_id = p.id AND r.comment_id IS NULL
     WHERE r.id = :id
     """, nativeQuery = true)
 void captureEvidence(@org.springframework.data.repository.query.Param("id") Long id);

 @org.springframework.data.jpa.repository.Query(value = """
     SELECT post_text AS publicacao, comment_text AS comentario, sha256,
            image_data IS NOT NULL AS "possuiImagem"
     FROM community_report_evidence
     WHERE report_id = :id AND expires_at > now()
     """, nativeQuery = true)
 Optional<Evidence> findEvidence(@org.springframework.data.repository.query.Param("id") Long id);

 @org.springframework.data.jpa.repository.Query(value = """
     SELECT image_data FROM community_report_evidence
     WHERE report_id = :id AND expires_at > now() AND image_data IS NOT NULL
     """, nativeQuery = true)
 Optional<byte[]> findEvidenceImage(@org.springframework.data.repository.query.Param("id") Long id);

 @org.springframework.transaction.annotation.Transactional
 @org.springframework.data.jpa.repository.Modifying
 @org.springframework.data.jpa.repository.Query(value = "DELETE FROM community_report_evidence WHERE expires_at <= now()", nativeQuery = true)
 int deleteExpiredEvidence();
}
