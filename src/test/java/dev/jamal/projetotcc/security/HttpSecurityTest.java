package dev.jamal.projetotcc.security;

import dev.jamal.projetotcc.Controllers.*;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Enum.Role;
import dev.jamal.projetotcc.Exception.GlobalExceptionHandler;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.*;
import dev.jamal.projetotcc.Service.AI.*;
import dev.jamal.projetotcc.Service.AI.provider.AIProvider;
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
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HttpSecurityTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private User owner;
    private User admin;

    @RestController static class AdminProbe {
        @GetMapping("/api/v1/admin/security-probe") String probe() {return "ok";}
    }
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, PublicAuthRateLimitFilter.class, UserDetailsServiceImpl.class,
            JwtAuthenticationFilter.class, FeedbackController.class, AIContextController.class,
            AuthController.class, UserController.class, GlobalExceptionHandler.class, AdminProbe.class})
    static class Config {
        @Bean JwtService jwtService() {return new JwtService(Base64.getEncoder().encodeToString(new byte[32]));}
        @Bean UserRepository users() {return mock(UserRepository.class);}
        @Bean FeedbackService feedback() {return mock(FeedbackService.class);}
        @Bean AIContextService contextService() {return mock(AIContextService.class);}
        @Bean PromptBuilderService promptBuilder() {return mock(PromptBuilderService.class);}
        @Bean PersonalizedPlanService plans() {return mock(PersonalizedPlanService.class);}
        @Bean AIGeneralPlanContextService generalContext() {return mock(AIGeneralPlanContextService.class);}
        @Bean AIProvider provider() {return mock(AIProvider.class);}
        @Bean GeneralPersonalizedPlanService generalPlans() {return mock(GeneralPersonalizedPlanService.class);}
        @Bean AuthService auth() {return mock(AuthService.class);}
        @Bean UserService userService() {return mock(UserService.class);}
    }
    @BeforeEach void setup() {
        context=new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext()); context.register(Config.class); context.refresh();
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner=new User(); owner.setId(1L); owner.setEmail("owner@example.test");
        admin=new User(); admin.setId(3L); admin.setEmail("admin@example.test"); admin.setRole(Role.ADMIN);
        when(context.getBean(UserRepository.class).findById(1L)).thenReturn(Optional.of(owner));
        when(context.getBean(UserRepository.class).findById(3L)).thenReturn(Optional.of(admin));
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();context.close();}
    private FeedbackService feedbackMock() { return org.springframework.test.util.AopTestUtils.getUltimateTargetObject(context.getBean(FeedbackService.class)); }
    private String bearer(User user){return "Bearer "+context.getBean(JwtService.class).gerarToken(user);}

    @Test void anonymousCannotReadPrivateDataOrLogout() throws Exception {
        mvc.perform(get("/api/v1/users/1/feedbacks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/ai/context/1/2")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
    }
    @Test void ownerCanReadButCannotReadOrWriteOthersFeedback() throws Exception {
        mvc.perform(get("/api/v1/users/1/feedbacks").header("Authorization",bearer(owner))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/2/feedbacks").header("Authorization",bearer(owner))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/users/2/feedbacks").header("Authorization",bearer(owner))
                .contentType("application/json").content("{\"hobbyId\":2,\"rating\":4}")).andExpect(status().isForbidden());
        verify(feedbackMock(),never()).avaliar(eq(2L),any());
    }
    @Test void foreignContextAndPromptAreDeniedBeforeServiceInvocation() throws Exception {
        mvc.perform(get("/api/v1/ai/context/2/4").header("Authorization",bearer(owner))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/ai/prompt/2/4").header("Authorization",bearer(owner))).andExpect(status().isForbidden());
        verifyNoInteractions(context.getBean(AIContextService.class));
    }
    @Test void serverRoleProtectsAdminRoutes() throws Exception {
        mvc.perform(get("/api/v1/admin/security-probe").header("Authorization",bearer(owner))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/security-probe").header("Authorization",bearer(admin))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/2").header("Authorization",bearer(owner))).andExpect(status().isForbidden());
    }
    @Test void logoutInvalidatesCapturedToken() throws Exception {
        String token=bearer(owner);
        when(context.getBean(UserRepository.class).revokeTokens(1L)).thenAnswer(invocation->{owner.setTokenVersion(owner.getTokenVersion()+1);return 1;});
        mvc.perform(post("/api/v1/auth/logout").header("Authorization",token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/1/feedbacks").header("Authorization",token)).andExpect(status().isUnauthorized());
    }
    @Test void unexpectedErrorsDoNotExposeInternalDetails() throws Exception {
        when(feedbackMock().listarPorUsuario(1L)).thenThrow(new RuntimeException("SENSITIVE_TEST_MARKER"));
        mvc.perform(get("/api/v1/users/1/feedbacks").header("Authorization",bearer(owner)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SENSITIVE_TEST_MARKER"))));
    }
    @Test void optimisticConflictsReturn409WithoutInternalDetails() throws Exception {
        when(feedbackMock().listarPorUsuario(1L)).thenThrow(
                new org.springframework.dao.OptimisticLockingFailureException("INTERNAL_TEST_DETAIL"));
        mvc.perform(get("/api/v1/users/1/feedbacks").header("Authorization", bearer(owner)))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("INTERNAL_TEST_DETAIL"))));
    }

    @Test void corsAcceptsConfiguredOriginAndRejectsOtherOrigins() throws Exception {
        mvc.perform(options("/api/v1/users/1/feedbacks").header("Origin","http://localhost:5173")
                .header("Access-Control-Request-Method","GET").header("Access-Control-Request-Headers","Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
        mvc.perform(options("/api/v1/users/1/feedbacks").header("Origin","https://untrusted.example.test")
                .header("Access-Control-Request-Method","GET")).andExpect(status().isForbidden());
    }
}
