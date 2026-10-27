package dev.jamal.projetotcc.security;

import dev.jamal.projetotcc.Service.AI.APIKeyEncryptionService;
import dev.jamal.projetotcc.Service.AI.PersonalizedPlanService;
import dev.jamal.projetotcc.Service.AI.*;
import dev.jamal.projetotcc.Service.AI.provider.AIProvider;
import dev.jamal.projetotcc.DTO.AI.AIUserContext;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AIKeySecurityTest {
    @org.junit.jupiter.api.AfterEach void cleanup() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    @Test void encryptionUsesFreshIVAndRejectsTamperingAndWrongKey() {
        var encryption=new APIKeyEncryptionService(Base64.getEncoder().encodeToString(new byte[32]));
        String a=encryption.encrypt("synthetic-api-key");
        String b=encryption.encrypt("synthetic-api-key");
        assertNotEquals(a,b); assertEquals("synthetic-api-key",encryption.decrypt(a));
        byte[] tampered=Base64.getDecoder().decode(a); tampered[tampered.length-1]^=1;
        assertThrows(IllegalStateException.class, () -> encryption.decrypt(Base64.getEncoder().encodeToString(tampered)));
        byte[] other=new byte[32]; Arrays.fill(other,(byte)42);
        var wrongKey=new APIKeyEncryptionService(Base64.getEncoder().encodeToString(other));
        assertThrows(IllegalStateException.class, () -> wrongKey.decrypt(a));
    }

    static class FakeTransactions extends AbstractPlatformTransactionManager {
        protected Object doGetTransaction(){return new Object();}
        protected void doBegin(Object tx,TransactionDefinition definition){}
        protected void doCommit(DefaultTransactionStatus status){}
        protected void doRollback(DefaultTransactionStatus status){}
    }

    @Test void providerRunsOutsideDatabaseTransactionAndSaveRunsInside() {
        var contextService=mock(AIContextService.class);
        var promptBuilder=mock(PromptBuilderService.class);
        var provider=mock(AIProvider.class);
        var plans=mock(PersonalizedPlanRepository.class);
        var users=mock(UserRepository.class);
        var hobbies=mock(HobbyRepository.class);
        var hashes=mock(AIContextHashService.class);
        var keys=mock(UserAIConfigurationService.class);
        var context=mock(AIUserContext.class);
        User user=new User(); user.setId(1L);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(users.findTokenVersionById(1L)).thenReturn(Optional.of(0L));
        when(users.lockTokenVersion(1L)).thenReturn(Optional.of(0L));
        Hobby hobby=new Hobby(); hobby.setId(2L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(hobbies.findById(2L)).thenReturn(Optional.of(hobby));
        when(contextService.construirContexto(1L,2L)).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());return context;
        });
        when(promptBuilder.construirPromptPlanoInicial(context)).thenReturn("synthetic context only");
        when(hashes.calcular(context)).thenReturn("hash");
        when(keys.obterApiKey(1L)).thenReturn("synthetic-key");
        when(provider.generate(anyString(),eq("synthetic-key"))).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());return "synthetic plan";
        });
        when(plans.save(any())).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());return invocation.getArgument(0);
        });
        var service=new PersonalizedPlanService(contextService,promptBuilder,provider,plans,users,hobbies,hashes,keys,
                new PlanGenerationGuard(),new TransactionTemplate(new FakeTransactions()));
        assertEquals("synthetic plan",service.gerarPlano(1L,2L));
        verify(keys).obterApiKey(1L);
        verify(plans).save(argThat(plan -> plan.getUser().getId().equals(1L) && plan.getHobby().getId().equals(2L)));
    }
}
