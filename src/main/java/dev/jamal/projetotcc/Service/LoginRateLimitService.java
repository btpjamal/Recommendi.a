package dev.jamal.projetotcc.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.jamal.projetotcc.Exception.TooManyRequestsException;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginRateLimitService {
    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
            .maximumSize(10000).expireAfterWrite(Duration.ofMinutes(5)).build();
    public void check(String email) {
        if (attempts.get(email.trim().toLowerCase(Locale.ROOT), k -> new AtomicInteger()).incrementAndGet() > 10)
            throw new TooManyRequestsException();
    }
}
