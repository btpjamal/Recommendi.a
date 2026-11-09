package dev.jamal.projetotcc.DTO.Social;

import jakarta.validation.constraints.*;

public record SocialContactRequestDTO(
        @NotNull @Positive Long destinatarioId,
        @Size(max = 300) String apresentacao
) {}
