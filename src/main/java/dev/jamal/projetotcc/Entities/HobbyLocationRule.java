
package dev.jamal.projetotcc.Entities;

import dev.jamal.projetotcc.Enum.HobbyLocationRelevance;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hobby_location_rule")
@Getter
@Setter
@NoArgsConstructor
public class HobbyLocationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HobbyLocationRelevance relevancia;

    @Column(nullable = false)
    private boolean ativo = true;

    @OneToMany(
            mappedBy = "rule",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<HobbyLocationCondition> conditions =
            new ArrayList<>();

    public void adicionarCondicao(HobbyLocationCondition condition) {
        condition.setRule(this);
        conditions.add(condition);
    }
}
