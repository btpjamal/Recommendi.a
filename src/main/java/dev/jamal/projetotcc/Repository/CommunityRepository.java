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

    interface CatalogRow {
        Long getId();
        Long getHobbyId();
        String getHobbyNome();
        long getMembros();
        boolean getParticipando();
    }

    @org.springframework.data.jpa.repository.Query(value = """
        SELECT c.id, h.id AS "hobbyId", h.nome AS "hobbyNome",
               count(m.user_id) AS membros,
               coalesce(bool_or(m.user_id = :userId), false) AS participando
        FROM hobby h
        LEFT JOIN communities c ON c.hobby_id = h.id AND c.tipo = :tipo
             AND c.estado IS NOT DISTINCT FROM cast(:estado AS varchar)
             AND c.cidade IS NOT DISTINCT FROM cast(:cidade AS varchar)
        LEFT JOIN community_members m ON m.community_id = c.id
        WHERE h.ativo OR c.id IS NOT NULL
        GROUP BY c.id, h.id, h.nome
        ORDER BY lower(h.nome), h.id
        """, nativeQuery = true)
    List<CatalogRow> catalogo(@org.springframework.data.repository.query.Param("userId") Long userId,
                             @org.springframework.data.repository.query.Param("tipo") String tipo,
                             @org.springframework.data.repository.query.Param("estado") String estado,
                             @org.springframework.data.repository.query.Param("cidade") String cidade);

    Optional<Community> findByHobby_IdAndTipoAndEstadoAndCidade(
            Long hobbyId, CommunityType tipo, String estado, String cidade);

    List<Community> findByTipoAndEstadoAndCidade(CommunityType tipo, String estado, String cidade);

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
