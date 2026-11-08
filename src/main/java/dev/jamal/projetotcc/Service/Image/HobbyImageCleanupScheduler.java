package dev.jamal.projetotcc.Service.Image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "app.imagens.exclusao",
        name = "agendamento-habilitado",
        havingValue = "true",
        matchIfMissing = false
)
public class HobbyImageCleanupScheduler {

    private final HobbyImageCleanupService cleanupService;

    @Scheduled(
            initialDelayString =
                    "${app.imagens.exclusao.atraso-inicial-ms:60000}",
            fixedDelayString =
                    "${app.imagens.exclusao.intervalo-ms:300000}"
    )
    public void executarLimpeza() {
        try {
            log.debug("Iniciando limpeza agendada de imagens.");

            cleanupService.limparImagensPendentes();

            log.debug("Limpeza agendada de imagens encerrada.");

        } catch (Exception erro) {
            log.error(
                    "Falha na execução da limpeza agendada de imagens.",
                    erro
            );
        }
    }
}