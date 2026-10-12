
package dev.jamal.projetotcc.DTO.Location;

import dev.jamal.projetotcc.Enum.HobbyLocationRelevance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record HobbyLocationRuleRequestDTO(

        @NotNull
        HobbyLocationRelevance relevancia,

        boolean ativo,

        @NotEmpty
        List<@Valid HobbyLocationConditionRequestDTO> conditions

) {}
