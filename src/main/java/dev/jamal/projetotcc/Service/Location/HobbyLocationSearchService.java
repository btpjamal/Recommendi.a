package dev.jamal.projetotcc.Service.Location;

import dev.jamal.projetotcc.DTO.Location.HobbyLocationDTO;
import dev.jamal.projetotcc.Entities.HobbyLocationRule;
import dev.jamal.projetotcc.Exception.OverpassUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class HobbyLocationSearchService {
    private static final Duration FRESCOR = Duration.ofHours(24);
    private static final String VERSAO_BUSCA = "1";

    private final JdbcTemplate jdbc;
    private final GeocodingService geocoding;
    private final OverpassService overpass;
    private final JsonMapper json = JsonMapper.builder().build();
    private final ConcurrentHashMap<Chave, CompletableFuture<Resultado>> emAndamento = new ConcurrentHashMap<>();

    private record Chave(long hobbyId, String municipio, String estado, String versao) {}

    public record Resultado(double latitude, double longitude, List<HobbyLocationDTO> locais,
                            Instant consultadoEm, boolean dadosDesatualizados) {
        Resultado desatualizado() {
            return new Resultado(latitude, longitude, locais, consultadoEm, true);
        }
    }

    public Resultado buscar(long hobbyId, String cidade, String estado, List<HobbyLocationRule> regras) {
        Chave chave = new Chave(hobbyId, normalizar(cidade), normalizar(estado), versao(regras));
        CompletableFuture<Resultado> novaBusca = new CompletableFuture<>();
        CompletableFuture<Resultado> existente = emAndamento.putIfAbsent(chave, novaBusca);
        if (existente != null) {
            try {
                return existente.join();
            } catch (CompletionException ex) {
                if (ex.getCause() instanceof RuntimeException causa) {
                    throw causa;
                }
                throw ex;
            }
        }

        try {
            Resultado resultado = buscarOuAtualizar(chave, cidade, estado, regras);
            novaBusca.complete(resultado);
            return resultado;
        } catch (RuntimeException | Error ex) {
            novaBusca.completeExceptionally(ex);
            throw ex;
        } finally {
            emAndamento.remove(chave, novaBusca);
        }
    }

    private Resultado buscarOuAtualizar(Chave chave, String cidade, String estado, List<HobbyLocationRule> regras) {
        Optional<Resultado> salvo = ler(chave);
        if (salvo.isPresent() && salvo.get().consultadoEm().plus(FRESCOR).isAfter(Instant.now())) {
            return salvo.get();
        }

        GeocodingService.Coordinates centro;
        List<HobbyLocationDTO> locais;
        try {
            centro = geocoding.buscarCoordenadas(cidade, estado);
            locais = List.copyOf(overpass.buscarLocais(regras, centro.latitude(), centro.longitude()));
        } catch (OverpassUnavailableException | RestClientException ex) {
            return salvo.map(Resultado::desatualizado).orElseThrow(() -> ex);
        }

        Resultado resultado = new Resultado(centro.latitude(), centro.longitude(), locais, Instant.now(), false);
        salvar(chave, resultado);
        return resultado;
    }

    private Optional<Resultado> ler(Chave chave) {
        return jdbc.query("""
                SELECT latitude, longitude, locais, consultado_em
                FROM hobby_location_result
                WHERE hobby_id = ? AND municipio = ? AND estado = ? AND versao_regras = ?
                """, (rs, linha) -> new Resultado(
                rs.getDouble("latitude"), rs.getDouble("longitude"),
                List.of(json.readValue(rs.getString("locais"), HobbyLocationDTO[].class)),
                rs.getTimestamp("consultado_em").toInstant(), false),
                chave.hobbyId(), chave.municipio(), chave.estado(), chave.versao()).stream().findFirst();
    }

    private void salvar(Chave chave, Resultado resultado) {
        jdbc.update("""
                INSERT INTO hobby_location_result
                    (hobby_id, municipio, estado, versao_regras, latitude, longitude, locais, consultado_em)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                ON CONFLICT (hobby_id, municipio, estado, versao_regras) DO UPDATE SET
                    latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude,
                    locais = EXCLUDED.locais, consultado_em = EXCLUDED.consultado_em
                WHERE hobby_location_result.consultado_em <= EXCLUDED.consultado_em
                """, chave.hobbyId(), chave.municipio(), chave.estado(), chave.versao(),
                resultado.latitude(), resultado.longitude(), json.writeValueAsString(resultado.locais()),
                Timestamp.from(resultado.consultadoEm()));
    }

    private String versao(List<HobbyLocationRule> regras) {
        var conteudo = regras.stream().map(regra -> json.writeValueAsString(List.of(
                regra.getRelevancia().name(),
                regra.getConditions().stream().map(condicao -> json.writeValueAsString(List.of(
                        condicao.getMatchType().name(), condicao.getOsmKey(),
                        condicao.getOsmValue() == null ? "" : condicao.getOsmValue()
                ))).sorted().toList()
        ))).sorted().toList();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (VERSAO_BUSCA + ":" + json.writeValueAsString(conteudo)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ");
    }
}
