
package dev.jamal.projetotcc.DTO.Location;

import dev.jamal.projetotcc.Enum.HobbyLocationRelevance;

import java.util.List;

public record HobbyLocationRuleResponseDTO(

        Long id,
        Long hobbyId,
        String hobbyNome,
        HobbyLocationRelevance relevancia,
        boolean ativo,
        List<HobbyLocationConditionResponseDTO> conditions

) {}
