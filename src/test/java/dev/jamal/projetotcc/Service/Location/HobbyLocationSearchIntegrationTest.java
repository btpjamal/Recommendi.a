package dev.jamal.projetotcc.Service.Location;

import dev.jamal.projetotcc.DTO.Location.HobbyLocationDTO;
import dev.jamal.projetotcc.Entities.HobbyLocationCondition;
import dev.jamal.projetotcc.Entities.HobbyLocationRule;
import dev.jamal.projetotcc.Enum.HobbyLocationRelevance;
import dev.jamal.projetotcc.Exception.OverpassUnavailableException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers
class HobbyLocationSearchIntegrationTest {
    @Container
    static final PostgreSQLContainer BANCO = new PostgreSQLContainer("postgres:17-alpine");

    private static final long HOBBY = 900000L;
    private static final HobbyLocationDTO LOCAL =
            new HobbyLocationDTO("way/1", "Quadra pública", -23.55, -46.63, "", "ALTA", 0.4);
    private static JdbcTemplate jdbc;

    private final GeocodingService geocoding = mock(GeocodingService.class);
    private final OverpassService overpass = mock(OverpassService.class);
    private HobbyLocationSearchService busca;

    @BeforeAll
    static void migrar() {
        var fonte = new DriverManagerDataSource(BANCO.getJdbcUrl(), BANCO.getUsername(), BANCO.getPassword());
        Flyway.configure().dataSource(fonte).load().migrate();
        jdbc = new JdbcTemplate(fonte);
        jdbc.update("INSERT INTO hobby_category (id, nome) VALUES (?, 'Teste de busca compartilhada')", HOBBY);
        jdbc.update("""
                INSERT INTO hobby (id, nome, category_id, ambiente, nivel_atividade_fisica,
                                   requer_equipamento, tipo_socializacao)
                VALUES (?, 'Futebol de teste', ?, 'AO_AR_LIVRE', 'MODERADO', false, 'SOCIAL')
                """, HOBBY, HOBBY);
    }

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM hobby_location_result WHERE hobby_id = ?", HOBBY);
        busca = new HobbyLocationSearchService(jdbc, geocoding, overpass);
        when(geocoding.buscarCoordenadas(anyString(), anyString()))
                .thenReturn(new GeocodingService.Coordinates(-23.55, -46.63));
        when(overpass.buscarLocais(anyList(), anyDouble(), anyDouble())).thenReturn(List.of(LOCAL));
    }

    @Test
    void reaproveitaResultadoPersistidoSemNovaConsulta() {
        busca.buscar(HOBBY, "São Paulo", "SP", regras("soccer"));
        var novaInstancia = new HobbyLocationSearchService(jdbc, geocoding, overpass);
        var resultado = novaInstancia.buscar(HOBBY, " sao   paulo ", "sp", regras("soccer"));

        assertEquals(List.of(LOCAL), resultado.locais());
        assertFalse(resultado.dadosDesatualizados());
        verify(overpass, times(1)).buscarLocais(anyList(), anyDouble(), anyDouble());
        verify(geocoding, times(1)).buscarCoordenadas(anyString(), anyString());
    }

    @Test
    void usaUltimoResultadoValidoQuandoOverpassFalha() {
        busca.buscar(HOBBY, "São Paulo", "SP", regras("soccer"));
        jdbc.update("UPDATE hobby_location_result SET consultado_em = now() - interval '25 hours'");
        when(overpass.buscarLocais(anyList(), anyDouble(), anyDouble()))
                .thenThrow(new OverpassUnavailableException());

        var resultado = busca.buscar(HOBBY, "São Paulo", "SP", regras("soccer"));
        assertEquals(List.of(LOCAL), resultado.locais());
        assertTrue(resultado.dadosDesatualizados());
        assertThrows(OverpassUnavailableException.class,
                () -> busca.buscar(HOBBY, "São Paulo", "SP", regras("tennis")));
        assertThrows(OverpassUnavailableException.class,
                () -> busca.buscar(HOBBY, "São Paulo", "MG", regras("soccer")));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM hobby_location_result", Integer.class));
    }

    @Test
    void compartilhaConsultaEntreBuscasSimultaneas() throws Exception {
        var entrouNoOverpass = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        when(overpass.buscarLocais(anyList(), anyDouble(), anyDouble())).thenAnswer(invocacao -> {
            entrouNoOverpass.countDown();
            assertTrue(liberar.await(5, TimeUnit.SECONDS));
            return List.of(LOCAL);
        });

        var executor = Executors.newFixedThreadPool(2);
        try {
            var primeira = executor.submit(() -> busca.buscar(HOBBY, "São Paulo", "SP", regras("soccer")));
            assertTrue(entrouNoOverpass.await(5, TimeUnit.SECONDS));
            var segunda = executor.submit(() -> busca.buscar(HOBBY, "São Paulo", "SP", regras("soccer")));
            liberar.countDown();
            assertEquals(List.of(LOCAL), primeira.get(5, TimeUnit.SECONDS).locais());
            assertEquals(List.of(LOCAL), segunda.get(5, TimeUnit.SECONDS).locais());
            verify(overpass, times(1)).buscarLocais(anyList(), anyDouble(), anyDouble());
        } finally {
            liberar.countDown();
            executor.shutdownNow();
        }
    }

    private static List<HobbyLocationRule> regras(String esporte) {
        var regra = new HobbyLocationRule();
        regra.setRelevancia(HobbyLocationRelevance.ALTA);
        var condicao = new HobbyLocationCondition();
        condicao.setOsmKey("sport");
        condicao.setOsmValue(esporte);
        regra.adicionarCondicao(condicao);
        return List.of(regra);
    }
}
