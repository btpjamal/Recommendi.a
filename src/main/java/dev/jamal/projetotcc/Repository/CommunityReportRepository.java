package dev.jamal.projetotcc.Repository;
import dev.jamal.projetotcc.Entities.CommunityReport;import dev.jamal.projetotcc.Enum.CommunityReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.domain.Pageable;import java.util.*;
public interface CommunityReportRepository extends JpaRepository<CommunityReport,Long> {
 Optional<CommunityReport> findByReporterIdAndPost_IdAndCommentIdAndTargetVersion(Long reporter,Long post,Long comment,Long version);
 List<CommunityReport> findByStatusAndIdLessThanOrderByIdDesc(CommunityReportStatus status,Long before,Pageable page);
}
