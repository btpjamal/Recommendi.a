package dev.jamal.projetotcc.DTO.Social;

public record SocialFeedItemDTO(CommunityPostDTO publicacao, String hobbyNome,
                                String tipoComunidade, String estado, String cidade,
                                boolean seguindo, CommunityInteractionDTO interacoes) {}
