package dev.jamal.projetotcc.DTO.Social;

public record CommunityDTO (
        Long id,
        Long hobbyId,
        String hobbyNome,
        String tipo,
        String estado,
        long membros,
        boolean participando,
        String cidade
){
}
