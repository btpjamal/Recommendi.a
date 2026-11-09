package dev.jamal.projetotcc.DTO.Social;

public record CommunityDetailsDTO(
        Long id,
        Long hobbyId,
        String hobbyNome,
        String tipo,
        String estado,
        long membros,
        String cidade
) {
}
