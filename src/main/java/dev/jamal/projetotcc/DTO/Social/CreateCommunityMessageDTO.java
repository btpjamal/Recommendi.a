package dev.jamal.projetotcc.DTO.Social;

public record CreateCommunityMessageDTO(
        String conteudo,
        java.util.UUID clientMessageId
) {
    public CreateCommunityMessageDTO(String conteudo) {
        this(conteudo, null);
    }
}
