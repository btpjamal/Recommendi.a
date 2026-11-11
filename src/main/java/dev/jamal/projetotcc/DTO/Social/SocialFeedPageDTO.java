package dev.jamal.projetotcc.DTO.Social;

import java.util.List;

public record SocialFeedPageDTO(List<SocialFeedItemDTO> itens, boolean temMais, Long proximoCursor) {}
