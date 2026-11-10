package dev.jamal.projetotcc.DTO.Social;
public record SocialNotificationDTO(Long id, dev.jamal.projetotcc.Enum.SocialNotificationType tipo, String mensagem, String destino, java.time.Instant criadoEm, boolean lida, java.time.Instant lidaEm) {}
