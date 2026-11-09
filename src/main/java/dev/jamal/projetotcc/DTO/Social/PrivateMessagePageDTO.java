package dev.jamal.projetotcc.DTO.Social;
import java.util.List;
public record PrivateMessagePageDTO(List<PrivateMessageDTO> mensagens,
        boolean temMais, Long menorId, Long maiorId) {}
