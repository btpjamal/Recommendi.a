package dev.jamal.projetotcc.security;

import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.JwtService;
import dev.jamal.projetotcc.Service.JwtAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtSecurityTest {
    private final byte[] key = new byte[32];
    private final JwtService jwt = new JwtService(Base64.getEncoder().encodeToString(key));
    private User user(long id, String email) {
        User user = new User(); user.setId(id); user.setEmail(email); return user;
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void immutableSubjectDoesNotFollowReassignedEmail() {
        User original = user(1, "old@example.test");
        String token = jwt.gerarToken(original);
        original.setEmail("new@example.test");
        assertEquals(1L, jwt.extrairUserId(token));
        assertTrue(jwt.tokenValido(token, original));
        assertFalse(jwt.tokenValido(token, user(2, "old@example.test")));
    }

    @Test void versionRevokesExistingToken() {
        User user = user(1, "a@example.test");
        String token = jwt.gerarToken(user);
        user.setTokenVersion(1);
        assertFalse(jwt.tokenValido(token, user));
        assertTrue(jwt.tokenValido(jwt.gerarToken(user), user));
    }

    @Test void rejectsExpiredForgedAndLegacyTokens() {
        User user = user(1, "a@example.test");
        String expired = Jwts.builder().issuer("recommendia").subject("1").claim("tokenVersion", 0)
                .issuedAt(new Date(0)).expiration(new Date(1)).signWith(Keys.hmacShaKeyFor(key)).compact();
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwt.tokenValido(expired, user));
        String legacy = Jwts.builder().subject(user.getEmail()).claim("userId", 1)
                .expiration(new Date(System.currentTimeMillis()+60000)).signWith(Keys.hmacShaKeyFor(key)).compact();
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwt.extrairUserId(legacy));
        byte[] other = new byte[32]; Arrays.fill(other, (byte) 42);
        String forged = new JwtService(Base64.getEncoder().encodeToString(other)).gerarToken(user);
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwt.tokenValido(forged, user));
    }

    @Test void filterLoadsByIdAndRejectsRevokedToken() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        User user = user(1, "a@example.test");
        String token = jwt.gerarToken(user);
        when(repo.findById(1L)).thenReturn(Optional.of(user));
        var request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer "+token);
        var filter = new JwtAuthenticationFilter(jwt, repo);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertSame(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(repo, never()).findByEmail(anyString());
        SecurityContextHolder.clearContext(); user.setTokenVersion(1);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
