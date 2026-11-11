package dev.jamal.projetotcc.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SocialProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "compartilhar_regiao", nullable = false)
    private boolean compartilharRegiao = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String biografia;
    @JsonIgnore
    @Column(name = "foto", columnDefinition = "bytea")
    private byte[] foto;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private dev.jamal.projetotcc.Enum.SocialVisibility fotoVisibilidade = dev.jamal.projetotcc.Enum.SocialVisibility.PARTICIPANTES;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private dev.jamal.projetotcc.Enum.SocialVisibility biografiaVisibilidade = dev.jamal.projetotcc.Enum.SocialVisibility.PARTICIPANTES;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private dev.jamal.projetotcc.Enum.SocialVisibility interessesVisibilidade = dev.jamal.projetotcc.Enum.SocialVisibility.PARTICIPANTES;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private dev.jamal.projetotcc.Enum.SocialVisibility hobbiesVisibilidade = dev.jamal.projetotcc.Enum.SocialVisibility.PARTICIPANTES;

    @PrePersist
    public void prePersist() {

        LocalDateTime agora = LocalDateTime.now();

        createdAt = agora;
        updatedAt = agora;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
