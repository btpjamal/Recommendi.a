package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.Controllers.CommunityPostController;
import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Exception.GlobalExceptionHandler;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.UserDetailsServiceImpl;
import dev.jamal.projetotcc.Service.JwtService;
import dev.jamal.projetotcc.Service.JwtAuthenticationFilter;
import dev.jamal.projetotcc.config.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommunityPostHttpTest {
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class,PublicAuthRateLimitFilter.class,UserDetailsServiceImpl.class,
        JwtAuthenticationFilter.class,CommunityPostController.class,GlobalExceptionHandler.class})
    static class Config {
        @Bean JwtService jwtService(){return new JwtService(Base64.getEncoder().encodeToString(new byte[32]));}
        @Bean UserRepository users(){return mock(UserRepository.class);}
        @Bean CommunityPostService posts(){return mock(CommunityPostService.class);}
        @Bean CommunityPostImageService images(){return spy(new CommunityPostImageService());}
        // Persistence annotations on the mocked service must resolve without opening a real datasource.
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){
            var factory=mock(jakarta.persistence.EntityManagerFactory.class);
            when(factory.getProperties()).thenReturn(Map.of());return factory;
        }
    }
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    CommunityPostService service;
    String bearer;
    final String base="/api/v1/community/communities/10/posts";
    @BeforeEach void setup() {
        context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());
        context.register(Config.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var user=new User();user.setId(7L);user.setEmail("owner@example.test");
        var users=context.getBean(UserRepository.class);
        when(users.findById(7L)).thenReturn(Optional.of(user));when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        bearer="Bearer "+context.getBean(JwtService.class).gerarToken(user);service=context.getBean(CommunityPostService.class);
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();context.close();}
    MockMultipartFile metadata(String json) {
        return new MockMultipartFile("publicacao","publicacao.json","application/json",json.getBytes(StandardCharsets.UTF_8));
    }
    @Test void anonimoNaoLePublicaOuBaixaImagem() throws Exception {
        mvc.perform(get(base)).andExpect(status().isUnauthorized());
        mvc.perform(get(base+"/1/image")).andExpect(status().isUnauthorized());
        mvc.perform(multipart(base).file(metadata("{}"))).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void envioMultipartUsaTitularDoJwt() throws Exception {
        var id=UUID.randomUUID();
        mvc.perform(multipart(base).file(metadata("{\"conteudo\":\"Texto\",\"clientId\":\""+id+"\"}"))
            .header("Authorization",bearer)).andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"));
        verify(service).criar(eq(10L),eq(7L),argThat(dto->dto.clientId().equals(id)&&dto.conteudo().equals("Texto")),isNull());
    }
    @Test void camposObrigatoriosEUuidInvalidoSaoRecusados() throws Exception {
        mvc.perform(multipart(base).file(metadata("{\"conteudo\":\"Texto\"}")).header("Authorization",bearer)).andExpect(status().isBadRequest());
        mvc.perform(multipart(base).file(metadata("{\"conteudo\":\"Texto\",\"clientId\":\"nao-e-uuid\"}"))
            .header("Authorization",bearer)).andExpect(status().isBadRequest());
        verify(service,never()).criar(anyLong(),anyLong(),any(),any());
    }
    @Test void arquivoDisfarcadoDeImagemNaoChegaAoServicoDePublicacao() throws Exception {
        mvc.perform(multipart(base).file(metadata("{\"conteudo\":\"Texto\",\"clientId\":\""+UUID.randomUUID()+"\"}"))
            .file(new MockMultipartFile("imagem","foto.png","image/png","<svg></svg>".getBytes(StandardCharsets.UTF_8)))
            .header("Authorization",bearer)).andExpect(status().isBadRequest());
        verify(service,never()).criar(anyLong(),anyLong(),any(),any());
    }
    @Test void imagemAutenticadaTemTipoSeguroESemCache() throws Exception {
        when(service.imagem(10L,7L,50L)).thenReturn(new byte[]{1,2});
        mvc.perform(get(base+"/50/image").header("Authorization",bearer)).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_JPEG)).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("X-Content-Type-Options","nosniff"));
    }
    @Test void exclusaoExigeVersaoEUsaAutorAutenticado() throws Exception {
        mvc.perform(delete(base+"/50").param("versao","3").header("Authorization",bearer)).andExpect(status().isNoContent());
        verify(service).excluir(10L,7L,50L,3L);
        mvc.perform(delete(base+"/50").header("Authorization",bearer)).andExpect(status().isBadRequest());
    }
    @Test void conflitoDaEdicaoMantem409NaResposta() throws Exception {
        when(service.editar(eq(10L),eq(7L),eq(50L),any(),isNull()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,"Atualize a publicação."));
        mvc.perform(multipart(HttpMethod.PUT,base+"/50").file(metadata("{\"conteudo\":\"Edição\",\"versao\":0,\"removerImagem\":false}"))
            .header("Authorization",bearer)).andExpect(status().isConflict());
    }
    @Test void participacaoEValidadaAntesDeProcessarImagem() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,"Participação inativa.")).when(service).validarAcesso(10L,7L);
        mvc.perform(multipart(base).file(metadata("{\"conteudo\":\"Texto\",\"clientId\":\""+UUID.randomUUID()+"\"}"))
            .header("Authorization",bearer)).andExpect(status().isForbidden());
        verify(context.getBean(CommunityPostImageService.class),never()).preparar(any());
    }
}
