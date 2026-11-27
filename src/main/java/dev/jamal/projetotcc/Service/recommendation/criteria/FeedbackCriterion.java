package dev.jamal.projetotcc.Service.recommendation.criteria;

import dev.jamal.projetotcc.Entities.Hobby;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import dev.jamal.projetotcc.Service.recommendation.model.HobbyRating;
import dev.jamal.projetotcc.Service.recommendation.model.RecommendationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FeedbackCriterion implements RecommendationCriterion {

    @Override
    public CriterionResult avaliar(Hobby h, RecommendationContext contexto) {

        List<HobbyRating> feedbacks = contexto.avaliacoes();

        if (feedbacks == null || feedbacks.isEmpty()) {
            return CriterionResult.of(0, null);
        }

        double pontos = feedbacks.stream()
                .filter(f ->
                        h.getCategory() != null
                                && f.categoryId().equals(h.getCategory().getId())
                )
                .mapToDouble(f -> switch (f.rating()) {
                    case 5 -> 8;
                    case 4 -> 5;
                    case 3 -> 0;
                    case 2 -> -5;
                    case 1 -> -8;
                    default -> 0;
                })
                .sum();

                pontos = Math.max(
                        -15,
                        Math.min(15, pontos));

                if (pontos > 0) {
                    return CriterionResult.of(
                            pontos,
                            "Seu histórico indica interesses por hobbies semelhantes");
                }

                if (pontos < 0) {
                    return CriterionResult.warning(
                            pontos,
                            "Você já avaliou hobbies semelhantes de forma negativa");
                }
        return CriterionResult.of(0, null);
    }
}
