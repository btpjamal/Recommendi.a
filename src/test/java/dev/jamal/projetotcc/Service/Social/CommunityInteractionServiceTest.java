package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Entities.*;import dev.jamal.projetotcc.Enum.*;import dev.jamal.projetotcc.Repository.*;
import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Test;import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;import org.springframework.data.domain.Pageable;
import java.time.*;import java.util.*;import static org.mockito.Mockito.*;import static org.junit.jupiter.api.Assertions.*;

class CommunityInteractionServiceTest {
 CommunityPostService access;CommunityCommentRepository comments;CommunityPostLikeRepository likes;CommunityReportRepository reports;
 UserRepository users;SocialProfileRepository profiles;SocialUserBlockRepository blocks;CommunityInteractionService service;
 CommunityPost post;User viewer,author;CommunityComment stored;CommunityReport reported;
 @BeforeEach void setup(){
  access=mock(CommunityPostService.class);comments=mock(CommunityCommentRepository.class);likes=mock(CommunityPostLikeRepository.class);reports=mock(CommunityReportRepository.class);
  users=mock(UserRepository.class);profiles=mock(SocialProfileRepository.class);blocks=mock(SocialUserBlockRepository.class);
  service=new CommunityInteractionService(access,comments,likes,reports,users,profiles,blocks,mock(org.springframework.context.ApplicationEventPublisher.class));
        var manager = mock(jakarta.persistence.EntityManager.class);
        var quotaQuery = mock(jakarta.persistence.Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(quotaQuery);
        when(quotaQuery.setParameter(anyString(), any())).thenReturn(quotaQuery);
        when(quotaQuery.getResultList()).thenReturn(List.of(1));
        ReflectionTestUtils.setField(service, "entityManager", manager);
  viewer=new User();viewer.setId(1L);viewer.setNome("Pessoa");viewer.setDataNascimento(LocalDate.of(1990,1,1));
  author=new User();author.setId(2L);author.setNome("Autor");author.setDataNascimento(LocalDate.of(1990,1,1));
  post=new CommunityPost();post.setId(10L);post.setUser(author);post.setVersion(0L);
  when(access.exigirVisivel(eq(5L),eq(1L),eq(10L),anyBoolean())).thenReturn(post);when(users.getReferenceById(1L)).thenReturn(viewer);
  when(comments.saveAndFlush(any())).thenAnswer(call->{CommunityComment c=call.getArgument(0);c.setId(20L);c.setVersion(c.getVersion()==null?0:c.getVersion()+1);stored=c;return c;});
  when(reports.saveAndFlush(any())).thenAnswer(call->{CommunityReport r=call.getArgument(0);r.setId(30L);r.setVersion(0L);reported=r;return r;});
 }
 void status(int code,org.junit.jupiter.api.function.Executable run){assertEquals(code,assertThrows(ResponseStatusException.class,run).getStatusCode().value());}
 CreateCommunityCommentDTO request(String text){return new CreateCommunityCommentDTO(text,UUID.randomUUID());}
 CommunityComment own(){
  var c=new CommunityComment();c.setId(20L);c.setPost(post);c.setUser(viewer);c.setConteudo("Inicial");c.setVersion(0L);c.setCreatedAt(Instant.now());
  when(comments.findByIdAndPost_Id(20L,10L)).thenReturn(Optional.of(c));return c;
 }
 @Test void comentarNormalizaTextoERetornaAutorDoServidor(){var dto=service.comentar(5L,1L,10L,request("  Comentário  "));assertEquals("Comentário",dto.conteudo());assertEquals(1L,dto.autorId());assertTrue(dto.propria());}
 @Test void comentarioVazioLongoOuSemUuidERecusado(){status(400,()->service.comentar(5L,1L,10L,request(" ")));status(400,()->service.comentar(5L,1L,10L,request("x".repeat(1001))));status(400,()->service.comentar(5L,1L,10L,new CreateCommunityCommentDTO("Texto",null)));}
 @Test void reenvioDeComentarioNaoDuplica(){var req=request("Texto");var first=service.comentar(5L,1L,10L,req);when(comments.findByUser_IdAndClientId(1L,req.clientId())).thenReturn(Optional.of(stored));assertEquals(first.id(),service.comentar(5L,1L,10L,req).id());verify(comments,times(1)).saveAndFlush(any());status(409,()->service.comentar(5L,1L,10L,new CreateCommunityCommentDTO("Outro",req.clientId())));}
 @Test void reenvioNaoRestauraComentarioOcultoOuExcluido(){var req=request("Texto");service.comentar(5L,1L,10L,req);when(comments.findByUser_IdAndClientId(1L,req.clientId())).thenReturn(Optional.of(stored));stored.setHiddenAt(Instant.now());status(409,()->service.comentar(5L,1L,10L,req));stored.setHiddenAt(null);stored.setDeletedAt(Instant.now());status(409,()->service.comentar(5L,1L,10L,req));}
 @Test void somenteAutorEditaOuExclui(){var c=own();c.setUser(author);status(404,()->service.editar(5L,1L,10L,20L,new EditCommunityCommentDTO("Ataque",0L)));status(404,()->service.excluir(5L,1L,10L,20L,0L));}
 @Test void versaoAntigaNaoEditaNemExclui(){var c=own();c.setVersion(3L);status(409,()->service.editar(5L,1L,10L,20L,new EditCommunityCommentDTO("Outro",2L)));status(409,()->service.excluir(5L,1L,10L,20L,2L));assertEquals("Inicial",c.getConteudo());}
 @Test void edicaoMarcaDataEExclusaoApagaTexto(){var c=own();var edited=service.editar(5L,1L,10L,20L,new EditCommunityCommentDTO("Novo",0L));assertNotNull(edited.editadoEm());service.excluir(5L,1L,10L,20L,1L);assertEquals("",c.getConteudo());assertNotNull(c.getDeletedAt());}
 @Test void autorNaoContornaOcultacaoEditando(){var c=own();c.setHiddenAt(Instant.now());status(404,()->service.editar(5L,1L,10L,20L,new EditCommunityCommentDTO("Restaurar",0L)));}
 @Test void curtidaPutEIdempotente(){when(likes.existsByPostIdAndUserId(10L,1L)).thenReturn(false);service.curtir(5L,1L,10L,true);verify(likes).saveAndFlush(any());when(likes.existsByPostIdAndUserId(10L,1L)).thenReturn(true);service.curtir(5L,1L,10L,true);verify(likes,times(1)).saveAndFlush(any());}
 @Test void descurtirExpressaEstadoDesejado(){service.curtir(5L,1L,10L,false);service.curtir(5L,1L,10L,false);verify(likes,times(2)).deleteByPostIdAndUserId(10L,1L);verify(likes,never()).saveAndFlush(any());}
 @Test void acessoNegadoImpedeInteracoesAntesDaEscrita(){when(access.exigirVisivel(eq(5L),eq(1L),eq(10L),anyBoolean())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));status(403,()->service.curtir(5L,1L,10L,true));status(403,()->service.comentar(5L,1L,10L,request("Texto")));verifyNoInteractions(likes,comments);}
 @Test void comentarioRecebeCursorDoUltimoItemEntregue(){var c=own();var extra=own();extra.setId(19L);when(comments.visiveis(eq(10L),eq(1L),any(),eq(Long.MAX_VALUE),any(Pageable.class))).thenReturn(List.of(c,extra));var p=service.listar(5L,1L,10L,null,1);assertTrue(p.temMais());assertEquals(20L,p.proximoCursor());status(400,()->service.listar(5L,1L,10L,0L,10));}
 @Test void denunciaEUnicaPorVersao(){var dto=new CreateCommunityReportDTO(CommunityReportReason.SPAM,"Detalhes",0L);service.denunciar(5L,1L,10L,null,dto);assertEquals(CommunityReportStatus.ABERTA,reported.getStatus());assertEquals(1L,reported.getReporterId());when(reports.findByReporterIdAndPost_IdAndCommentIdAndTargetVersion(1L,10L,null,0L)).thenReturn(Optional.of(reported));assertEquals(30L,service.denunciar(5L,1L,10L,null,dto).id());verify(reports,times(1)).saveAndFlush(any());}
 @Test void naoDenunciaConteudoProprio(){post.setUser(viewer);status(400,()->service.denunciar(5L,1L,10L,null,new CreateCommunityReportDTO(CommunityReportReason.OUTRO,"",0L)));}
 @Test void comentarioBloqueadoNaoPodeSerDenunciado(){var c=own();c.setUser(author);when(blocks.bloqueados(1L,2L)).thenReturn(true);status(404,()->service.denunciar(5L,1L,10L,20L,new CreateCommunityReportDTO(CommunityReportReason.SPAM,"",0L)));verifyNoInteractions(reports);}
 @Test void novaVersaoDoConteudoPermiteNovaDenuncia(){var dto=new CreateCommunityReportDTO(CommunityReportReason.SPAM,"",0L);service.denunciar(5L,1L,10L,null,dto);post.setVersion(1L);service.denunciar(5L,1L,10L,null,new CreateCommunityReportDTO(CommunityReportReason.SPAM,"",1L));verify(reports,times(2)).saveAndFlush(any());}
 @Test void denunciaDeVersaoQueMudouExigeAtualizacao(){post.setVersion(1L);status(409,()->service.denunciar(5L,1L,10L,null,new CreateCommunityReportDTO(CommunityReportReason.SPAM,"",0L)));verifyNoInteractions(reports);}
 @Test void novoComentarioEmiteAvisoUmaVezMesmoComReenvio(){var req=request("Texto");service.comentar(5L,1L,10L,req);when(comments.findByUser_IdAndClientId(1L,req.clientId())).thenReturn(Optional.of(stored));service.comentar(5L,1L,10L,req);var publisher=(org.springframework.context.ApplicationEventPublisher)ReflectionTestUtils.getField(service,"publisher");verify(publisher,times(1)).publishEvent(argThat((SocialNotificationEvent e)->e.tipo()==dev.jamal.projetotcc.Enum.SocialNotificationType.COMENTARIO_PUBLICACAO&&e.sourceId().equals(20L)));}
 @Test void proprioComentarioNaoEmiteAviso(){post.setUser(viewer);service.comentar(5L,1L,10L,request("Meu comentário"));verifyNoInteractions(ReflectionTestUtils.getField(service,"publisher"));}
}
