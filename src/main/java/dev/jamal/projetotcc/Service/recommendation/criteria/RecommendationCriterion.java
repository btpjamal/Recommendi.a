package dev.jamal.projetotcc.Service.recommendation.criteria;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import dev.jamal.projetotcc.Service.recommendation.model.RecommendationContext;

public interface RecommendationCriterion {
    CriterionResult avaliar(Hobby hobby, RecommendationContext contexto);
}
