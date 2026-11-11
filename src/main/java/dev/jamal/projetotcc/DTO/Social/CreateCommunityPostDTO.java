package dev.jamal.projetotcc.DTO.Social;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record CreateCommunityPostDTO(@Size(max=4000) String conteudo, @NotNull UUID clientId) {}
