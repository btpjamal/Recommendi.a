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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        List<UserRecommendationFeedback> feedbacks = feedbackRepository.findByUser_IdAndTipo(userId, tipo);
        if (feedbacks.isEmpty()) {
            return List.of();
        }

        RecommendationProfile perfil = recommendationService.buscarPerfil(userId);
        List<Long> ids = feedbacks.stream().map(feedback -> feedback.getHobby().getId()).toList();
        List<Hobby> hobbiesElegiveis = hobbyRepository.findEligibleByIds(ids);
        Map<Long, Hobby> hobbiesPorId = hobbiesElegiveis.stream()
                .collect(Collectors.toMap(Hobby::getId, Function.identity()));
        if (!hobbiesPorId.keySet().containsAll(ids)) {
            throw new BusinessException("Hobby não encontrado ou indisponível para recomendação.");
        }

        var contexto = recommendationService.carregarContexto(userId, perfil, hobbiesElegiveis);
        Map<Long, UserHobby> vinculos = contexto.hobbiesDoUsuario().stream()
                .collect(Collectors.toMap(vinculo -> vinculo.getHobby().getId(), Function.identity()));
        return feedbacks.stream()
                .map(feedback -> {
                    Hobby hobby = hobbiesPorId.get(feedback.getHobby().getId());
                    UserHobby vinculo = vinculos.get(hobby.getId());
                    HobbyRecommendationDTO recomendacao = recommendationService.avaliarHobby(hobby, contexto);

                    return new RecommendationFeedbackResponseDTO(
                            hobby.getId(),
                            hobby.getNome(),
                            hobby.getDescricao(),
                            hobby.getCategory().getNome(),
                            feedback.getTipo(),
                            vinculo == null ? null : vinculo.getNivelAtual(),
                            vinculo == null ? null : vinculo.getStatusAtual(),
                            recomendacao.getScore(),
                            hobby.getImagemUrl()
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

        userHobbyRepository
                .findByUser_IdAndHobby_Id(userId, hobbyId)
                .ifPresent(userHobbyRepository::delete);
    }

}
