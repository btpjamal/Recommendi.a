
package dev.jamal.projetotcc.DTO.Location;

import dev.jamal.projetotcc.Enum.OsmMatchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HobbyLocationConditionRequestDTO(

        @NotBlank
        String osmKey,

        String osmValue,

        @NotNull
        OsmMatchType matchType

) {}
