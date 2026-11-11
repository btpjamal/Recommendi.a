package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.Repository.SocialNotificationStore;
import lombok.RequiredArgsConstructor;import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.*;
@Component @RequiredArgsConstructor
public class SocialNotificationRecorder {
 private final SocialNotificationStore store;
 @EventListener @Transactional(propagation=Propagation.MANDATORY)
 public void registrar(SocialNotificationEvent event){store.registrar(event.tipo(),event.sourceId(),event.cycle());}
}
