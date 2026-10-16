
package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.HobbySuggestion;
import dev.jamal.projetotcc.Enum.HobbySuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

import org.springframework.data.repository.query.Param;

@Repository
public interface HobbySuggestionRepository
        extends JpaRepository<HobbySuggestion, Long> {

    List<HobbySuggestion> findByCreatedByIdOrderByCreatedAtDesc(
            Long userId
    );

    List<HobbySuggestion> findByStatusOrderByCreatedAtAsc(
            HobbySuggestionStatus status
    );

    boolean existsByNomeIgnoreCaseAndStatus(
            String nome,
            HobbySuggestionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT s
    FROM HobbySuggestion s
    WHERE s.id = :id
""")
    Optional<HobbySuggestion> findByIdForUpdate(
            @org.springframework.data.repository.query.Param("id") Long id
    );

    List<HobbySuggestion> findAllByOrderByCreatedAtDesc();

    @Query(value = """
    SELECT EXISTS (
        SELECT 1
        FROM hobby_suggestion s
        WHERE s.status = :status
          AND normalizar_nome_hobby(s.nome) =
              normalizar_nome_hobby(:nome)
    )
    """, nativeQuery = true)
    boolean existsByNomeNormalizadoAndStatus(
            @Param("nome") String nome,
            @Param("status") String status
    );
}
