package dev.jamal.projetotcc.security;

import dev.jamal.projetotcc.Service.PasswordPolicyService;
import dev.jamal.projetotcc.Service.LoginRateLimitService;
import dev.jamal.projetotcc.Service.AI.PlanGenerationGuard;
import dev.jamal.projetotcc.Exception.*;
import dev.jamal.projetotcc.config.ExternalServicesConfig;
import dev.jamal.projetotcc.config.PublicAuthRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourceSecurityTest {
    @Test void rejectsWeakAndOversizedPasswordsIncludingUtf8Overflow() {
        var policy = new PasswordPolicyService();
        for(String password : new String[]{"123", "123456789012", "aaaaaaaaaaaa", "á".repeat(40)})
            assertThrows(BusinessException.class, () -> policy.validar(password));
        assertDoesNotThrow(() -> policy.validar("Ficticia!Com-Entropia-2026"));
    }
    @Test void accountThrottleIsCaseInsensitive() {
        var limit = new LoginRateLimitService();
        for(int i=0;i<10;i++) limit.check("A@example.test");
        assertThrows(TooManyRequestsException.class, () -> limit.check("a@example.test"));
        assertDoesNotThrow(() -> limit.check("b@example.test"));
    }
    @Test void networkThrottleIgnoresSpoofedForwardingHeaders() throws Exception {
        var filter = new PublicAuthRateLimitFilter();
        for(int i=0;i<61;i++) {
            var request = new MockHttpServletRequest("POST","/api/v1/auth/login");
            request.setServletPath("/api/v1/auth/login"); request.setRemoteAddr("192.0.2.1");
            request.addHeader("X-Forwarded-For", "198.51.100."+i);
            var response = new MockHttpServletResponse();
            filter.doFilter(request,response,new MockFilterChain());
            assertEquals(i==60 ? 429 : 200,response.getStatus());
        }
    }
    @Test void generationRejectsParallelSameUserAndReleasesAfterFailure() {
        var guard=new PlanGenerationGuard();
        guard.run(1L, () -> {
            assertThrows(TooManyRequestsException.class, () -> guard.run(1L, () -> "unexpected"));
            return "ok";
        });
        assertThrows(IllegalStateException.class, () -> guard.run(1L, () -> {throw new IllegalStateException();}));
        assertEquals("released", guard.run(1L, () -> "released"));
        assertThrows(TooManyRequestsException.class, () -> guard.run(1L, () -> "too many"));
    }
    @Test void globalCapacityRejectsWithoutCreatingUnboundedWork() {
        var guard = new PlanGenerationGuard();
        guard.run(1L, () -> guard.run(2L, () -> guard.run(3L, () -> guard.run(4L, () -> {
            assertThrows(TooManyRequestsException.class, () -> guard.run(5L, () -> "unexpected"));
            return "ok";
        }))));
        assertEquals("released",guard.run(6L, () -> "released"));
    }
    @Test void locationExecutorHasABoundedQueue() throws Exception {
        var executor = (ThreadPoolExecutor)new ExternalServicesConfig().locationExecutor();
        var release = new CountDownLatch(1);
        try {
            for(int i=0;i<20;i++) executor.execute(() -> {try {release.await();} catch(InterruptedException ex){Thread.currentThread().interrupt();}});
            assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> {}));
        } finally {release.countDown(); executor.shutdownNow(); assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
    }
}
