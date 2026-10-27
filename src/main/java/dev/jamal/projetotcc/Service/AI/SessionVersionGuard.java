package dev.jamal.projetotcc.Service.AI;

import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Repository.UserRepository;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class SessionVersionGuard {
    private SessionVersionGuard() {}

    static long snapshot(UserRepository users, Long userId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User principal)
                || !userId.equals(principal.getId())) throw expired();
        long expected = principal.getTokenVersion();
        if (users.findTokenVersionById(userId).orElseThrow(SessionVersionGuard::expired) != expected)
            throw expired();
        return expected;
    }

    static void lockAndValidate(UserRepository users, Long userId, long expected) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("A verificação de sessão exige uma transação.");
        if (users.lockTokenVersion(userId).orElseThrow(SessionVersionGuard::expired) != expected)
            throw expired();
    }

    private static CredentialsExpiredException expired() {
        return new CredentialsExpiredException("A sessão foi alterada. Entre novamente.");
    }
}
