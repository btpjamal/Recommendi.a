package dev.jamal.projetotcc.security;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.DTO.Feedback.FeedbackCreateRequestDTO;
import dev.jamal.projetotcc.Repository.*;
import dev.jamal.projetotcc.Mapper.FeedbackMapper;
import dev.jamal.projetotcc.Service.*;
import dev.jamal.projetotcc.Service.AI.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnershipSecurityTest {
    private AnnotationConfigApplicationContext context;
    @Configuration @EnableMethodSecurity
    @Import({FeedbackService.class, AIContextService.class, UserAIConfigurationService.class})
    static class Config {
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean HobbyRepository hobbies() { return mock(HobbyRepository.class); }
        @Bean UserHobbyFeedbackRepository feedbacks() { return mock(UserHobbyFeedbackRepository.class); }
        @Bean FeedbackMapper mapper() { return mock(FeedbackMapper.class); }
        @Bean RecommendationProfileRepository profiles() { return mock(RecommendationProfileRepository.class); }
        @Bean UserInterestRepository interests() { return mock(UserInterestRepository.class); }
        @Bean UserObjectiveRepository objectives() { return mock(UserObjectiveRepository.class); }
        @Bean UserHobbyRepository userHobbies() { return mock(UserHobbyRepository.class); }
        @Bean RecommendationService recommendations() { return mock(RecommendationService.class); }
        @Bean UserAIConfigurationRepository keys() { return mock(UserAIConfigurationRepository.class); }
        @Bean APIKeyEncryptionService encryption() { return mock(APIKeyEncryptionService.class); }
    }
    @BeforeEach void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        User user = new User(); user.setId(1L); user.setEmail("a@example.test");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities()));
    }
    @AfterEach void close() { SecurityContextHolder.clearContext(); context.close(); }

    @Test void deniesBothFeedbackOperationsBeforeReadingOrWritingOtherUsersData() {
        FeedbackService service = context.getBean(FeedbackService.class);
        assertThrows(AccessDeniedException.class, () -> service.listarPorUsuario(2L));
        assertThrows(AccessDeniedException.class, () -> service.avaliar(2L,new FeedbackCreateRequestDTO()));
        verifyNoInteractions(context.getBean(UserHobbyFeedbackRepository.class),context.getBean(UserRepository.class),context.getBean(HobbyRepository.class));
    }
    @Test void ownerCanReadAndWriteFeedback() {
        var repo = context.getBean(UserHobbyFeedbackRepository.class);
        when(repo.buscarComHobbyEUsuario(1L)).thenReturn(List.of());
        assertTrue(context.getBean(FeedbackService.class).listarPorUsuario(1L).isEmpty());
        User user = new User(); user.setId(1L);
        Hobby hobby = new Hobby(); hobby.setId(3L);
        var feedback = new UserHobbyFeedback();
        var dto = new FeedbackCreateRequestDTO(); dto.setHobbyId(3L); dto.setRating(4);
        when(context.getBean(UserRepository.class).findById(1L)).thenReturn(Optional.of(user));
        when(context.getBean(HobbyRepository.class).findById(3L)).thenReturn(Optional.of(hobby));
        when(repo.findByUserIdAndHobbyId(1L,3L)).thenReturn(Optional.of(feedback));
        context.getBean(FeedbackService.class).avaliar(1L,dto);
        verify(repo).save(feedback); assertEquals(4,feedback.getRating());
    }
    @Test void deniesForeignContextAndEveryKeyOperation() {
        assertThrows(AccessDeniedException.class, () -> context.getBean(AIContextService.class).construirContexto(2L,3L));
        var keys = context.getBean(UserAIConfigurationService.class);
        assertThrows(AccessDeniedException.class, () -> keys.obterApiKey(2L));
        assertThrows(AccessDeniedException.class, () -> keys.salvar(2L,"fake-key"));
        assertThrows(AccessDeniedException.class, () -> keys.remover(2L));
        assertThrows(AccessDeniedException.class, () -> keys.configurada(2L));
        verifyNoInteractions(context.getBean(UserAIConfigurationRepository.class),context.getBean(UserRepository.class));
    }
    @Test void ownersKeyIsEncryptedAndNeverReturnedByStatus() {
        var repo=context.getBean(UserAIConfigurationRepository.class);
        var encryption=context.getBean(APIKeyEncryptionService.class);
        User user=new User(); user.setId(1L);
        when(context.getBean(UserRepository.class).findById(1L)).thenReturn(Optional.of(user));
        when(repo.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(encryption.encrypt("fake-key")).thenReturn("encrypted");
        context.getBean(UserAIConfigurationService.class).salvar(1L,"fake-key");
        verify(repo).save(argThat(c -> c.getApiKeyEncrypted().equals("encrypted") && c.getUser().getId().equals(1L)));
    }
}
