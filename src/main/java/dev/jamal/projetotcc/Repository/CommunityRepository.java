package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.Community;
import dev.jamal.projetotcc.Enum.CommunityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityRepository
        extends JpaRepository<Community, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = """
        INSERT INTO communities(hobby_id, tipo, estado, cidade, data_criacao)
        VALUES (:hobbyId, :tipo, :estado, :cidade, CURRENT_TIMESTAMP)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@org.springframework.data.repository.query.Param("hobbyId") Long hobbyId,
                       @org.springframework.data.repository.query.Param("tipo") String tipo,
                       @org.springframework.data.repository.query.Param("estado") String estado,
                       @org.springframework.data.repository.query.Param("cidade") String cidade);

    List<Community> findByTipo(
            CommunityType tipo
    );

    List<Community> findByTipoAndEstado(
            CommunityType tipo,
            String estado
    );

    Optional<Community> findByHobby_IdAndTipo(
            Long hobbyId,
            CommunityType tipo
    );

    boolean existsByHobby_IdAndTipo(
            Long hobbyId,
            CommunityType tipo
    );

    Optional<Community> findByHobby_IdAndTipoAndEstado(
            Long hobbyId,
            CommunityType tipo,
            String estado
    );

    boolean existsByHobby_IdAndTipoAndEstado(
            Long hobbyId,
            CommunityType tipo,
            String estado
    );
}