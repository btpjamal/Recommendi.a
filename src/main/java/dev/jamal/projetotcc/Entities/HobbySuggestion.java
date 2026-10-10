
package dev.jamal.projetotcc.Entities;

import dev.jamal.projetotcc.Enum.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hobby_suggestion")
@Getter
@Setter
@NoArgsConstructor
public class HobbySuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private Double custoEstimado;

    private Double custoRecorrenteEstimado;

    private Integer nivelDificuldade;

    private Double tempoNecessario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSocializacao tipoSocializacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelAtividadeFisica nivelAtividadeFisica;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmbientePreferido ambiente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormatoPreferido formato;

    @Column(nullable = false)
    private boolean requerEquipamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private HobbyCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HobbySuggestionStatus status =
            HobbySuggestionStatus.PENDENTE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_hobby_id")
    private Hobby approvedHobby;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = HobbySuggestionStatus.PENDENTE;
        }
    }
}
