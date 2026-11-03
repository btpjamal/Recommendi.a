package dev.jamal.projetotcc.Service.AI;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.jamal.projetotcc.Exception.TooManyRequestsException;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Service
public class PlanGenerationGuard {
    private final Semaphore capacity = new Semaphore(4);
    private final ConcurrentHashMap<Long, Boolean> active = new ConcurrentHashMap<>();
    private final Cache<Long, AtomicInteger> attempts = Caffeine.newBuilder().maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(5)).build();

    public <T> T run(Long userId, Supplier<T> action) {
        if (active.putIfAbsent(userId, true) != null) throw new TooManyRequestsException();
        boolean acquired = false;
        try {
            if (attempts.get(userId, k -> new AtomicInteger()).incrementAndGet() > 3
                    || !(acquired = capacity.tryAcquire())) throw new TooManyRequestsException();
            return action.get();
        } finally {
            if (acquired) capacity.release();
            active.remove(userId);
        }
    }
}
