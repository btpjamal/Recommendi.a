package dev.jamal.projetotcc.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PublicAuthRateLimitFilter extends OncePerRequestFilter {
    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
            .maximumSize(10000).expireAfterWrite(Duration.ofMinutes(1)).build();
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getServletPath();
        boolean publicAuth = "POST".equals(request.getMethod()) &&
                (uri.equals("/api/v1/auth/login") || uri.equals("/api/v1/auth/register") || uri.equals("/api/v1/users"));
        // Forwarded headers are untrusted. Configure a trusted proxy separately when deploying.
        if (publicAuth && attempts.get(request.getRemoteAddr(), k -> new AtomicInteger()).incrementAndGet() > 60) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Muitas solicitações. Tente novamente em um minuto.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
