package dev.jamal.projetotcc.DTO.Social;
import java.util.List;
public record CommunityMessagePageDTO(List<CommunityMessageDTO> mensagens,
        boolean temMais, Long menorId, Long maiorId) {}
