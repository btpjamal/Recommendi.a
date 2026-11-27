package dev.jamal.projetotcc.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_profile_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SocialProfilePhoto {

    @Id
    @Column(name = "profile_id")
    private Long profileId;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] foto;

    @Column(columnDefinition = "bytea")
    private byte[] miniatura;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void atualizarData() {
        updatedAt = LocalDateTime.now();
    }
}