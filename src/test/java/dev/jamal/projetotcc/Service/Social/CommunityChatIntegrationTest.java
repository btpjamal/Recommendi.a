package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.Controllers.CommunityController;
import dev.jamal.projetotcc.DTO.Social.CreateCommunityMessageDTO;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.*;
import dev.jamal.projetotcc.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = CommunityChatIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.config.name=community-chat-integration-isolated",
                "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=false", "spring.sql.init.mode=never"})
class CommunityChatIntegrationTest {
    @Container
    static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES =
            new org.testcontainers.postgresql.PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("community_chat_integration").withUsername("integration").withPassword("integration");
    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry properties) {
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
    @Import({CommunityService.class, CommunityMessageService.class, SocialLocationService.class,
            SocialProfileService.class, SocialHubService.class, SocialAffinityService.class,
            SocialProfileResumeService.class, CommunityController.class})
    static class Config {}

    @Autowired CommunityService comunidades;
    @Autowired CommunityMessageService chat;
    @Autowired CommunityMemberRepository membros;
    @Autowired CommunityMessageRepository mensagens;
    @Autowired SocialProfileRepository sociais;
    @Autowired UserRepository usuarios;
    @Autowired RecommendationProfileRepository perfis;
    @Autowired HobbyRepository hobbies;
    @Autowired HobbyCategoryRepository categorias;
    @Autowired CommunityController controller;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private Long a, b, externo, comunidade, outra;

    @BeforeEach
    void preparar() {
        jdbc.execute("TRUNCATE users, hobby, hobby_category CASCADE");
        a = usuario(); b = usuario(); externo = usuario();
        var categoria = new HobbyCategory(); categoria.setNome("Teste chat"); categorias.saveAndFlush(categoria);
        var hobby = new Hobby(); hobby.setNome("Hobby chat"); hobby.setCategory(categoria);
        hobby.setTipoSocializacao(TipoSocializacao.SOCIAL);
        hobby.setNivelAtividadeFisica(NivelAtividadeFisica.BAIXO); hobby.setAmbiente(AmbientePreferido.CASA);
        hobbies.saveAndFlush(hobby);
        comunidades.garantirComunidadesGerais(); comunidades.garantirComunidadesRegionais(a);
        comunidade = comunidades.listarGerais(a).get(0).id();
        outra = comunidades.listarRegionais(a).get(0).id();
        comunidades.entrar(comunidade, a); comunidades.entrar(comunidade, b); comunidades.entrar(outra, a);
    }

    @Test
    void historicoRecentesEAnterioresSaoPaginadosSemLacunas() {
        for (int i = 0; i < 65; i++) enviar(a, "Mensagem " + i);
        var recente = chat.listarPagina(comunidade, a, null, null, 30);
        assertEquals(30, recente.mensagens().size()); assertTrue(recente.temMais());
        assertEquals("Mensagem 35", recente.mensagens().get(0).conteudo());
        var anterior = chat.listarPagina(comunidade, a, recente.menorId(), null, 30);
        var primeira = chat.listarPagina(comunidade, a, anterior.menorId(), null, 30);
        assertTrue(anterior.temMais()); assertFalse(primeira.temMais());
        var ids = new HashSet<Long>();
        for (var pagina : List.of(primeira, anterior, recente)) pagina.mensagens().forEach(m -> assertTrue(ids.add(m.id())));
        assertEquals(65, ids.size());
    }

    @Test
    void novidadesTemCursorProprioENaoMisturamComunidades() {
        var inicial = enviar(a, "Inicial");
        for (int i = 0; i < 5; i++) enviar(b, "Nova " + i);
        chat.enviar(outra, a, new CreateCommunityMessageDTO("Outro grupo"));
        Long cursor = inicial.id(); int total = 0;
        boolean mais;
        do {
            var pagina = chat.listarPagina(comunidade, a, null, cursor, 2);
            assertTrue(pagina.mensagens().stream().noneMatch(m -> m.conteudo().equals("Outro grupo")));
            total += pagina.mensagens().size(); cursor = pagina.maiorId(); mais = pagina.temMais();
        } while (mais);
        assertEquals(5, total);
        assertTrue(chat.listarPagina(comunidade, a, null, cursor, 2).mensagens().isEmpty());
    }

    @Test
    void naoMembroESaidaRevogamLeituraEEnvio() {
        assertThrows(IllegalStateException.class, () -> chat.listarPagina(comunidade, externo, null, null, 30));
        assertThrows(IllegalStateException.class, () -> enviar(externo, "Sem acesso"));
        comunidades.sair(comunidade, a);
        assertThrows(IllegalStateException.class, () -> chat.listarPagina(comunidade, a, null, null, 30));
        assertThrows(IllegalStateException.class, () -> enviar(a, "Depois de sair"));
        assertTrue(comunidades.entrar(comunidade, a).participando());
    }

    @Test
    void validacaoDeConteudoEAutoriaContinuaNoServidor() {
        assertThrows(IllegalArgumentException.class, () -> enviar(a, "   "));
        assertThrows(IllegalArgumentException.class, () -> chat.enviar(comunidade, a, null));
        assertThrows(IllegalArgumentException.class, () -> enviar(a, "x".repeat(1001)));
        var enviada = enviar(a, "x".repeat(1000));
        assertEquals(a, enviada.userId()); assertTrue(enviada.propria());
        assertFalse(chat.listarPagina(comunidade, b, null, null, 30).mensagens().get(0).propria());
    }

    @Test
    void cursoresInvalidosSaoRejeitados() {
        assertThrows(IllegalArgumentException.class, () -> chat.listarPagina(comunidade, a, 10L, 1L, 30));
        assertThrows(IllegalArgumentException.class, () -> chat.listarPagina(comunidade, a, -1L, null, 30));
        assertThrows(IllegalArgumentException.class, () -> chat.listarPagina(comunidade, a, null, -1L, 30));
        assertThrows(IllegalArgumentException.class, () -> chat.listarPagina(comunidade, a, null, null, 51));
    }

    @Test
    void entrarAoMesmoTempoNaoDuplicaParticipacao() throws Exception {
        var executor = Executors.newFixedThreadPool(2); var inicio = new CountDownLatch(1);
        try {
            var um = executor.submit(() -> { inicio.await(); return comunidades.entrar(comunidade, externo); });
            var dois = executor.submit(() -> { inicio.await(); return comunidades.entrar(comunidade, externo); });
            inicio.countDown();
            assertTrue(um.get(20, TimeUnit.SECONDS).participando());
            assertTrue(dois.get(20, TimeUnit.SECONDS).participando());
            assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM community_members WHERE community_id=? AND user_id=?", Long.class, comunidade, externo));
        } finally { inicio.countDown(); executor.shutdownNow(); }
    }

    @Test
    void saidaConcorrenteComEnvioNaoPermiteNovosEnviosAposConcluir() throws Exception {
        var executor = Executors.newFixedThreadPool(2); var inicio = new CountDownLatch(1);
        try {
            var envio = executor.submit(() -> { inicio.await(); try { enviar(a, "Concorrente"); } catch (IllegalStateException permitido) {} return true; });
            var saida = executor.submit(() -> { inicio.await(); return comunidades.sair(comunidade, a); });
            inicio.countDown(); envio.get(20, TimeUnit.SECONDS); assertFalse(saida.get(20, TimeUnit.SECONDS).participando());
            assertThrows(IllegalStateException.class, () -> enviar(a, "Novo envio"));
        } finally { inicio.countDown(); executor.shutdownNow(); }
    }

    @Test
    void mensagemNaoConfirmadaNaoPodeFicarAtrasDoCursorDeOutraTransacao() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var inserida = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        try {
            var primeira = executor.submit(() -> new TransactionTemplate(transactionManager).execute(tx -> {
                var mensagem = enviar(a, "Primeira transação"); inserida.countDown();
                try { if (!liberar.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tempo esgotado"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                return mensagem;
            }));
            assertTrue(inserida.await(10, TimeUnit.SECONDS));
            var segunda = executor.submit(() -> enviar(b, "Segunda transação"));
            assertThrows(TimeoutException.class, () -> segunda.get(200, TimeUnit.MILLISECONDS));
            assertTrue(chat.listarPagina(comunidade, b, null, 0L, 30).mensagens().isEmpty());
            liberar.countDown();
            var um = primeira.get(20, TimeUnit.SECONDS); var dois = segunda.get(20, TimeUnit.SECONDS);
            assertTrue(um.id() < dois.id());
            assertEquals(dois.id(), chat.listarPagina(comunidade, a, null, um.id(), 30).mensagens().get(0).id());
        } finally { liberar.countDown(); executor.shutdownNow(); }
    }

    @Test
    void endpointValidaParticipacaoSocialEFormatoPaginado() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var usuario = usuarios.findById(a).orElseThrow();
        var auth = new UsernamePasswordAuthenticationToken(usuario.getEmail(), "ignorado");
        enviar(a, "Teste de formato");
        String url = "/api/v1/community/communities/" + comunidade + "/messages";
        mvc.perform(get(url).principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagens[0].conteudo").value("Teste de formato"))
                .andExpect(jsonPath("$.temMais").value(false));
        var perfil = sociais.findByUser_Id(a).orElseThrow(); perfil.setEnabled(false); sociais.saveAndFlush(perfil);
        mvc.perform(get(url).principal(auth)).andExpect(status().isForbidden());
        mvc.perform(post(url).principal(auth).contentType("application/json").content("{\"conteudo\":\"Sem acesso\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chatFiltraAutoresIndisponiveisAntesDePaginar() {
        var mensagemA = enviar(a, "Mensagem de A");
        var mensagemB = enviar(b, "Mensagem de B");
        jdbc.update("INSERT INTO social_user_blocks (blocker_id, blocked_id, created_at) VALUES (?, ?, now())", a, b);

        var historico = chat.listarPagina(comunidade, a, null, null, 1);
        assertEquals(mensagemA.id(), historico.mensagens().get(0).id());
        assertFalse(historico.temMais());
        var novidades = chat.listarPagina(comunidade, b, null, 0L, 1);
        assertEquals(mensagemB.id(), novidades.mensagens().get(0).id());
        assertFalse(novidades.temMais());

        jdbc.update("DELETE FROM social_user_blocks WHERE blocker_id = ? AND blocked_id = ?", a, b);
        jdbc.update("UPDATE social_profiles SET enabled = false WHERE user_id = ?", b);
        assertTrue(chat.listarPagina(comunidade, a, null, mensagemA.id(), 1).mensagens().isEmpty());
        assertThrows(IllegalStateException.class, () -> enviar(b, "Envio indisponível"));
    }

    @Test
    void reenviosConcorrentesPreservamUmaMensagemPorTentativa() throws Exception {
        var tentativa = new CreateCommunityMessageDTO("Olá", UUID.randomUUID());
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var primeiro = executor.submit(() -> chat.enviar(comunidade, a, tentativa));
            var segundo = executor.submit(() -> chat.enviar(comunidade, a, tentativa));
            assertEquals(primeiro.get(20, TimeUnit.SECONDS).id(),
                    segundo.get(20, TimeUnit.SECONDS).id());
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, chat.listar(comunidade, a).size());

        var conflito = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> chat.enviar(comunidade, a,
                        new CreateCommunityMessageDTO("Outro texto", tentativa.clientMessageId())));
        assertEquals(409, conflito.getStatusCode().value());
        assertEquals(1, chat.listar(comunidade, a).size());

        chat.enviar(comunidade, a, new CreateCommunityMessageDTO("Olá", UUID.randomUUID()));
        assertEquals(2, chat.listar(comunidade, a).size());
    }

    @Test
    void limitePersistePermiteReenvioERecuperaDepoisDaJanela() {
        var tentativa = new CreateCommunityMessageDTO("Mensagem confirmada", UUID.randomUUID());
        var original = chat.enviar(comunidade, a, tentativa);
        jdbc.update("UPDATE social_write_quotas SET used = 120 WHERE user_id = ? AND action = 'COMMUNITY_MESSAGE'", a);

        assertEquals(original.id(), chat.enviar(comunidade, a, tentativa).id());
        assertThrows(SocialQuotaExceededException.class, () -> enviar(a, "Acima do limite"));
        assertEquals(1, chat.listar(comunidade, a).size());
        enviar(b, "Outro participante tem limite independente");

        jdbc.update("UPDATE social_write_quotas SET started_at = now() - interval '61 seconds' WHERE user_id = ?", a);
        enviar(a, "Depois da janela");
        assertEquals(1, jdbc.queryForObject(
                "SELECT used FROM social_write_quotas WHERE user_id = ? AND action = 'COMMUNITY_MESSAGE'",
                Integer.class, a));
    }

    @Test
    void participantesSimultaneosPublicamEReleemSemDuplicatas() throws Exception {
        var participantes = new java.util.ArrayList<Long>();
        participantes.add(a);
        participantes.add(b);
        for (int i = 0; i < 6; i++) {
            Long id = usuario();
            comunidades.entrar(comunidade, id);
            participantes.add(id);
        }
        var executor = java.util.concurrent.Executors.newFixedThreadPool(participantes.size());
        var inicio = new CountDownLatch(1);
        try {
            var envios = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (Long autor : participantes) {
                envios.add(executor.submit(() -> {
                    inicio.await();
                    for (int i = 0; i < 5; i++) {
                        var pedido = new CreateCommunityMessageDTO("Olá 👋\nMensagem " + i, UUID.randomUUID());
                        var primeira = chat.enviar(comunidade, autor, pedido);
                        assertEquals(primeira.id(), chat.enviar(comunidade, autor, pedido).id());
                    }
                    return null;
                }));
            }
            inicio.countDown();
            for (var envio : envios) envio.get(30, TimeUnit.SECONDS);
            for (Long leitor : participantes) {
                var historico = chat.listarPagina(comunidade, leitor, null, null, 50);
                assertEquals(40, historico.mensagens().size());
                assertFalse(historico.temMais());
            }
            for (String texto : java.util.List.of("\0início", "me\0io", "fim\0")) {
                var erro = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> enviar(a, texto));
                assertEquals(400, erro.getStatusCode().value());
            }
            assertEquals(40, mensagens.count());
        } finally {
            inicio.countDown();
            executor.shutdownNow();
        }
    }

    private dev.jamal.projetotcc.DTO.Social.CommunityMessageDTO enviar(Long autor, String texto) {
        return chat.enviar(comunidade, autor, new CreateCommunityMessageDTO(texto));
    }

    private Long usuario() {
        var user = new User(); user.setNome("Participante de teste"); user.setEmail(UUID.randomUUID() + "@example.com");
        user.setSenha("senha-do-banco-descartavel"); user.setDataNascimento(LocalDate.of(1990, 1, 1)); usuarios.saveAndFlush(user);
        var profile = new RecommendationProfile(); profile.setUser(user); profile.setEstado("MG"); profile.setCidade("São João");
        profile.setTempoDisponivelSemanal(5.0); profile.setOrcamentoInicial(100.0); profile.setTipoSocializacao(TipoSocializacao.SOCIAL);
        profile.setNivelAtividadeFisicaDesejada(NivelAtividadeFisica.BAIXO); profile.setAmbientePreferido(AmbientePreferido.CASA); perfis.saveAndFlush(profile);
        var social = new SocialProfile(); social.setUser(user); social.setEnabled(true); sociais.saveAndFlush(social);
        return user.getId();
    }
}
