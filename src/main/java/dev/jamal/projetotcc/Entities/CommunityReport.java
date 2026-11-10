package dev.jamal.projetotcc.Entities;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import java.time.Instant;
import dev.jamal.projetotcc.Enum.*;
@Entity @Table(name="community_reports") @Getter @Setter
public class CommunityReport {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="post_id",nullable=false) private CommunityPost post;
 private Long commentId;
 @Column(nullable=false) private Long reporterId;
 @Column(nullable=false) private Long targetVersion;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private CommunityReportReason reason;
 @Column(length=500) private String detalhes;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private CommunityReportStatus status;
 @Enumerated(EnumType.STRING) @Column(length=20) private CommunityModerationDecision decision;
 @Column(length=1000) private String notas;
 private Long reviewerId;
 @Column(nullable=false) private Instant createdAt;
 private Instant reviewedAt;
 @Version private Long version;
}
