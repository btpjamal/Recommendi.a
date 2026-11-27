package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.HobbyObjective;
import dev.jamal.projetotcc.Entities.HobbyObjectiveId;
import dev.jamal.projetotcc.Service.recommendation.model.HobbyAssociation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface HobbyObjectiveRepository
        extends JpaRepository<HobbyObjective, HobbyObjectiveId> {

    List<HobbyObjective> findByHobbyId(Long hobbyId);

    @Query("""
            SELECT new dev.jamal.projetotcc.Service.recommendation.model.HobbyAssociation(
                a.id.hobbyId, a.id.objectiveId, a.peso
            )
            FROM HobbyObjective a
            WHERE a.id.hobbyId IN :hobbyIds
            """)
    List<HobbyAssociation> findAssociationsByHobbyIds(@Param("hobbyIds") Collection<Long> hobbyIds);

    void deleteByHobbyId(Long hobbyId);

    boolean existsByHobbyIdAndPesoBetween(
            Long hobbyId,
            Integer pesoMinimo,
            Integer pesoMaximo
    );
}