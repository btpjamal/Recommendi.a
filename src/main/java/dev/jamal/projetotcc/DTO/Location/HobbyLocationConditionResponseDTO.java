
package dev.jamal.projetotcc.DTO.Location;

import dev.jamal.projetotcc.Enum.OsmMatchType;

public record HobbyLocationConditionResponseDTO(

        Long id,
        String osmKey,
        String osmValue,
        OsmMatchType matchType

) {}
