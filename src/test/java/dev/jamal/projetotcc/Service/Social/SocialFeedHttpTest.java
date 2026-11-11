package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.Controllers.SocialFeedController;
import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.JwtAuthenticationFilter;
import dev.jamal.projetotcc.Service.JwtService;
import dev.jamal.projetotcc.Service.UserDetailsServiceImpl;
import dev.jamal.projetotcc.config.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SocialFeedHttpTest {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, PublicAuthRateLimitFilter.class, UserDetailsServiceImpl.class,
            JwtAuthenticationFilter.class, SocialFeedController.class})
    static class Config {
        @Bean JwtService jwtService() { return new JwtService(Base64.getEncoder().encodeToString(new byte[32])); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean SocialFeedService feed() { return mock(SocialFeedService.class); }
        @Bean SocialFollowService follows() { return mock(SocialFollowService.class); }
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory() {
            var factory = mock(jakarta.persistence.EntityManagerFactory.class);
            when(factory.getProperties()).thenReturn(Map.of());
            return factory;
        }
    }

    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    SocialFeedService feed;
    SocialFollowService follows;
    String token;
    final String base = "/api/v1/community";

    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var user = new User();
        user.setId(7L);
        user.setEmail("owner@example.test");
        var users = context.getBean(UserRepository.class);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        token = "Bearer " + context.getBean(JwtService.class).gerarToken(user);
        feed = context.getBean(SocialFeedService.class);
        follows = context.getBean(SocialFollowService.class);
    }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test void anonimoNaoAcessaFeedNemRelacionamentos() throws Exception {
        mvc.perform(get(base + "/feed")).andExpect(status().isUnauthorized());
        mvc.perform(get(base + "/users/8/follow")).andExpect(status().isUnauthorized());
        mvc.perform(put(base + "/users/8/follow")).andExpect(status().isUnauthorized());
        mvc.perform(delete(base + "/users/8/follow")).andExpect(status().isUnauthorized());
        verifyNoInteractions(feed, follows);
    }

    @Test void feedUsaPrincipalERecusaCache() throws Exception {
        when(feed.listar(7L, false, null, 20)).thenReturn(new SocialFeedPageDTO(List.of(), false, null));
        mvc.perform(get(base + "/feed").header("Authorization", token).param("userId", "99"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.itens").isArray());
        verify(feed).listar(7L, false, null, 20);
    }

    @Test void filtroECursorSaoEncaminhados() throws Exception {
        mvc.perform(get(base + "/feed").header("Authorization", token)
                .param("seguindo", "true").param("antesDe", "40").param("limite", "10"))
                .andExpect(status().isOk());
        verify(feed).listar(7L, true, 40L, 10);
    }

    @Test void seguirEDeseguirUsamPrincipalMesmoComAutorForjado() throws Exception {
        when(follows.alterar(7L, 8L, true)).thenReturn(new SocialFollowDTO(8L, true));
        when(follows.alterar(7L, 8L, false)).thenReturn(new SocialFollowDTO(8L, false));
        mvc.perform(put(base + "/users/8/follow").header("Authorization", token).param("actor", "99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.seguindo").value(true));
        mvc.perform(delete(base + "/users/8/follow").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.seguindo").value(false));
        verify(follows).alterar(7L, 8L, true);
        verify(follows).alterar(7L, 8L, false);
    }

    @Test void consultaEAutenticadaESemCache() throws Exception {
        when(follows.consultar(7L, 8L)).thenReturn(new SocialFollowDTO(8L, true));
        mvc.perform(get(base + "/users/8/follow").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        verify(follows).consultar(7L, 8L);
    }

    @Test void participacaoInativaRetorna403() throws Exception {
        when(feed.listar(7L, false, null, 20)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        mvc.perform(get(base + "/feed").header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test void destinoBloqueadoMantem404() throws Exception {
        when(follows.alterar(7L, 8L, true)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(put(base + "/users/8/follow").header("Authorization", token)).andExpect(status().isNotFound());
    }

    @Test void cursorMalformadoRetorna400() throws Exception {
        mvc.perform(get(base + "/feed").header("Authorization", token).param("antesDe", "abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(feed);
    }
}
