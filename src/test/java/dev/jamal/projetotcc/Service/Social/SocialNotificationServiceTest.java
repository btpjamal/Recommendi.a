package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Enum.SocialNotificationType;import dev.jamal.projetotcc.Repository.*;
import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Test;import org.springframework.web.server.ResponseStatusException;
import java.time.*;import java.util.*;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
class SocialNotificationServiceTest {
 SocialNotificationStore store;UserRepository users;SocialProfileRepository profiles;SocialNotificationService service;
 @BeforeEach void setup(){store=mock(SocialNotificationStore.class);users=mock(UserRepository.class);profiles=mock(SocialProfileRepository.class);service=new SocialNotificationService(store,users,profiles);nascimento(LocalDate.of(1990,1,1));when(profiles.existsByUser_IdAndEnabledTrue(1L)).thenReturn(true);}
 void nascimento(LocalDate data){when(users.findBirthDateById(1L)).thenReturn(Optional.of(()->data));}
 void status(int code,org.junit.jupiter.api.function.Executable run){assertEquals(code,assertThrows(ResponseStatusException.class,run).getStatusCode().value());}
 SocialNotificationStore.Row row(Long id,SocialNotificationType type,Instant read){return new SocialNotificationStore.Row(id,type,"Pessoa",5L,20L,10L,3L,Instant.now(),read);}
 @Test void participanteDesativadoNaoConsultaNemMarca(){when(profiles.existsByUser_IdAndEnabledTrue(1L)).thenReturn(false);status(403,()->service.contar(1L));status(403,()->service.listar(1L,null,20));status(403,()->service.marcarLida(1L,1L));verifyNoInteractions(store);}
 @Test void menorOuDataAusenteNaoAcessa(){
  nascimento(LocalDate.now().minusYears(17));status(403,()->service.contar(1L));
  when(users.findBirthDateById(1L)).thenReturn(Optional.empty());
  when(users.existsById(1L)).thenReturn(true);
  var semData=assertThrows(ResponseStatusException.class,()->service.contar(1L));
  assertEquals(403,semData.getStatusCode().value());
  assertEquals("Ative sua participação social para acessar notificações.",semData.getReason());
  when(users.existsById(1L)).thenReturn(false);
  var semUsuario=assertThrows(ResponseStatusException.class,()->service.contar(1L));
  assertEquals(403,semUsuario.getStatusCode().value());
  assertNull(semUsuario.getReason());
  verifyNoInteractions(profiles,store);
 }
 @Test void contaSomenteDoUsuarioAutenticado(){
  when(store.contar(1L)).thenReturn(4L);
  assertEquals(4,service.contar(1L).naoLidas());
  var ordem=inOrder(users,profiles,store);
  ordem.verify(users).findBirthDateById(1L);
  ordem.verify(profiles).existsByUser_IdAndEnabledTrue(1L);
  ordem.verify(store).contar(1L);
  verifyNoMoreInteractions(users);
 }
 @Test void paginaTemCursorDoUltimoItemEntregue(){when(store.listar(1L,Long.MAX_VALUE,2)).thenReturn(List.of(row(9L,SocialNotificationType.COMENTARIO_PUBLICACAO,null),row(8L,SocialNotificationType.COMENTARIO_PUBLICACAO,null)));var page=service.listar(1L,null,1);assertTrue(page.temMais());assertEquals(9L,page.proximoCursor());assertEquals(1,page.notificacoes().size());}
 @Test void parametrosInvalidosSaoRecusados(){status(400,()->service.listar(1L,0L,20));status(400,()->service.listar(1L,null,31));status(400,()->service.marcarLida(1L,0L));}
 @Test void avisoDeComentarioNaoCopiaTextoEAbreDestinoExato(){when(store.listar(1L,Long.MAX_VALUE,21)).thenReturn(List.of(row(1L,SocialNotificationType.COMENTARIO_PUBLICACAO,null)));var dto=service.listar(1L,null,20).notificacoes().get(0);assertEquals("Pessoa comentou na sua publicação.",dto.mensagem());assertEquals("/community/communities/3?aba=posts&publicacao=10&comentario=20",dto.destino());assertFalse(dto.lida());}
 @Test void solicitacaoEAceiteTemDestinosDistintos(){when(store.listar(1L,Long.MAX_VALUE,21)).thenReturn(List.of(row(2L,SocialNotificationType.SOLICITACAO_CONTATO,null),row(1L,SocialNotificationType.CONTATO_ACEITO,Instant.now())));var page=service.listar(1L,null,20);assertEquals("/community?aba=conversas&caixa=RECEBIDAS",page.notificacoes().get(0).destino());assertEquals("/community/conversations/5",page.notificacoes().get(1).destino());assertTrue(page.notificacoes().get(1).lida());}
 @Test void leituraDeOutroUsuarioOuAvisoIndisponivelRetorna404(){when(store.marcarLida(1L,100L)).thenReturn(0);status(404,()->service.marcarLida(1L,100L));}
 @Test void repetirLeituraEPermitido(){when(store.marcarLida(1L,2L)).thenReturn(1);service.marcarLida(1L,2L);service.marcarLida(1L,2L);verify(store,times(2)).marcarLida(1L,2L);}
}
