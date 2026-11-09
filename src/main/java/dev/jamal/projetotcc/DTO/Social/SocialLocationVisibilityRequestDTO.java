package dev.jamal.projetotcc.DTO.Social;

import jakarta.validation.constraints.NotNull;

public record SocialLocationVisibilityRequestDTO(
        @NotNull Boolean compartilharRegiao
) {}
