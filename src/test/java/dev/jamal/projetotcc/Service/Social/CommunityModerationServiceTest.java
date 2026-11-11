package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Entities.*;import dev.jamal.projetotcc.Enum.*;import dev.jamal.projetotcc.Repository.*;
import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Test;import org.springframework.test.util.ReflectionTestUtils;import org.springframework.web.server.ResponseStatusException;
import java.time.*;import java.util.*;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;

class CommunityModerationServiceTest {
 CommunityReportRepository reports;CommunityCommentRepository comments;CommunityPostRepository posts;CommunityPostImageRepository images;
 UserRepository users;CommunityChatLockRepository locks;CommunityModerationService service;CommunityReport report;CommunityPost post;User admin;
 @BeforeEach void setup(){
  reports=mock(CommunityReportRepository.class);comments=mock(CommunityCommentRepository.class);posts=mock(CommunityPostRepository.class);images=mock(CommunityPostImageRepository.class);users=mock(UserRepository.class);locks=mock(CommunityChatLockRepository.class);
  service=new CommunityModerationService(reports,comments,posts,images,users,locks);ReflectionTestUtils.setField(service,"entityManager",mock(jakarta.persistence.EntityManager.class));
  admin=new User();admin.setId(1L);admin.setRole(Role.ADMIN);when(users.findById(1L)).thenReturn(Optional.of(admin));
  var hobby=new Hobby();hobby.setNome("Hobby");var community=new Community();community.setId(5L);community.setHobby(hobby);
  post=new CommunityPost();post.setId(10L);post.setCommunity(community);post.setVersion(0L);post.setConteudo("Texto");
  report=new CommunityReport();report.setId(30L);report.setPost(post);report.setTargetVersion(0L);report.setVersion(0L);report.setStatus(CommunityReportStatus.ABERTA);
  when(reports.findById(30L)).thenReturn(Optional.of(report));when(locks.lockUser(1L)).thenReturn(Optional.of(1L));when(locks.lockCommunity(5L)).thenReturn(Optional.of(5L));
 }
 ReviewCommunityReportDTO review(CommunityModerationDecision d){return new ReviewCommunityReportDTO(d,"Justificativa",0L,0L);}
 void status(int code,org.junit.jupiter.api.function.Executable f){assertEquals(code,assertThrows(ResponseStatusException.class,f).getStatusCode().value());}
 @Test void naoAdminNaoListaNemModeraNemBaixaImagem(){admin.setRole(Role.USER);status(403,()->service.listar(1L,CommunityReportStatus.ABERTA,null,20));status(403,()->service.analisar(1L,30L,review(CommunityModerationDecision.OCULTAR)));status(403,()->service.imagem(1L,30L));verifyNoInteractions(reports);}
 @Test void ocultarPublicacaoRegistraDecisaoSemApagarEvidencia(){service.analisar(1L,30L,review(CommunityModerationDecision.OCULTAR));assertNotNull(post.getHiddenAt());assertEquals("Texto",post.getConteudo());assertEquals(CommunityReportStatus.ANALISADA,report.getStatus());assertEquals(1L,report.getReviewerId());assertNotNull(report.getReviewedAt());verify(posts).saveAndFlush(post);}
 @Test void manterNaoOcultaNemRestauraConteudo(){post.setHiddenAt(Instant.now());service.analisar(1L,30L,review(CommunityModerationDecision.MANTER));assertNotNull(post.getHiddenAt());verifyNoInteractions(posts);}
 @Test void naoReanalisaDenunciaEncerrada(){report.setStatus(CommunityReportStatus.ANALISADA);status(409,()->service.analisar(1L,30L,review(CommunityModerationDecision.MANTER)));}
 @Test void versaoAntigaDeConteudoOuDenunciaImpedeAcao(){post.setVersion(1L);status(409,()->service.analisar(1L,30L,review(CommunityModerationDecision.OCULTAR)));post.setVersion(0L);report.setVersion(1L);status(409,()->service.analisar(1L,30L,review(CommunityModerationDecision.OCULTAR)));assertNull(post.getHiddenAt());}
 @Test void justificativaObrigatoria(){status(400,()->service.analisar(1L,30L,new ReviewCommunityReportDTO(CommunityModerationDecision.MANTER," ",0L,0L)));}
 @Test void comentarioPodeSerOcultadoSemOcultarPublicacao(){var c=new CommunityComment();c.setId(20L);c.setPost(post);c.setVersion(0L);c.setConteudo("Comentário");report.setCommentId(20L);when(comments.findById(20L)).thenReturn(Optional.of(c));service.analisar(1L,30L,review(CommunityModerationDecision.OCULTAR));assertNotNull(c.getHiddenAt());assertNull(post.getHiddenAt());}
 @Test void conteudoExcluidoPodeSerEncerradoMasNaoOcultado(){post.setDeletedAt(Instant.now());status(409,()->service.analisar(1L,30L,review(CommunityModerationDecision.OCULTAR)));service.analisar(1L,30L,review(CommunityModerationDecision.MANTER));assertEquals(CommunityReportStatus.ANALISADA,report.getStatus());}
}
