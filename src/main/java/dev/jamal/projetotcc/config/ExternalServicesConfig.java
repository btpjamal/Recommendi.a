package dev.jamal.projetotcc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.*;

@Configuration
public class ExternalServicesConfig {
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService locationExecutor() {
        return new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(16), new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    public TransactionTemplate planTransactions(PlatformTransactionManager manager) {
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setTimeout(10);
        return template;
    }
}
