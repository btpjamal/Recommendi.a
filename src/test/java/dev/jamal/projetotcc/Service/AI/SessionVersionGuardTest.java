package dev.jamal.projetotcc.Service.AI;

import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionVersionGuardTest {
    private final UserRepository users = mock(UserRepository.class);
    @BeforeEach void authenticate() {
        User principal = new User(); principal.setId(1L); principal.setTokenVersion(4);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void rejectedIfRevokedBeforeSnapshot() {
        when(users.findTokenVersionById(1L)).thenReturn(Optional.of(5L));
        assertThrows(CredentialsExpiredException.class, () -> SessionVersionGuard.snapshot(users, 1L));
    }
    @Test void snapshotUsesAuthenticatedVersion() {
        when(users.findTokenVersionById(1L)).thenReturn(Optional.of(4L));
        assertEquals(4L, SessionVersionGuard.snapshot(users, 1L));
    }
    @Test void finalCheckRejectsRevokedVersionEvenWithStaleEntity() {
        User stale = new User(); stale.setTokenVersion(4);
        when(users.findById(1L)).thenReturn(Optional.of(stale));
        when(users.lockTokenVersion(1L)).thenReturn(Optional.of(5L));
        assertThrows(CredentialsExpiredException.class, () -> new TransactionTemplate(new FakeTransactions())
                .execute(status -> {SessionVersionGuard.lockAndValidate(users, 1L, 4); return null;}));
        verify(users, never()).findById(1L);
    }
    @Test void finalCheckRequiresTransactionToRetainLock() {
        assertThrows(IllegalStateException.class, () -> SessionVersionGuard.lockAndValidate(users, 1L, 4));
    }
    static class FakeTransactions extends AbstractPlatformTransactionManager {
        protected Object doGetTransaction() { return new Object(); }
        protected void doBegin(Object tx, TransactionDefinition definition) {}
        protected void doCommit(DefaultTransactionStatus status) {}
        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
