package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.HobbyObjective;
import dev.jamal.projetotcc.Entities.HobbyObjectiveId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HobbyObjectiveRepository
        extends JpaRepository<HobbyObjective, HobbyObjectiveId> {

    List<HobbyObjective> findByHobbyId(Long hobbyId);

    void deleteByHobbyId(Long hobbyId);

    boolean existsByHobbyIdAndPesoBetween(
            Long hobbyId,
            Integer pesoMinimo,
            Integer pesoMaximo
    );
}