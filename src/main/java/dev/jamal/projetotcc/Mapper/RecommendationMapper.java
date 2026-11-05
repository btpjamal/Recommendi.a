package dev.jamal.projetotcc.Mapper;

import dev.jamal.projetotcc.DTO.Recommendation.CriterionScoreDTO;
import dev.jamal.projetotcc.DTO.Recommendation.HobbyRecommendationDTO;
import dev.jamal.projetotcc.Entities.Hobby;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecommendationMapper {
    public HobbyRecommendationDTO toDTO(
            Hobby h,
            Double score,
            List<String> motivos,
            List<String> alertas,
            List<CriterionScoreDTO> criterios
    ) {
        return new HobbyRecommendationDTO(
                h.getId(),
                h.getNome(),
                h.getDescricao(),
                h.getCategory().getNome(),
                Math.max(0, Math.round(score * 10.0) / 10.0),
                motivos,
                alertas,
                criterios,
                h.getImagemUrl());
    }
}
