
package dev.jamal.projetotcc.DTO.RecommendationProfile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UserInterestAssociationDTO(

        @NotNull
        Long interestId,

        @NotNull
        @Min(1)
        @Max(3)
        Integer peso

) {}
