
package dev.jamal.projetotcc.Service.Location;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.migration.hobby-location-rules",
        havingValue = "true"
)
public class HobbyLocationRuleMigrationRunner
        implements ApplicationRunner {

    private final HobbyLocationRuleMigrationService migrationService;

    @Override
    public void run(ApplicationArguments args) {

        System.out.println(
                "Iniciando migração das regras de localização..."
        );

        migrationService.migrarRegrasIniciais();

        System.out.println(
                "Migração das regras de localização concluída."
        );
    }
}
