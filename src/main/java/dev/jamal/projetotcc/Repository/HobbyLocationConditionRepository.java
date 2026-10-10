
package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.HobbyLocationCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface HobbyLocationConditionRepository
        extends JpaRepository<HobbyLocationCondition, Long> {
}
