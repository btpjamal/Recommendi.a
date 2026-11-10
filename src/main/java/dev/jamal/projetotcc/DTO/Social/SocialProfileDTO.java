package dev.jamal.projetotcc.DTO.Social;

import java.util.List;

public record SocialProfileDTO(
        Long userId,
        String nome,
        String resumo,
        List<String> interesses,
        List<SocialHobbyDTO> praticando,
        List<SocialHobbyDTO> interessados,
        SocialAffinityDTO afinidade,
        String proximidade,
        String biografia,
        boolean possuiFoto
) {
    public SocialProfileDTO(Long userId, String nome, String resumo,
                            List<String> interesses, List<SocialHobbyDTO> praticando,
                            List<SocialHobbyDTO> interessados, SocialAffinityDTO afinidade, String proximidade) {
        this(userId, nome, resumo, interesses, praticando, interessados, afinidade, proximidade, null, false);
    }
    public SocialProfileDTO(Long userId, String nome, String resumo,
                            List<String> interesses, List<SocialHobbyDTO> praticando,
                            List<SocialHobbyDTO> interessados, SocialAffinityDTO afinidade) {
        this(userId, nome, resumo, interesses, praticando, interessados, afinidade, null);
    }
}
