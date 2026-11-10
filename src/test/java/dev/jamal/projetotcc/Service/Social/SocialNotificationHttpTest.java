package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.Controllers.*;import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Entities.User;
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
class SocialNotificationHttpTest {
 @Configuration @EnableWebMvc @EnableWebSecurity
 @Import({SecurityConfig.class,PublicAuthRateLimitFilter.class,UserDetailsServiceImpl.class,JwtAuthenticationFilter.class,SocialNotificationController.class,CommunityPostController.class,CommunityInteractionController.class,GlobalExceptionHandler.class})
 static class Config {
  @Bean JwtService jwtService(){return new JwtService(Base64.getEncoder().encodeToString(new byte[32]));}
  @Bean UserRepository users(){return mock(UserRepository.class);}
  @Bean SocialNotificationService notifications(){return mock(SocialNotificationService.class);}
  @Bean CommunityPostService posts(){return mock(CommunityPostService.class);}
  @Bean CommunityPostImageService images(){return mock(CommunityPostImageService.class);}
  @Bean CommunityInteractionService interactions(){return mock(CommunityInteractionService.class);}
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){var f=mock(jakarta.persistence.EntityManagerFactory.class);when(f.getProperties()).thenReturn(Map.of());return f;}
 }
 AnnotationConfigWebApplicationContext context;MockMvc mvc;SocialNotificationService service;String token;final String base="/api/v1/social/notifications";
 @BeforeEach void setup(){context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var u=new User();u.setId(7L);u.setEmail("owner@example.test");var users=context.getBean(UserRepository.class);when(users.findById(7L)).thenReturn(Optional.of(u));when(users.findByEmail(u.getEmail())).thenReturn(Optional.of(u));token="Bearer "+context.getBean(JwtService.class).gerarToken(u);service=context.getBean(SocialNotificationService.class);}
 @AfterEach void cleanup(){SecurityContextHolder.clearContext();context.close();}
 @Test void anonimoNaoListaContaNemMarca()throws Exception{mvc.perform(get(base)).andExpect(status().isUnauthorized());mvc.perform(get(base+"/unread")).andExpect(status().isUnauthorized());mvc.perform(put(base+"/1/read")).andExpect(status().isUnauthorized());verifyNoInteractions(service);}
 @Test void listaUsaUsuarioDoJwtENaoPermiteCache()throws Exception{when(service.listar(7L,null,20)).thenReturn(new SocialNotificationPageDTO(List.of(),false,null,0));mvc.perform(get(base).header("Authorization",token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));verify(service).listar(7L,null,20);}
 @Test void contadorUsaUsuarioDoJwtENaoPermiteCache()throws Exception{when(service.contar(7L)).thenReturn(new SocialNotificationCountDTO(2));mvc.perform(get(base+"/unread").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.naoLidas").value(2)).andExpect(header().string("Cache-Control","no-store"));}
 @Test void leituraSomenteNoEscopoDoTitular()throws Exception{mvc.perform(put(base+"/10/read").header("Authorization",token)).andExpect(status().isNoContent());verify(service).marcarLida(7L,10L);}
 @Test void leituraDeAvisoAlheioOuIndisponivelMantem404()throws Exception{doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(service).marcarLida(7L,99L);mvc.perform(put(base+"/99/read").header("Authorization",token)).andExpect(status().isNotFound());}
 @Test void participacaoInativaMantem403()throws Exception{when(service.contar(7L)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));mvc.perform(get(base+"/unread").header("Authorization",token)).andExpect(status().isForbidden());}
 @Test void cursorMalformadoERecusado()throws Exception{mvc.perform(get(base).param("antesDe","abc").header("Authorization",token)).andExpect(status().isBadRequest());verify(service,never()).listar(anyLong(),any(),anyInt());}
 @Test void publicacaoDeDestinoEAutenticada()throws Exception{String url="/api/v1/community/communities/5/posts/10";mvc.perform(get(url)).andExpect(status().isUnauthorized());mvc.perform(get(url).header("Authorization",token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));verify(context.getBean(CommunityPostService.class)).buscar(5L,7L,10L);}
 @Test void comentarioDeDestinoEAutenticado()throws Exception{String url="/api/v1/community/communities/5/posts/10/comments/20";mvc.perform(get(url)).andExpect(status().isUnauthorized());mvc.perform(get(url).header("Authorization",token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));verify(context.getBean(CommunityInteractionService.class)).buscarComentario(5L,7L,10L,20L);}
}
