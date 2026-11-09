package dev.jamal.projetotcc.DTO.Social;
import java.time.Instant;
public record PrivateMessageDTO(Long id, Long userId, String autorNome,
        String conteudo, Instant dataEnvio, boolean propria) {}
