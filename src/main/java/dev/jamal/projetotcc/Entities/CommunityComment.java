package dev.jamal.projetotcc.Entities;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="community_comments") @Getter @Setter
public class CommunityComment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="post_id",nullable=false) private CommunityPost post;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(nullable=false) private UUID clientId;
 @Column(nullable=false,length=64) private String requestHash;
 @Column(nullable=false,length=1000) private String conteudo;
 @Column(nullable=false) private Instant createdAt;
 private Instant editedAt;private Instant deletedAt;private Instant hiddenAt;
 @Version private Long version;
}
