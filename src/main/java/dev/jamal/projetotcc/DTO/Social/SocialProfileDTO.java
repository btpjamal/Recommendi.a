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
        String proximidade
) {
    public SocialProfileDTO(Long userId, String nome, String resumo,
                            List<String> interesses, List<SocialHobbyDTO> praticando,
                            List<SocialHobbyDTO> interessados, SocialAffinityDTO afinidade) {
        this(userId, nome, resumo, interesses, praticando, interessados, afinidade, null);
    }
}
