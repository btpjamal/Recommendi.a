
package dev.jamal.projetotcc.Entities;

import dev.jamal.projetotcc.Enum.OsmMatchType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hobby_location_condition")
@Getter
@Setter
@NoArgsConstructor
public class HobbyLocationCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private HobbyLocationRule rule;

    @Column(name = "osm_key", nullable = false, length = 100)
    private String osmKey;

    @Column(name = "osm_value", length = 150)
    private String osmValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OsmMatchType matchType = OsmMatchType.IGUAL;
}
