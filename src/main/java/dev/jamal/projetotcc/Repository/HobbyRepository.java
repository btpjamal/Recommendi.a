package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.Hobby;
import dev.jamal.projetotcc.Entities.HobbyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HobbyRepository extends JpaRepository<Hobby, Long> {

    @Query("""
        SELECT h
        FROM Hobby h
        JOIN FETCH h.category       
    """)
    List<Hobby> findAllWithCategory();

    boolean existsByNomeIgnoreCase(String nome);

    Optional<Hobby> findByNomeIgnoreCase(String nome);

    @Query("""
    SELECT h
    FROM Hobby h
    JOIN FETCH h.category
    WHERE h.ativo = true
""")
    List<Hobby> findAllActiveWithCategory();

    Optional<Hobby> findByIdAndAtivoTrue(Long id);
}
