package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.Enum.Role;
import dev.jamal.projetotcc.Controllers.*;import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Entities.User;import dev.jamal.projetotcc.Enum.*;
import dev.jamal.projetotcc.Exception.GlobalExceptionHandler;import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.*;import dev.jamal.projetotcc.config.*;
import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.AfterEach;import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;import org.springframework.http.*;import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.server.ResponseStatusException;import java.util.*;
import static org.mockito.Mockito.*;import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommunityInteractionHttpTest {
 @Configuration @EnableWebMvc @EnableWebSecurity
 @Import({SecurityConfig.class,PublicAuthRateLimitFilter.class,UserDetailsServiceImpl.class,JwtAuthenticationFilter.class,
  CommunityInteractionController.class,CommunityModerationController.class,GlobalExceptionHandler.class})
 static class Config {
  @Bean JwtService jwtService(){return new JwtService(Base64.getEncoder().encodeToString(new byte[32]));}
  @Bean UserRepository users(){return mock(UserRepository.class);}
  @Bean CommunityInteractionService interactions(){return mock(CommunityInteractionService.class);}
  @Bean CommunityModerationService moderation(){return mock(CommunityModerationService.class);}
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){var f=mock(jakarta.persistence.EntityManagerFactory.class);when(f.getProperties()).thenReturn(Map.of());return f;}
 }
 AnnotationConfigWebApplicationContext context;MockMvc mvc;CommunityInteractionService service;CommunityModerationService moderation;
 String userToken,adminToken;String base="/api/v1/community/communities/5/posts/10",admin="/api/v1/admin/community-reports";
 @BeforeEach void setup(){
  context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
  mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  service=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(context.getBean(CommunityInteractionService.class));
  moderation=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(context.getBean(CommunityModerationService.class));
  userToken=token(7L,Role.USER);adminToken=token(9L,Role.ADMIN);
 }
 String token(Long id,Role role){var u=new User();u.setId(id);u.setEmail(id+"@example.test");u.setRole(role);var users=context.getBean(UserRepository.class);when(users.findById(id)).thenReturn(Optional.of(u));when(users.findByEmail(u.getEmail())).thenReturn(Optional.of(u));return "Bearer "+context.getBean(JwtService.class).gerarToken(u);}
 @AfterEach void cleanup(){SecurityContextHolder.clearContext();context.close();}
 @Test void anonimoNaoLeNemInterage()throws Exception{mvc.perform(get(base+"/interactions")).andExpect(status().isUnauthorized());mvc.perform(get(base+"/comments")).andExpect(status().isUnauthorized());mvc.perform(put(base+"/like")).andExpect(status().isUnauthorized());mvc.perform(get(admin)).andExpect(status().isUnauthorized());verifyNoInteractions(service,moderation);}
 @Test void usuarioComumNaoAcessaFilaImagemOuDecisaoAdministrativa()throws Exception{
  mvc.perform(get(admin).header("Authorization",userToken)).andExpect(status().isForbidden());
  mvc.perform(get(admin+"/30/image").header("Authorization",userToken)).andExpect(status().isForbidden());
  mvc.perform(put(admin+"/30/review").header("Authorization",userToken).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());verifyNoInteractions(moderation);
 }
 @Test void curtidasSaoEstadosIdempotentesDoTitular()throws Exception{
  mvc.perform(put(base+"/like").header("Authorization",userToken)).andExpect(status().isOk());verify(service).curtir(5L,7L,10L,true);
  mvc.perform(delete(base+"/like").header("Authorization",userToken)).andExpect(status().isOk());verify(service).curtir(5L,7L,10L,false);
 }
 @Test void comentarioUsaIdentidadeDoJwt()throws Exception{
  var id=UUID.randomUUID();mvc.perform(post(base+"/comments").header("Authorization",userToken).contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"Texto\",\"clientId\":\""+id+"\"}")).andExpect(status().isCreated());
  verify(service).comentar(eq(5L),eq(7L),eq(10L),argThat(dto->dto.clientId().equals(id)));
 }
 @Test void comentarioVazioOuSemIdNaoChegaAoServico()throws Exception{
  mvc.perform(post(base+"/comments").header("Authorization",userToken).contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"\"}")).andExpect(status().isBadRequest());verify(service,never()).comentar(anyLong(),anyLong(),anyLong(),any());
 }
 @Test void exclusaoExigeVersao()throws Exception{
  mvc.perform(delete(base+"/comments/20").header("Authorization",userToken)).andExpect(status().isBadRequest());
  mvc.perform(delete(base+"/comments/20").param("versao","2").header("Authorization",userToken)).andExpect(status().isNoContent());verify(service).excluir(5L,7L,10L,20L,2L);
 }
 @Test void denunciaPublicaNaoRetornaIdentidadeDoDenunciante()throws Exception{
  when(service.denunciar(eq(5L),eq(7L),eq(10L),isNull(),any())).thenReturn(new CommunityReportReceiptDTO(30L));
  mvc.perform(post(base+"/reports").header("Authorization",userToken).contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"SPAM\",\"detalhes\":\"Detalhes\",\"versao\":0}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(30)).andExpect(jsonPath("$.reporterId").doesNotExist());
 }
 @Test void motivoInvalidoERecusado()throws Exception{
  mvc.perform(post(base+"/reports").header("Authorization",userToken).contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"INVALIDO\"}")).andExpect(status().isBadRequest());
 }
 @Test void adminPodeAnalisarComJustificativaEVersoes()throws Exception{
  mvc.perform(put(admin+"/30/review").header("Authorization",adminToken).contentType(MediaType.APPLICATION_JSON)
   .content("{\"decisao\":\"OCULTAR\",\"notas\":\"Justificativa\",\"versaoDenuncia\":0,\"versaoConteudo\":1}")).andExpect(status().isOk());
  verify(moderation).analisar(eq(9L),eq(30L),argThat(dto->dto.versaoConteudo()==1));
 }
 @Test void adminImagemNaoPermiteCache()throws Exception{
  when(moderation.imagem(9L,30L)).thenReturn(new byte[]{1,2});
  mvc.perform(get(admin+"/30/image").header("Authorization",adminToken)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG)).andExpect(header().string("Cache-Control","no-store"));
 }
 @Test void conflitoDoComentarioRetorna409()throws Exception{
  when(service.editar(eq(5L),eq(7L),eq(10L),eq(20L),any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,"Atualize."));
  mvc.perform(put(base+"/comments/20").header("Authorization",userToken).contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"Texto\",\"versao\":0}")).andExpect(status().isConflict());
 }
}
