package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.Recommendation.HobbyRecommendationDTO;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.*;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Exception.ResourceNotFoundException;
import dev.jamal.projetotcc.Mapper.RecommendationMapper;
import dev.jamal.projetotcc.Repository.*;
import dev.jamal.projetotcc.Service.recommendation.criteria.RecommendationCriterion;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {
    private final HobbyRepository hobbyRepository;
    private final RecommendationProfileRepository recommendationProfileRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserHobbyFeedbackRepository feedbackRepository;
    private final RecommendationMapper mapper;
    private final List<RecommendationCriterion> criterios;
    private final UserRecommendationFeedbackRepository userRecommendationFeedbackRepository;

    public List<HobbyRecommendationDTO> recomendar(Long userId) {
        RecommendationProfile p = recommendationProfileRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Complete o questionário inicial antes de receber recomendações."));
        List<UserInterest> interesses = userInterestRepository.findByUserIdWithInterest(userId);
        List<UserHobbyFeedback> feedbacks = feedbackRepository.buscarComHobbyEUsuario(userId);

        Set<Long> avaliados = feedbacks
                .stream()
                .map(f -> f.getHobby().getId())
                .collect(Collectors.toSet());

        List<UserRecommendationFeedback> feedbacksRecomendacao =
                userRecommendationFeedbackRepository
                        .findByUser_Id(userId);

        Set<Long> hobbiesDecididos = feedbacksRecomendacao.stream()
                .map(feedback -> feedback.getHobby().getId())
                .collect(Collectors.toSet());

        return hobbyRepository.findAllActiveWithCategory()
                .stream()
                .filter(h -> !avaliados.contains(h.getId()))
                .filter(h -> !hobbiesDecididos.contains(h.getId()))
                .map(h ->
                        avaliarHobby(
                                h,
                                p,
                                interesses,
                                feedbacks
                        )
                )
                .sorted(
                        Comparator
                                .comparing(HobbyRecommendationDTO::getScore)
                                .reversed()
                )
                .limit(20)
                .toList();

    }

    public HobbyRecommendationDTO calcularRecomendacao(
            Long userId,
            Long hobbyId
    ) {
        RecommendationProfile perfil =
                recommendationProfileRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Perfil de recomendação não encontrado."
                                )
                        );

        Hobby hobby =
                hobbyRepository
                        .findByIdAndAtivoTrue(hobbyId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Hobby não encontrado."
                                )
                        );

        List<UserInterest> interesses =
                userInterestRepository
                        .findByUserIdWithInterest(userId);

        List<UserHobbyFeedback> feedbacks =
                feedbackRepository
                        .buscarComHobbyEUsuario(userId);

        return avaliarHobby(
                hobby,
                perfil,
                interesses,
                feedbacks
        );
    }

    private HobbyRecommendationDTO avaliarHobby(
            Hobby hobby,
            RecommendationProfile perfil,
            List<UserInterest> interesses,
            List<UserHobbyFeedback> feedbacks
    ) {

        List<CriterionResult> resultados =
                criterios.stream()
                        .map(criterio ->
                                criterio.avaliar(
                                        hobby,
                                        perfil,
                                        interesses,
                                        feedbacks
                                )
                        )
                        .toList();

        double score =
                resultados.stream()
                        .mapToDouble(CriterionResult::pontos)
                        .sum();

        List<String> motivos =
                resultados.stream()
                        .flatMap(resultado ->
                                resultado.motivos().stream()
                        )
                        .distinct()
                        .toList();

        List<String> alertas =
                resultados.stream()
                        .flatMap(resultado ->
                                resultado.alertas().stream()
                        )
                        .distinct()
                        .toList();

        return mapper.toDTO(
                hobby,
                score,
                motivos,
                alertas
        );
    }
}
