package dev.jamal.projetotcc.Entities;

import dev.jamal.projetotcc.Enum.SocialContactStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "social_contacts", uniqueConstraints = @UniqueConstraint(
        name = "uk_social_contact_pair", columnNames = {"low_user_id", "high_user_id"}))
@Getter @Setter
public class SocialContact {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long lowUserId;
    @Column(nullable = false) private Long highUserId;
    @Column(nullable = false) private Long requesterId;
    @Column(nullable = false) private Long recipientId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SocialContactStatus status;
    @Column(length = 300) private String apresentacao;
    @Column(nullable = false) private Instant requestedAt;
    @Column(nullable = false) private Instant updatedAt;
    @Column(nullable=false) private java.util.UUID notificationCycle = java.util.UUID.randomUUID();
    @Version private Long version;
}
