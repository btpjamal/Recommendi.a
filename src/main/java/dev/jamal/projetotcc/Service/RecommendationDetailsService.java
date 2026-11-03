package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.AI.AIUserContext;
import dev.jamal.projetotcc.DTO.Recommendation.HobbyRecommendationDTO;
import dev.jamal.projetotcc.DTO.Recommendation.RecommendationDetailsDTO;
import dev.jamal.projetotcc.Entities.Hobby;
import dev.jamal.projetotcc.Entities.PersonalizedPlan;
import dev.jamal.projetotcc.Entities.UserHobby;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Repository.HobbyRepository;
import dev.jamal.projetotcc.Repository.PersonalizedPlanRepository;
import dev.jamal.projetotcc.Repository.UserHobbyRepository;
import dev.jamal.projetotcc.Service.AI.AIContextHashService;
import dev.jamal.projetotcc.Service.AI.AIContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationDetailsService {

    private final RecommendationService recommendationService;

    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final PersonalizedPlanRepository personalizedPlanRepository;

    private final AIContextService aiContextService;
    private final AIContextHashService aiContextHashService;

    @Transactional(readOnly = true)
    public RecommendationDetailsDTO buscarDetalhes(
            Long userId,
            Long hobbyId
    ) {

        Hobby hobby = hobbyRepository.findEligibleById(hobbyId)
                .orElseThrow(() ->
                        new BusinessException("Hobby não encontrado ou indisponível para recomendação.")
                );

        HobbyRecommendationDTO recomendacao =
                recommendationService.calcularRecomendacao(
                        userId,
                        hobbyId
                );

        UserHobby userHobby =
                userHobbyRepository
                        .findByUser_IdAndHobby_Id(userId, hobbyId)
                        .orElse(null);

        String nivelAtual =
                userHobby != null
                        ? userHobby.getNivelAtual().name()
                        : "INICIANTE";

        String statusAtual =
                userHobby != null
                        ? userHobby.getStatusAtual().name()
                        : null;

        RecommendationDetailsDTO.PlanoResumoDTO plano =
                construirResumoPlano(userId, hobbyId);

        return new RecommendationDetailsDTO(
                hobby.getId(),
                hobby.getNome(),
                hobby.getDescricao(),
                hobby.getCategory().getNome(),

                recomendacao.getScore(),
                recomendacao.getMotivos(),
                recomendacao.getAlertas(),

                nivelAtual,
                statusAtual,

                plano
        );
    }

    private RecommendationDetailsDTO.PlanoResumoDTO construirResumoPlano(
            Long userId,
            Long hobbyId
    ) {

        PersonalizedPlan plano =
                personalizedPlanRepository
                        .findByUser_IdAndHobby_Id(userId, hobbyId)
                        .orElse(null);

        if (plano == null) {
            return new RecommendationDetailsDTO.PlanoResumoDTO(
                    false,
                    null,
                    false,
                    null
            );
        }

        AIUserContext contextoAtual =
                aiContextService.construirContexto(
                        userId,
                        hobbyId
                );

        String contextHashAtual =
                aiContextHashService.calcular(contextoAtual);

        boolean stale =
                !plano.getContextHash()
                        .equals(contextHashAtual);

        return new RecommendationDetailsDTO.PlanoResumoDTO(
                true,
                plano.getConteudo(),
                stale,
                plano.getDataAtualizacao()
        );
    }
}