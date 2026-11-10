package dev.jamal.projetotcc.DTO.Social;
public record CreateCommunityCommentDTO(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=1000) String conteudo, @jakarta.validation.constraints.NotNull java.util.UUID clientId) {}
