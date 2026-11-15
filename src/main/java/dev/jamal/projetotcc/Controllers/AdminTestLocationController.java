package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Repository.RecommendationProfileRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/location-preferences")
@RequiredArgsConstructor
public class AdminTestLocationController {
    private final RecommendationProfileRepository profiles;
    private final JdbcTemplate jdbc;

    public record Location(
            @NotBlank @Size(max = 100) String cidade,
            @NotBlank @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO")
            String estado) {}

    @GetMapping
    public ResponseEntity<Location> obter(@AuthenticationPrincipal User admin) {
        var location = profiles.findByUserId(admin.getId())
                .map(profile -> new Location(profile.getCidade(), profile.getEstado()))
                .orElseGet(() -> new Location(null, null));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(location);
    }

    @PutMapping
    public ResponseEntity<Void> salvar(@AuthenticationPrincipal User admin, @Valid @RequestBody Location location) {
        jdbc.update("""
                INSERT INTO recommendation_profile (user_id, cidade, estado, tempo_disponivel_semanal,
                    orcamento_inicial, tipo_socializacao, nivel_atividade_fisica_desejada, ambiente_preferido,
                    questionario_concluido, updated_at)
                VALUES (?, ?, ?, 0, 0, 'INDIFERENTE', 'INDIFERENTE', 'INDIFERENTE', false, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id) DO UPDATE SET
                    cidade = EXCLUDED.cidade, estado = EXCLUDED.estado, updated_at = EXCLUDED.updated_at
                """, admin.getId(), location.cidade().strip(), location.estado());
        return ResponseEntity.noContent().build();
    }
}
