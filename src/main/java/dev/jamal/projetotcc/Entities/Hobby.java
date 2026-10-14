package dev.jamal.projetotcc.Entities;

import dev.jamal.projetotcc.Enum.TipoSocializacao;
import dev.jamal.projetotcc.Enum.*;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "hobby")
public class Hobby {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    private String descricao;
    private String imagemUrl;
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
    @Column(nullable = false)
    private boolean requerEquipamento;
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private HobbyCategory category;
}
