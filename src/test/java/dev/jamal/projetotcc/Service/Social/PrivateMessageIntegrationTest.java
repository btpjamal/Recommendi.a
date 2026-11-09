package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.Controllers.PrivateMessageController;
import dev.jamal.projetotcc.DTO.Social.*;
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
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = PrivateMessageIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.config.name=private-message-integration-isolated",
                "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=false", "spring.sql.init.mode=never"})
class PrivateMessageIntegrationTest {
    @Container
    static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES =
            new org.testcontainers.postgresql.PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("private_message_integration").withUsername("integration").withPassword("integration");
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
    @Import({SocialContactService.class, SocialProfileService.class, SocialHubService.class,
            SocialLocationService.class, SocialAffinityService.class, SocialProfileResumeService.class,
            PrivateMessageService.class, PrivateMessageController.class})
    static class Config {}

    @Autowired PrivateMessageService chat;
    @Autowired SocialContactService contacts;
    @Autowired SocialProfileService social;
    @Autowired PrivateMessageRepository messages;
    @Autowired PrivateMessageReadRepository reads;
    @Autowired SocialProfileRepository profiles;
    @Autowired RecommendationProfileRepository recommendations;
    @Autowired UserRepository users;
    @Autowired PrivateMessageController controller;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private Long a, b, terceiro, contato;

    @BeforeEach
    void preparar() {
        jdbc.execute("TRUNCATE users CASCADE");
        a = usuario(); b = usuario(); terceiro = usuario();
        contato = contacts.solicitar(a, new SocialContactRequestDTO(b, "Olá")).id();
    }
    private void aceitar() { contacts.responder(b, contato, true); }
    private PrivateMessageDTO enviar(Long autor, String texto) {
        return chat.enviar(autor, contato, new SendPrivateMessageDTO(texto, UUID.randomUUID()));
    }
    private void falha(int esperado, org.junit.jupiter.api.function.Executable acao) {
        assertEquals(esperado, assertThrows(ResponseStatusException.class, acao).getStatusCode().value());
    }

    @Test
    void pendenciaRecusaECancelamentoImpedemAcessoAoChat() {
        falha(403, () -> chat.detalhes(a, contato));
        falha(403, () -> enviar(a, "Antes de aceitar"));
        falha(403, () -> chat.listar(b, contato, null, null, 30));
        falha(403, () -> chat.marcarLidas(b, contato, 1L));
        contacts.responder(b, contato, false);
        falha(403, () -> enviar(a, "Depois da recusa"));
        assertEquals(0, messages.count());
    }

    @Test
    void participantesAceitosEnviamELeemComAutoriaCorreta() {
        aceitar();
        var primeira = enviar(a, "Olá, B"); var segunda = enviar(b, "Olá, A");
        var pagina = chat.listar(b, contato, null, null, 30);
        assertEquals(List.of(primeira.id(), segunda.id()), pagina.mensagens().stream().map(PrivateMessageDTO::id).toList());
        assertFalse(pagina.mensagens().get(0).propria()); assertTrue(pagina.mensagens().get(1).propria());
        assertEquals(a, pagina.mensagens().get(0).userId());
        assertTrue(pagina.mensagens().get(0).dataEnvio().toString().endsWith("Z"));
    }

    @Test
    void terceiroNaoAcessaMetadadosHistoricoEnvioOuLeitura() {
        aceitar(); var mensagem = enviar(a, "Privada");
        falha(404, () -> chat.detalhes(terceiro, contato));
        falha(404, () -> chat.listar(terceiro, contato, null, null, 30));
        falha(404, () -> enviar(terceiro, "Intrusão"));
        falha(404, () -> chat.marcarLidas(terceiro, contato, mensagem.id()));
        assertEquals(0, chat.naoLidas(terceiro).total());
    }

    @Test
    void bloqueioImpedeLeituraEnvioEAckNosDoisSentidos() {
        aceitar(); var mensagem = enviar(a, "Antes do bloqueio");
        contacts.bloquear(b, a);
        for (Long ator : List.of(a, b)) {
            falha(403, () -> chat.listar(ator, contato, null, null, 30));
            falha(403, () -> enviar(ator, "Bloqueada"));
            falha(403, () -> chat.marcarLidas(ator, contato, mensagem.id()));
            assertEquals(0, chat.naoLidas(ator).total());
        }
        contacts.desbloquear(b, a);
        falha(403, () -> chat.detalhes(a, contato));
    }

    @Test
    void desativacaoSocialDeQualquerParticipanteSuspendeOChat() {
        aceitar(); enviar(a, "Mensagem");
        social.desativar(b);
        falha(403, () -> enviar(a, "Outro desativado"));
        falha(403, () -> chat.listar(b, contato, null, null, 30));
        assertEquals(0, chat.naoLidas(a).total());
        social.ativar(b);
        assertEquals(1, chat.naoLidas(b).total());
        assertEquals(1, chat.listar(b, contato, null, null, 30).mensagens().size());
    }

    @Test
    void contagemLeituraERespostasNaoMarcamMensagensFuturas() {
        aceitar(); var uma = enviar(a, "Uma"); var duas = enviar(a, "Duas");
        assertEquals(0, chat.naoLidas(a).total()); assertEquals(2, chat.naoLidas(b).total());
        chat.marcarLidas(b, contato, uma.id());
        assertEquals(1, chat.naoLidas(b).total());
        var tres = enviar(a, "Três");
        chat.marcarLidas(b, contato, duas.id());
        assertEquals(1, chat.naoLidas(b).total());
        chat.marcarLidas(b, contato, uma.id());
        assertEquals(1, chat.naoLidas(b).total());
        chat.marcarLidas(b, contato, tres.id());
        assertEquals(0, chat.naoLidas(b).total());
    }

    @Test
    void leituraNaoAceitaMensagemDeOutroContato() {
        aceitar();
        Long outroContato = contacts.solicitar(a, new SocialContactRequestDTO(terceiro, "Oi")).id();
        contacts.responder(terceiro, outroContato, true);
        var outra = chat.enviar(a, outroContato, new SendPrivateMessageDTO("Outro chat", UUID.randomUUID()));
        falha(400, () -> chat.marcarLidas(b, contato, outra.id()));
        assertTrue(chat.listar(b, contato, null, null, 30).mensagens().isEmpty());
        assertEquals(0, chat.naoLidas(b).total()); assertEquals(1, chat.naoLidas(terceiro).total());
    }

    @Test
    void repetirEnvioComMesmaChaveNaoDuplica() {
        aceitar(); UUID chave = UUID.randomUUID();
        var primeira = chat.enviar(a, contato, new SendPrivateMessageDTO("Olá", chave));
        var repetida = chat.enviar(a, contato, new SendPrivateMessageDTO("Olá", chave));
        assertEquals(primeira.id(), repetida.id()); assertEquals(1, messages.count());
        falha(409, () -> chat.enviar(a, contato, new SendPrivateMessageDTO("Outro texto", chave)));
        assertEquals(1, chat.naoLidas(b).total());
    }

    @Test
    void reenviosSimultaneosContinuamIdempotentes() throws Exception {
        aceitar(); var pedido = new SendPrivateMessageDTO("Reenvio concorrente", UUID.randomUUID());
        var executor = Executors.newFixedThreadPool(2); var inicio = new CountDownLatch(1);
        try {
            var uma = executor.submit(() -> { inicio.await(); return chat.enviar(a, contato, pedido); });
            var duas = executor.submit(() -> { inicio.await(); return chat.enviar(a, contato, pedido); });
            inicio.countDown();
            assertEquals(uma.get(20, TimeUnit.SECONDS).id(), duas.get(20, TimeUnit.SECONDS).id());
            assertEquals(1, messages.count());
        } finally { inicio.countDown(); executor.shutdownNow(); }
    }

    @Test
    void historicoENovidadesSaoPaginadosSemLacunas() {
        aceitar();
        for (int i = 0; i < 65; i++) enviar(a, "Mensagem " + i);
        var recente = chat.listar(b, contato, null, null, 30);
        assertEquals("Mensagem 35", recente.mensagens().get(0).conteudo()); assertTrue(recente.temMais());
        var anterior = chat.listar(b, contato, recente.menorId(), null, 30);
        var primeira = chat.listar(b, contato, anterior.menorId(), null, 30);
        var ids = new HashSet<Long>();
        for (var pagina : List.of(primeira, anterior, recente)) pagina.mensagens().forEach(m -> assertTrue(ids.add(m.id())));
        assertEquals(65, ids.size()); assertFalse(primeira.temMais());
        var nova = enviar(b, "Novidade");
        assertEquals(List.of(nova.id()), chat.listar(a, contato, null, recente.maiorId(), 30).mensagens().stream().map(PrivateMessageDTO::id).toList());
    }

    @Test
    void validacaoDeMensagemECursoresPermaneceNoServidor() {
        aceitar();
        falha(400, () -> chat.enviar(a, contato, null));
        falha(400, () -> enviar(a, "  "));
        falha(400, () -> enviar(a, "x".repeat(1001)));
        falha(400, () -> chat.enviar(a, contato, new SendPrivateMessageDTO("Sem chave", null)));
        falha(400, () -> chat.listar(a, contato, 1L, 1L, 30));
        falha(400, () -> chat.listar(a, contato, null, null, 51));
        assertEquals(1000, enviar(a, "x".repeat(1000)).conteudo().length());
    }

    @Test
    void bloqueioConcorrenteComEnvioNaoDeixaContatoAcessivel() throws Exception {
        aceitar(); var executor = Executors.newFixedThreadPool(2); var inicio = new CountDownLatch(1);
        try {
            var envio = executor.submit(() -> { inicio.await(); try { enviar(a, "Concorrente"); } catch (ResponseStatusException e) { assertEquals(403, e.getStatusCode().value()); } return true; });
            var bloqueio = executor.submit(() -> { inicio.await(); contacts.bloquear(b, a); return true; });
            inicio.countDown(); envio.get(20, TimeUnit.SECONDS); bloqueio.get(20, TimeUnit.SECONDS);
            falha(403, () -> enviar(a, "Após bloquear"));
            falha(403, () -> chat.listar(b, contato, null, null, 30));
            assertEquals(0, chat.naoLidas(b).total());
        } finally { inicio.countDown(); executor.shutdownNow(); }
    }

    @Test
    void ordemDeCommitProtegeCursoresDeMensagensAtrasadas() throws Exception {
        aceitar(); var executor = Executors.newFixedThreadPool(2);
        var inserida = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        try {
            var primeira = executor.submit(() -> new TransactionTemplate(transactionManager).execute(tx -> {
                var mensagem = enviar(a, "Primeira"); inserida.countDown();
                try { if (!liberar.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tempo esgotado"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                return mensagem;
            }));
            assertTrue(inserida.await(10, TimeUnit.SECONDS));
            var segunda = executor.submit(() -> enviar(b, "Segunda"));
            assertThrows(TimeoutException.class, () -> segunda.get(200, TimeUnit.MILLISECONDS));
            liberar.countDown();
            var uma = primeira.get(20, TimeUnit.SECONDS); var duas = segunda.get(20, TimeUnit.SECONDS);
            assertTrue(uma.id() < duas.id());
            assertEquals(duas.id(), chat.listar(a, contato, null, uma.id(), 30).mensagens().get(0).id());
        } finally { liberar.countDown(); executor.shutdownNow(); }
    }

    @Test
    void exclusaoDeContaRemoveMensagensELeituras() {
        aceitar(); var mensagem = enviar(a, "Mensagem"); chat.marcarLidas(b, contato, mensagem.id());
        users.deleteById(a);
        assertEquals(0, messages.count()); assertEquals(0, reads.count());
        assertEquals(0, chat.naoLidas(b).total());
    }

    @Test
    void desativacaoConfirmadaEnquantoEnvioAguardaLockRevogaPermissao() throws Exception {
        aceitar(); var executor = Executors.newFixedThreadPool(2);
        var alterada = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        try {
            var desativa = executor.submit(() -> new TransactionTemplate(transactionManager).execute(tx -> {
                social.desativar(a); alterada.countDown();
                try { if (!liberar.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tempo esgotado"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                return true;
            }));
            assertTrue(alterada.await(10, TimeUnit.SECONDS));
            var envio = executor.submit(() -> { falha(403, () -> enviar(a, "Após desativar")); return true; });
            assertThrows(TimeoutException.class, () -> envio.get(200, TimeUnit.MILLISECONDS));
            liberar.countDown(); desativa.get(20, TimeUnit.SECONDS); envio.get(20, TimeUnit.SECONDS);
            assertEquals(0, messages.count());
        } finally { liberar.countDown(); executor.shutdownNow(); }
    }

    @Test
    void endpointUsaUsuarioAutenticadoENaoPermiteHistoricoDeTerceiro() throws Exception {
        aceitar(); var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var auth = new UsernamePasswordAuthenticationToken(users.findById(a).orElseThrow().getEmail(), "ignorado",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        String url = "/api/v1/community/private-messages/" + contato + "/messages";
        mvc.perform(post(url).principal(auth).contentType("application/json").content(
                "{\"conteudo\":\"Mensagem HTTP\",\"clientMessageId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(a.intValue()));
        var intruso = new UsernamePasswordAuthenticationToken(users.findById(terceiro).orElseThrow().getEmail(), "ignorado",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        mvc.perform(get(url).principal(intruso)).andExpect(status().isNotFound());
        mvc.perform(get(url).principal(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.mensagens.length()").value(1));
    }

    private Long usuario() {
        var user = new User(); user.setNome("Participante de teste"); user.setEmail(UUID.randomUUID() + "@example.com");
        user.setSenha("senha-do-banco-descartavel"); user.setDataNascimento(LocalDate.of(1990, 1, 1)); users.saveAndFlush(user);
        var profile = new RecommendationProfile(); profile.setUser(user); profile.setEstado("MG"); profile.setCidade("São João");
        profile.setTempoDisponivelSemanal(5.0); profile.setOrcamentoInicial(100.0); profile.setTipoSocializacao(TipoSocializacao.SOCIAL);
        profile.setNivelAtividadeFisicaDesejada(NivelAtividadeFisica.BAIXO); profile.setAmbientePreferido(AmbientePreferido.CASA); recommendations.saveAndFlush(profile);
        var social = new SocialProfile(); social.setUser(user); social.setEnabled(true); profiles.saveAndFlush(social);
        return user.getId();
    }
}
