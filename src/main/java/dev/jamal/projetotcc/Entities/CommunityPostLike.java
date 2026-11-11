package dev.jamal.projetotcc.Entities;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import java.time.Instant;
@Entity @Table(name="community_post_likes") @Getter @Setter
public class CommunityPostLike {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long postId;
 @Column(nullable=false) private Long userId;
 @Column(nullable=false) private Instant createdAt;
}
