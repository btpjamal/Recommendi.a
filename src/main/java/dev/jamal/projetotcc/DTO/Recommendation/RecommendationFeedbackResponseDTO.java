package dev.jamal.projetotcc.DTO.Recommendation;

import dev.jamal.projetotcc.Enum.NivelExperiencia;
import dev.jamal.projetotcc.Enum.RecommendationFeedbackType;
import dev.jamal.projetotcc.Enum.UserHobbyStatus;

public record RecommendationFeedbackResponseDTO (

        Long hobbyId,
        String nome,
        String descricao,
        String categoria,
        RecommendationFeedbackType tipo,
        NivelExperiencia nivelAtual,
        UserHobbyStatus statusAtual,
        Double score,
        String imagemUrl
) {}
