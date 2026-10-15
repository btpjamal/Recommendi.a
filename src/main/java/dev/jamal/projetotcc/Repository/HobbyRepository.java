package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.Hobby;
import dev.jamal.projetotcc.Entities.HobbyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

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

    @Query(
            value = """
        SELECT h
        FROM Hobby h
        JOIN FETCH h.category
        WHERE (
            :nome = ''
            OR LOWER(h.nome) LIKE CONCAT('%', LOWER(:nome), '%')
        )
        AND (
            :status = 'TODOS'
            OR (:status = 'ATIVOS' AND h.ativo = true)
            OR (:status = 'INATIVOS' AND h.ativo = false)
        )
        """,
            countQuery = """
        SELECT COUNT(h)
        FROM Hobby h
        WHERE (
            :nome = ''
            OR LOWER(h.nome) LIKE CONCAT('%', LOWER(:nome), '%')
        )
        AND (
            :status = 'TODOS'
            OR (:status = 'ATIVOS' AND h.ativo = true)
            OR (:status = 'INATIVOS' AND h.ativo = false)
        )
        """
    )
    Page<Hobby> buscarPaginado(
            @Param("nome") String nome,
            @Param("status") String status,
            Pageable pageable
    );

    long countByAtivoTrue();

    long countByAtivoFalse();
}
