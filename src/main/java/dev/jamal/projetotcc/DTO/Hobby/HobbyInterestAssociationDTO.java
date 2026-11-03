
package dev.jamal.projetotcc.DTO.Hobby;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HobbyInterestAssociationDTO(
        @NotNull(message = "O interesse é obrigatório.")
        Long interestId,

        @NotNull(message = "O peso é obrigatório.")
        @Min(value = 1, message = "O peso mínimo é 1.")
        @Max(value = 3, message = "O peso máximo é 3.")
        Integer peso
) {}
