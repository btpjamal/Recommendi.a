package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.Entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

final class AuthenticatedUser {
    private AuthenticatedUser() {}

    static Long id(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User user && user.getId() != null) {
            return user.getId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
