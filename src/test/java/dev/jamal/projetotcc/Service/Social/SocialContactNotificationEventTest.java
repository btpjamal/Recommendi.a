package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Entities.*;import dev.jamal.projetotcc.Enum.*;import dev.jamal.projetotcc.Repository.*;
import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Test;import org.springframework.context.ApplicationEventPublisher;import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;import static org.mockito.Mockito.*;import static org.junit.jupiter.api.Assertions.*;
class SocialContactNotificationEventTest {
 SocialContactRepository contacts;SocialContactService service;ApplicationEventPublisher publisher;SocialContact stored;
 @BeforeEach void setup(){
  contacts=mock(SocialContactRepository.class);publisher=mock(ApplicationEventPublisher.class);
  var social=mock(SocialProfileService.class);var locks=mock(SocialContactLockRepository.class);var users=mock(UserRepository.class);
  when(social.elegivel(anyLong())).thenReturn(true);when(social.participa(anyLong())).thenReturn(true);when(users.existsById(anyLong())).thenReturn(true);
  when(locks.lockUser(anyLong())).thenAnswer(i->Optional.of(i.<Long>getArgument(0)));
  service=new SocialContactService(contacts,mock(SocialUserBlockRepository.class),locks,users,mock(SocialProfileRepository.class),social,mock(SocialHubService.class),mock(SocialAffinityService.class),mock(SocialLocationService.class),publisher);
  ReflectionTestUtils.setField(service,"entityManager",mock(jakarta.persistence.EntityManager.class));
  when(contacts.saveAndFlush(any())).thenAnswer(i->{stored=i.getArgument(0);stored.setId(5L);return stored;});
 }
 @Test void solicitarRepetidamenteNaoPublicaDoisEventos(){service.solicitar(1L,new SocialContactRequestDTO(2L,"Olá"));when(contacts.findByLowUserIdAndHighUserId(1L,2L)).thenReturn(Optional.of(stored));service.solicitar(1L,new SocialContactRequestDTO(2L,"Olá"));verify(publisher,times(1)).publishEvent(any(SocialNotificationEvent.class));assertNotNull(stored.getNotificationCycle());}
 @Test void aceitarRepetidamenteNaoPublicaDoisEventos(){service.solicitar(1L,new SocialContactRequestDTO(2L,"Olá"));reset(publisher);when(contacts.findById(5L)).thenReturn(Optional.of(stored));service.responder(2L,5L,true);service.responder(2L,5L,true);verify(publisher).publishEvent(argThat((SocialNotificationEvent e)->e.tipo()==SocialNotificationType.CONTATO_ACEITO));}
 @Test void recusaNaoEmiteAvisoDeAceite(){service.solicitar(1L,new SocialContactRequestDTO(2L,"Olá"));reset(publisher);when(contacts.findById(5L)).thenReturn(Optional.of(stored));service.responder(2L,5L,false);verifyNoInteractions(publisher);}
}
