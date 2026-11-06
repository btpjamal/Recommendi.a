package dev.jamal.projetotcc.Entities;

import dev.jamal.projetotcc.Enum.HobbyImageStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hobby_image_upload")
@Getter
@Setter
public class HobbyImageUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String imagemUrl;

    @Column(nullable = false, unique = true)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HobbyImageStatus status = HobbyImageStatus.PENDENTE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataUpload = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id")
    private Hobby hobby;
}