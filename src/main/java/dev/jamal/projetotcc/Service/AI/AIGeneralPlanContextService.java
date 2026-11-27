package dev.jamal.projetotcc.Service.AI;

import dev.jamal.projetotcc.DTO.AI.AIGeneralPlanContext;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.RecommendationFeedbackType;
import dev.jamal.projetotcc.Enum.UserHobbyStatus;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Repository.*;
import dev.jamal.projetotcc.Service.RecommendationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AIGeneralPlanContextService {

    private static final int MAX_HOBBIES_GENERAL_PLAN = 10;

    private final UserRepository userRepository;
    private final RecommendationProfileRepository profileRepository;
    private final UserRecommendationFeedbackRepository feedbackRepository;
    private final RecommendationService recommendationService;

    public AIGeneralPlanContextService(
            UserRepository userRepository,
            RecommendationProfileRepository profileRepository,
            UserRecommendationFeedbackRepository feedbackRepository,
            RecommendationService recommendationService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.feedbackRepository = feedbackRepository;
        this.recommendationService = recommendationService;
    }

    public AIGeneralPlanContext construir(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException("Usuário não encontrado.")
                );

        RecommendationProfile profile =
                profileRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Perfil de recomendação não encontrado."
                                )
                        );

        // Maintain the original encounter order for ties in the plan's status and score sort.
        List<Long> ids = feedbackRepository.findByUser_IdAndTipo(userId, RecommendationFeedbackType.INTERESSADO)
                .stream()
                .map(feedback -> feedback.getHobby().getId())
                .toList();
        if (ids.isEmpty()) {
            throw new BusinessException("Você ainda não possui hobbies marcados como interessado.");
        }

        var lote = recommendationService.pontuarHobbiesElegiveis(userId, profile, ids);
        var contexto = lote.contexto();
        List<String> interesses = contexto.interesses().stream()
                .map(interesse -> interesse.getInterest().getNome())
                .sorted()
                .toList();
        List<String> objetivos = contexto.objetivos().stream()
                .map(objetivo -> contexto.nomesDosObjetivos().get(objetivo.getId().getObjectiveId()))
                .sorted()
                .toList();
        Map<Long, UserHobby> vinculos = contexto.hobbiesDoUsuario().stream()
                .collect(Collectors.toMap(hobby -> hobby.getHobby().getId(), Function.identity()));
        List<AIGeneralPlanContext.HobbySummaryContext> hobbies = lote.pontuacoes().stream()
                .map(pontuacao -> montarHobbyContext(pontuacao, vinculos.get(pontuacao.hobby().getId())))
                .sorted(Comparator.comparingInt(this::prioridadeStatus)
                        .thenComparing(AIGeneralPlanContext.HobbySummaryContext::score,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_HOBBIES_GENERAL_PLAN)
                .toList();

        return new AIGeneralPlanContext(

                new AIGeneralPlanContext.UserContext(
                        calcularIdade(user.getDataNascimento()),
                        profile.getCidade(),
                        profile.getEstado()
                ),

                new AIGeneralPlanContext.ProfileContext(
                        profile.getTempoDisponivelSemanal(),
                        profile.getOrcamentoInicial(),
                        profile.getTipoSocializacao().name(),
                        profile.getNivelAtividadeFisicaDesejada().name(),
                        profile.getAmbientePreferido().name(),
                        interesses,
                        objetivos
                ),

                hobbies
        );
    }

    private AIGeneralPlanContext.HobbySummaryContext montarHobbyContext(
            RecommendationService.PontuacaoHobby pontuacao,
            UserHobby userHobby
    ) {
        Hobby hobby = pontuacao.hobby();
        return new AIGeneralPlanContext.HobbySummaryContext(
                hobby.getId(),
                hobby.getNome(),
                hobby.getCategory().getNome(),
                hobby.getTempoNecessario(),
                hobby.getNivelAtividadeFisica().name(),
                hobby.getAmbiente().name(),
                pontuacao.score(),
                userHobby != null
                        ? userHobby.getNivelAtual().name()
                        : null,
                userHobby != null
                        ? userHobby.getStatusAtual().name()
                        : null
        );
    }

    private int prioridadeStatus(
            AIGeneralPlanContext.HobbySummaryContext hobby
    ) {

        if (UserHobbyStatus.PRATICANDO.name()
                .equals(hobby.statusAtual())) {
            return 0;
        }

        if (UserHobbyStatus.INTERESSADO.name()
                .equals(hobby.statusAtual())) {
            return 1;
        }

        if (UserHobbyStatus.PAUSADO.name()
                .equals(hobby.statusAtual())) {
            return 2;
        }

        return 3;
    }

    private Integer calcularIdade(LocalDate dataNascimento) {

        if (dataNascimento == null) {
            return null;
        }

        return Period.between(
                dataNascimento,
                LocalDate.now()
        ).getYears();
    }
}