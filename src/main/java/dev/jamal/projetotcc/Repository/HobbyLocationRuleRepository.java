
package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.HobbyLocationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HobbyLocationRuleRepository
        extends JpaRepository<HobbyLocationRule, Long> {

    List<HobbyLocationRule> findByHobbyIdAndAtivoTrue(
            Long hobbyId
    );

    List<HobbyLocationRule> findByHobbyId(
            Long hobbyId
    );

    @Query("""
    SELECT DISTINCT r
    FROM HobbyLocationRule r
    LEFT JOIN FETCH r.conditions
    WHERE r.hobby.id = :hobbyId
      AND r.ativo = true
""")
    List<HobbyLocationRule> findActiveWithConditions(
            @Param("hobbyId") Long hobbyId
    );

    @Query("""
    SELECT DISTINCT r
    FROM HobbyLocationRule r
    LEFT JOIN FETCH r.conditions
    WHERE r.hobby.id = :hobbyId
    ORDER BY r.id
""")
    List<HobbyLocationRule> findAllWithConditionsByHobbyId(
            @Param("hobbyId") Long hobbyId
    );
}
