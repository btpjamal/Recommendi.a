package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.UserHobbyFeedback;
import dev.jamal.projetotcc.Service.recommendation.model.HobbyRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserHobbyFeedbackRepository extends JpaRepository<UserHobbyFeedback, Long> {

    Optional<UserHobbyFeedback> findByUserIdAndHobbyId(Long userId, Long hobbyId);

    @Query("""
        SELECT new dev.jamal.projetotcc.Service.recommendation.model.HobbyRating(h.id, c.id, f.rating)
        FROM UserHobbyFeedback f
        JOIN f.hobby h
        JOIN h.category c
        WHERE f.user.id = :userId
    """)
    List<HobbyRating> buscarAvaliacoesParaRecomendacao(@Param("userId") Long userId);

    @Query("""
        SELECT f
        FROM UserHobbyFeedback f
        JOIN FETCH f.hobby h
        JOIN FETCH h.category    
        JOIN FETCH f.user    
        WHERE f.user.id =:userId        
    """)
    List<UserHobbyFeedback> buscarComHobbyEUsuario(@Param("userId") Long userId);

}
