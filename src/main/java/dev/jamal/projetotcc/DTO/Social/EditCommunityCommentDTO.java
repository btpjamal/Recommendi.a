package dev.jamal.projetotcc.DTO.Social;
public record EditCommunityCommentDTO(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=1000) String conteudo, @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero Long versao) {}
