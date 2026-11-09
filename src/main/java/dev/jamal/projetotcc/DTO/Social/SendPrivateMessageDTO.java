package dev.jamal.projetotcc.DTO.Social;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record SendPrivateMessageDTO(@NotBlank @Size(max = 1000) String conteudo,
                                   @NotNull UUID clientMessageId) {}
