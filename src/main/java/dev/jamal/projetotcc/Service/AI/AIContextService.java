package dev.jamal.projetotcc.Service.AI;

import dev.jamal.projetotcc.DTO.AI.AIUserContext;
import dev.jamal.projetotcc.DTO.Recommendation.HobbyRecommendationDTO;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Repository.*;
import dev.jamal.projetotcc.Service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class AIContextService {

    private final UserRepository userRepository;
    private final RecommendationProfileRepository recommendationProfileRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserObjectiveRepository userObjectiveRepository;
    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final RecommendationService recommendationService;

    public AIUserContext construirContexto(
            Long userId,
            Long hobbyId
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado.")
                );

        RecommendationProfile perfil =
                recommendationProfileRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Perfil de recomendação não encontrado."
                                )
                        );

        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() ->
                        new RuntimeException("Hobby não encontrado.")
                );

        List<UserInterest> interesses =
                userInterestRepository
                        .findByUserIdWithInterest(userId);

        List<UserObjective> objetivos =
                userObjectiveRepository
                        .findByUserId(userId);

        UserHobby userHobby =
                userHobbyRepository
                        .findByUser_IdAndHobby_Id(userId, hobbyId)
                        .orElse(null);

        HobbyRecommendationDTO recomendacao =
                recommendationService
                        .calcularRecomendacao(userId, hobbyId);

        return new AIUserContext(

                new AIUserContext.UserContext(
                        calcularIdade(user),
                        perfil.getCidade(),
                        perfil.getEstado()
                ),

                new AIUserContext.ProfileContext(
                        perfil.getTempoDisponivelSemanal(),
                        perfil.getOrcamentoInicial(),

                        perfil.getTipoSocializacao() != null
                                ? perfil.getTipoSocializacao().name()
                                : null,

                        perfil.getNivelAtividadeFisicaDesejada() != null
                                ? perfil.getNivelAtividadeFisicaDesejada().name()
                                : null,

                        perfil.getAmbientePreferido() != null
                                ? perfil.getAmbientePreferido().name()
                                : null,

                        interesses.stream()
                                .map(ui ->
                                        ui.getInterest().getNome()
                                )
                                .toList(),

                        objetivos.stream()
                                .map(uo ->
                                        uo.getObjective().getNome()
                                )
                                .toList()
                ),

                new AIUserContext.HobbyContext(
                        hobby.getId(),
                        hobby.getNome(),
                        hobby.getDescricao(),
                        hobby.getCustoEstimado(),
                        hobby.getTempoNecessario(),
                        hobby.getNivelDificuldade(),

                        hobby.getCategory() != null
                                ? hobby.getCategory().getNome()
                                : null,

                        hobby.getTipoSocializacao() != null
                                ? hobby.getTipoSocializacao().name()
                                : null,

                        hobby.getNivelAtividadeFisica() != null
                                ? hobby.getNivelAtividadeFisica().name()
                                : null,

                        hobby.getAmbiente() != null
                                ? hobby.getAmbiente().name()
                                : null,

                        recomendacao.getScore(),
                        recomendacao.getMotivos(),
                        recomendacao.getAlertas()
                ),

                new AIUserContext.UserHobbyContext(

                        userHobby != null
                                && userHobby.getNivelAtual() != null
                                ? userHobby
                                .getNivelAtual()
                                .name()
                                : "INICIANTE",

                        userHobby != null
                                && userHobby.getStatusAtual() != null
                                ? userHobby
                                .getStatusAtual()
                                .name()
                                : "INTERESSADO"
                )
        );
    }

    private Integer calcularIdade(User user) {

        if (user.getDataNascimento() == null) {
            return null;
        }

        return Period.between(
                user.getDataNascimento(),
                LocalDate.now()
        ).getYears();
    }
}