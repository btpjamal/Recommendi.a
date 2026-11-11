package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.Enum.SocialNotificationType;import java.util.UUID;
public record SocialNotificationEvent(SocialNotificationType tipo, Long sourceId, UUID cycle) {}
