package dev.jamal.projetotcc.DTO.Social;
import dev.jamal.projetotcc.Enum.SocialVisibility;
import jakarta.validation.constraints.*;
public record SocialProfileEditDTO(
 @Size(max=500) String biografia,
 @NotNull SocialVisibility fotoVisibilidade,
 @NotNull SocialVisibility biografiaVisibilidade,
 @NotNull SocialVisibility interessesVisibilidade,
 @NotNull SocialVisibility hobbiesVisibilidade) {}
