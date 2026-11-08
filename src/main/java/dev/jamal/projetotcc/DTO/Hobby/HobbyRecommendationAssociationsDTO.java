package dev.jamal.projetotcc.DTO.Hobby;

import java.util.List;

public record HobbyRecommendationAssociationsDTO(
        List<HobbyInterestAssociationDTO> interestAssociations,
        List<HobbyObjectiveAssociationDTO> objectiveAssociations
) {}