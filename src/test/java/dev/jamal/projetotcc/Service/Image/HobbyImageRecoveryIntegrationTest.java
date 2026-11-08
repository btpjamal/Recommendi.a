package dev.jamal.projetotcc.Service.Image;

import dev.jamal.projetotcc.Entities.HobbyImageUpload;
import dev.jamal.projetotcc.Enum.HobbyImageStatus;
import dev.jamal.projetotcc.Repository.HobbyImageUploadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(
        classes = HobbyImageRecoveryIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.name=hobby-image-integration-isolated",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false",
                "spring.sql.init.mode=never",
                "app.imagens.exclusao.reserva-expiracao-minutos=30"
        }
)
class HobbyImageRecoveryIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("hobby_images_integration")
                    .withUsername("integration")
                    .withPassword("integration");

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("dev.jamal.projetotcc.Entities")
    @EnableJpaRepositories("dev.jamal.projetotcc.Repository")
    @Import({HobbyImageStatusService.class, HobbyImageCleanupService.class})
    static class Config {
        @Bean
        HobbyImageStorageService storageService() {
            return mock(HobbyImageStorageService.class);
        }
    }

    @Autowired HobbyImageUploadRepository repository;
    @Autowired HobbyImageStatusService statusService;
    @Autowired HobbyImageCleanupService cleanupService;
    @Autowired HobbyImageStorageService storageService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void preparar() {
        // Este repository usa exclusivamente o banco descartavel do container.
        repository.deleteAll();
        reset(storageService);
    }

    @Test
    void consultaSelecionaSomenteReservasExpiradasOuSemData() {
        var limite = Instant.parse("2026-11-08T12:00:00Z");
        var expirada = criar(HobbyImageStatus.EXCLUINDO, limite.minus(1, ChronoUnit.MINUTES));
        var legada = criar(HobbyImageStatus.EXCLUINDO, null);
        criar(HobbyImageStatus.EXCLUINDO, limite);
        criar(HobbyImageStatus.EXCLUINDO, limite.plus(1, ChronoUnit.MINUTES));
        criar(HobbyImageStatus.SUBSTITUIDA, null);

        var ids = repository.buscarPublicIdsComReservaExpirada(
                HobbyImageStatus.EXCLUINDO, limite);

        assertEquals(2, ids.size());
        assertTrue(ids.contains(expirada));
        assertTrue(ids.contains(legada));
    }

    @Test
    void recuperacaoPersisteENovoTokenRejeitaExecucaoAntiga() {
        var limite = Instant.now().minus(30, ChronoUnit.MINUTES);
        String id = criar(HobbyImageStatus.EXCLUINDO, limite.minus(1, ChronoUnit.MINUTES));
        String tokenAntigo = buscar(id).getExclusaoToken();

        assertTrue(statusService.recuperarReservaExpirada(id, limite));
        var recuperada = buscar(id);
        assertEquals(HobbyImageStatus.AGUARDANDO_EXCLUSAO, recuperada.getStatus());
        assertNull(recuperada.getExclusaoToken());
        assertNull(recuperada.getExclusaoIniciadaEm());

        String tokenNovo = statusService.reservarParaExclusao(id);
        assertNotNull(tokenNovo);
        assertNotEquals(tokenAntigo, tokenNovo);

        assertThrows(IllegalStateException.class,
                () -> statusService.marcarComoSubstituida(id, tokenAntigo));
        statusService.liberarReservaParaNovaTentativa(id, tokenAntigo);
        assertEquals(tokenNovo, buscar(id).getExclusaoToken());
        assertEquals(HobbyImageStatus.EXCLUINDO, buscar(id).getStatus());

        statusService.marcarComoSubstituida(id, tokenNovo);
        assertEquals(HobbyImageStatus.SUBSTITUIDA, buscar(id).getStatus());
        assertNull(buscar(id).getExclusaoToken());
        assertNull(buscar(id).getExclusaoIniciadaEm());
    }

    @Test
    void recuperacaoReconfereDataDepoisDaConsulta() {
        var limite = Instant.now().minus(30, ChronoUnit.MINUTES);
        String id = criar(HobbyImageStatus.EXCLUINDO, limite.minus(1, ChronoUnit.MINUTES));
        assertTrue(repository.buscarPublicIdsComReservaExpirada(
                HobbyImageStatus.EXCLUINDO, limite).contains(id));

        var imagem = buscar(id);
        String tokenNovo = UUID.randomUUID().toString();
        imagem.setExclusaoToken(tokenNovo);
        imagem.setExclusaoIniciadaEm(Instant.now());
        repository.saveAndFlush(imagem);

        assertFalse(statusService.recuperarReservaExpirada(id, limite));
        assertEquals(tokenNovo, buscar(id).getExclusaoToken());
        assertEquals(HobbyImageStatus.EXCLUINDO, buscar(id).getStatus());
    }

    @Test
    void limpezaRecuperaExpiradaEPreservaRecenteSemTransacaoNoStorage()
            throws Exception {
        String expirada = criar(HobbyImageStatus.EXCLUINDO,
                Instant.now().minus(1, ChronoUnit.HOURS));
        String recente = criar(HobbyImageStatus.EXCLUINDO, Instant.now());

        doAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            var reservada = jdbcTemplate.queryForMap("""
                SELECT status, exclusao_token
                FROM hobby_image_upload
                WHERE public_id = ?
                """, expirada);

            assertEquals("EXCLUINDO", reservada.get("status"));
            assertNotNull(reservada.get("exclusao_token"));
            return null;
        }).when(storageService).excluirImagem(expirada);

        cleanupService.limparImagensPendentes();

        assertEquals(HobbyImageStatus.SUBSTITUIDA, buscar(expirada).getStatus());
        assertNull(buscar(expirada).getExclusaoToken());
        assertNull(buscar(expirada).getExclusaoIniciadaEm());
        assertEquals(HobbyImageStatus.EXCLUINDO, buscar(recente).getStatus());
        verify(storageService).excluirImagem(expirada);
        verifyNoMoreInteractions(storageService);
    }

    @Test
    void falhaNoStorageLiberaReservaNoBanco() throws Exception {
        String id = criar(HobbyImageStatus.AGUARDANDO_EXCLUSAO, null);
        doThrow(new IOException("Falha simulada"))
                .when(storageService).excluirImagem(id);

        cleanupService.excluirImagemPendente(id);

        assertEquals(HobbyImageStatus.AGUARDANDO_EXCLUSAO, buscar(id).getStatus());
        assertNull(buscar(id).getExclusaoToken());
        assertNull(buscar(id).getExclusaoIniciadaEm());
        verify(storageService).excluirImagem(id);
    }

    @Test
    void duasTentativasConcorrentesConseguemApenasUmaReserva() throws Exception {
        String id = criar(HobbyImageStatus.AGUARDANDO_EXCLUSAO, null);
        var executor = Executors.newFixedThreadPool(2);
        var prontas = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);

        java.util.concurrent.Callable<String> tentativa = () -> {
            prontas.countDown();
            if (!iniciar.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tempo esgotado aguardando inicio");
            }
            return statusService.reservarParaExclusao(id);
        };

        try {
            var primeira = executor.submit(tentativa);
            var segunda = executor.submit(tentativa);
            assertTrue(prontas.await(10, TimeUnit.SECONDS));
            iniciar.countDown();

            String token1 = primeira.get(20, TimeUnit.SECONDS);
            String token2 = segunda.get(20, TimeUnit.SECONDS);
            assertTrue((token1 == null) != (token2 == null));
            assertEquals(token1 != null ? token1 : token2,
                    buscar(id).getExclusaoToken());
            assertEquals(HobbyImageStatus.EXCLUINDO, buscar(id).getStatus());
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void consultaDeExpiracaoIndependeDoFusoDaSessao() {
        Instant inicio = Instant.parse("2026-11-08T15:00:00Z");
        Instant limite = inicio.plusSeconds(60);
        String id = criar(HobbyImageStatus.EXCLUINDO, inicio);
        var transaction = new TransactionTemplate(transactionManager);

        for (String fuso : new String[]{"UTC", "America/Sao_Paulo"}) {
            transaction.executeWithoutResult(status -> {
                // Fuso restrito a esta transacao, sem alterar o banco globalmente.
                jdbcTemplate.execute("SET LOCAL TIME ZONE '" + fuso + "'");
                assertEquals(inicio, buscar(id).getExclusaoIniciadaEm());
                assertTrue(repository.buscarPublicIdsComReservaExpirada(
                        HobbyImageStatus.EXCLUINDO, limite).contains(id));
                assertEquals(inicio.getEpochSecond(), jdbcTemplate.queryForObject(
                        "SELECT EXTRACT(EPOCH FROM exclusao_iniciada_em)::bigint "
                                + "FROM hobby_image_upload WHERE public_id = ?",
                        Long.class, id).longValue());
            });
        }
    }

    private String criar(HobbyImageStatus status, Instant inicio) {
        String id = "recommendia/hobbies/integration-" + UUID.randomUUID();
        var imagem = new HobbyImageUpload();
        imagem.setPublicId(id);
        imagem.setImagemUrl("https://example.com/" + id + ".jpg");
        imagem.setStatus(status);
        imagem.setExclusaoIniciadaEm(inicio);
        imagem.setExclusaoToken(status == HobbyImageStatus.EXCLUINDO
                && inicio != null ? UUID.randomUUID().toString() : null);
        repository.saveAndFlush(imagem);
        return id;
    }

    private HobbyImageUpload buscar(String id) {
        return repository.findByPublicId(id).orElseThrow();
    }
}
