package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.HobbyInterest;
import dev.jamal.projetotcc.Entities.HobbyInterestId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HobbyInterestRepository
        extends JpaRepository<HobbyInterest, HobbyInterestId> {

    List<HobbyInterest> findByHobbyId(Long hobbyId);

    void deleteByHobbyId(Long hobbyId);

    boolean existsByHobbyIdAndPesoBetween(
            Long hobbyId,
            Integer pesoMinimo,
            Integer pesoMaximo
    );
}
