package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.Recommendation.HobbyRecommendationDTO;
import dev.jamal.projetotcc.DTO.Recommendation.RecommendationFeedbackResponseDTO;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.NivelExperiencia;
import dev.jamal.projetotcc.Enum.RecommendationFeedbackType;
import dev.jamal.projetotcc.Enum.UserHobbyStatus;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
@Service
@RequiredArgsConstructor
public class RecommendationFeedbackService {

    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final UserRecommendationFeedbackRepository feedbackRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final RecommendationProfileRepository recommendationProfileRepository;
    private final RecommendationService recommendationService;

    @Transactional
    public void registrar(
            Long userId,
            Long hobbyId,
            RecommendationFeedbackType tipo
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException("Usuário não encontrado.")
                );

        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() ->
                        new BusinessException("Hobby não encontrado.")
                );

        UserRecommendationFeedback feedback =
                feedbackRepository
                        .findByUser_IdAndHobby_Id(userId, hobbyId)
                        .orElseGet(UserRecommendationFeedback::new);

        feedback.setUser(user);
        feedback.setHobby(hobby);
        feedback.setTipo(tipo);
        feedback.setCreatedAt(LocalDateTime.now());

        feedbackRepository.save(feedback);

        if (tipo == RecommendationFeedbackType.INTERESSADO){
            garantirUserHobby(user, hobby);
        }
    }

    private void garantirUserHobby(
            User user,
            Hobby hobby
    ) {
       boolean jaExiste =
               userHobbyRepository
                       .findByUser_IdAndHobby_Id(
                               user.getId(),
                               hobby.getId()
                       )
                       .isPresent();

       if (jaExiste) {
           return;
       }

        RecommendationProfile profile =
                recommendationProfileRepository
                        .findByUserId(user.getId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Perfil de recomendação não encontrado"
                                )
                        );

        UserHobby userHobby = new UserHobby();

        userHobby.setUser(user);
        userHobby.setHobby(hobby);
        userHobby.setNivelAtual(
                NivelExperiencia.INICIANTE
        );
        userHobby.setStatusAtual(
                UserHobbyStatus.INTERESSADO
        );

        userHobbyRepository.save(userHobby);
    }

    @Transactional
    public List<RecommendationFeedbackResponseDTO> listarPorTipo(
            Long userId,
            RecommendationFeedbackType tipo
    ) {

        return feedbackRepository
                .findByUser_IdAndTipo(userId, tipo)
                .stream()
                .map(feedback -> {

                    Hobby hobby = feedback.getHobby();

                    UserHobby userHobby =
                            userHobbyRepository
                                    .findByUser_IdAndHobby_Id(
                                            userId,
                                            hobby.getId()
                                    )
                                    .orElse(null);

                    NivelExperiencia nivelAtual =
                            userHobby != null
                                    ? userHobby.getNivelAtual()
                                    : null;

                    UserHobbyStatus statusAtual =
                            userHobby != null
                                    ? userHobby.getStatusAtual()
                                    : null;

                    HobbyRecommendationDTO recomendacao =
                            recommendationService.calcularRecomendacao(
                                    userId,
                                    feedback.getHobby().getId()
                            );

                    return new RecommendationFeedbackResponseDTO(
                            hobby.getId(),
                            hobby.getNome(),
                            hobby.getDescricao(),
                            hobby.getCategory().getNome(),
                            feedback.getTipo(),
                            nivelAtual,
                            statusAtual,
                            recomendacao.getScore()
                    );
                })
                .toList();
    }

    @Transactional
    public void removerDecisao(
            Long userId,
            Long hobbyId
    ) {
        UserRecommendationFeedback feedback =
                feedbackRepository
                        .findByUser_IdAndHobby_Id(userId, hobbyId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Decisão de recomendação não encontrada."
                                )
                        );

        feedbackRepository.delete(feedback);
    }
}
