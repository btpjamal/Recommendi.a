package dev.jamal.projetotcc.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "social_user_blocks", uniqueConstraints = @UniqueConstraint(
        name = "uk_social_user_block", columnNames = {"blocker_id", "blocked_id"}))
@Getter @Setter
public class SocialUserBlock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long blockerId;
    @Column(nullable = false) private Long blockedId;
    @Column(nullable = false) private Instant createdAt;
}
