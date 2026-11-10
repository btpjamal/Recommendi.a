package dev.jamal.projetotcc.DTO.Social;
import jakarta.validation.constraints.*;
public record EditCommunityPostDTO(@Size(max=4000) String conteudo,
    @NotNull @PositiveOrZero Long versao, boolean removerImagem) {}
