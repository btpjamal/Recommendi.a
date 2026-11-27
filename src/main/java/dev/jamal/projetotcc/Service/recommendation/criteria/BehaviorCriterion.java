package dev.jamal.projetotcc.Service.recommendation.criteria;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.RecommendationFeedbackType;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import dev.jamal.projetotcc.Service.recommendation.model.HobbyAssociation;
import dev.jamal.projetotcc.Service.recommendation.model.RecommendationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BehaviorCriterion implements RecommendationCriterion {

    @Override
    public CriterionResult avaliar(Hobby hobby, RecommendationContext contexto) {

        List<UserRecommendationFeedback> decisoes =
                contexto.decisoes();

        if (decisoes.isEmpty()) {
            return CriterionResult.of(0, null);
        }

        List<HobbyAssociation> interessesCandidato =
                contexto.interessesDoHobby(hobby.getId());

        if (interessesCandidato.isEmpty()) {
            return CriterionResult.of(0, null);
        }

        double sinal = 0;

        for (UserRecommendationFeedback decisao : decisoes) {

            List<HobbyAssociation> interessesHistorico =
                    contexto.interessesDoHobby(decisao.getHobby().getId());

            for (HobbyAssociation historico : interessesHistorico) {

                for (HobbyAssociation candidato : interessesCandidato) {

                    if (!historico.associationId()
                            .equals(candidato.associationId())) {
                        continue;
                    }

                    int pesoHistorico =
                            historico.peso() != null
                                    ? historico.peso()
                                    : 1;

                    int pesoCandidato =
                            candidato.peso() != null
                                    ? candidato.peso()
                                    : 1;

                    double similaridade =
                            pesoHistorico * pesoCandidato;

                    if (decisao.getTipo()
                            == RecommendationFeedbackType.INTERESSADO) {

                        sinal += similaridade * 0.10;

                    } else if (decisao.getTipo()
                            == RecommendationFeedbackType.NAO_INTERESSADO) {

                        sinal -= similaridade * 0.15;
                    }
                }
            }
        }

        // Converte a afinidade comportamental
        // em um modificador do score.
        double pontos = sinal * 5;

        // Impede o comportamento de dominar
        // o perfil declarado pelo usuário.
        pontos = Math.max(
                -15,
                Math.min(15, pontos)
        );

        if (pontos > 0) {
            return CriterionResult.of(
                    pontos,
                    "Seu histórico indica interesse por atividades semelhantes"
            );
        }

        if (pontos < 0) {
            return CriterionResult.warning(
                    pontos,
                    "Seu histórico indica menor interesse por atividades semelhantes"
            );
        }

        return CriterionResult.of(0, null);
    }
}