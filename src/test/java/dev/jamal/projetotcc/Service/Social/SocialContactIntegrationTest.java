package dev.jamal.projetotcc.Service.Social;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = SocialContactIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.name=social-contact-integration-isolated",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.sql.init.mode=never"
        })
class SocialContactIntegrationTest {
    @Container
    static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES =
            new org.testcontainers.postgresql.PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("social_contact_integration")
                    .withUsername("integration").withPassword("integration");

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
    @Import({SocialLocationService.class, SocialHubService.class, SocialProfileService.class,
            SocialAffinityService.class, SocialProfileResumeService.class, SocialContactService.class})
    static class Config {}

    @Autowired SocialContactService service;
    @Autowired SocialContactRepository contatos;
    @Autowired SocialUserBlockRepository bloqueios;
    @Autowired SocialHubService hub;
    @Autowired SocialLocationService localizacao;
    @Autowired SocialProfileRepository sociais;
    @Autowired RecommendationProfileRepository perfis;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    private Long a, b, terceiro;

    @BeforeEach
    void preparar() {
        jdbc.execute("TRUNCATE users CASCADE");
        a = usuario(); b = usuario(); terceiro = usuario();
    }

    private SocialContactDTO solicitar(Long autor, Long destino) {
        return service.solicitar(autor, new SocialContactRequestDTO(destino, "Olá! Temos interesses em comum."));
    }

    private void status(int esperado, org.junit.jupiter.api.function.Executable operacao) {
        assertEquals(esperado, assertThrows(ResponseStatusException.class, operacao).getStatusCode().value());
    }

    @Test
    void conversaExigeAceiteDoDestinatario() {
        var pedido = solicitar(a, b);
        assertEquals(SocialContactStatus.PENDENTE, pedido.status());
        status(403, () -> service.validarConversaAceita(a, pedido.id()));
        status(403, () -> service.responder(a, pedido.id(), true));
        status(404, () -> service.responder(terceiro, pedido.id(), true));
        var aceita = service.responder(b, pedido.id(), true);
        assertEquals(SocialContactStatus.ACEITA, aceita.status());
        service.validarConversaAceita(a, pedido.id());
        service.validarConversaAceita(b, pedido.id());
        status(404, () -> service.validarConversaAceita(terceiro, pedido.id()));
    }

    @Test
    void repetirSolicitacaoNaoDuplicaNemTrocaApresentacao() {
        var primeira = solicitar(a, b);
        var repetida = service.solicitar(a, new SocialContactRequestDTO(b, "Outra apresentação"));
        var inversa = solicitar(b, a);
        assertEquals(primeira.id(), repetida.id());
        assertEquals(primeira.apresentacao(), repetida.apresentacao());
        assertEquals(primeira.id(), inversa.id());
        assertEquals("RECEBIDA", inversa.direcao());
        assertEquals(1, contatos.count());
    }

    @Test
    void recusaImpedeReenvioImediatoEPermiteNovaSolicitacaoAposPrazo() {
        var pedido = solicitar(a, b);
        status(403, () -> service.responder(a, pedido.id(), false));
        service.responder(b, pedido.id(), false);
        status(409, () -> solicitar(a, b));
        status(403, () -> service.validarConversaAceita(a, pedido.id()));
        jdbc.update("UPDATE social_contacts SET updated_at=CURRENT_TIMESTAMP - INTERVAL '8 days' WHERE id=?", pedido.id());
        var nova = solicitar(a, b);
        assertEquals(pedido.id(), nova.id());
        assertEquals(SocialContactStatus.PENDENTE, nova.status());
    }

    @Test
    void somenteRemetenteCancelaPedidoPendente() {
        var pedido = solicitar(a, b);
        status(403, () -> service.cancelar(b, pedido.id()));
        service.cancelar(a, pedido.id());
        assertEquals(SocialContactStatus.CANCELADA, contatos.findById(pedido.id()).orElseThrow().getStatus());
        status(409, () -> service.responder(b, pedido.id(), true));
        status(409, () -> solicitar(a, b));
    }

    @Test
    void bloqueioFechaContatoEOcultaPerfisNosDoisSentidos() {
        var pedido = solicitar(a, b);
        service.responder(b, pedido.id(), true);
        service.bloquear(b, a);
        assertEquals(SocialContactStatus.ENCERRADA, contatos.findById(pedido.id()).orElseThrow().getStatus());
        status(403, () -> solicitar(a, b));
        status(403, () -> solicitar(b, a));
        status(403, () -> service.validarConversaAceita(a, pedido.id()));
        assertTrue(service.listar(a, SocialContactBox.ACEITAS, 0).isEmpty());
        assertTrue(hub.buscarUsuarios(a).stream().noneMatch(p -> p.userId().equals(b)));
        assertTrue(hub.buscarUsuarios(b).stream().noneMatch(p -> p.userId().equals(a)));
        status(404, () -> hub.buscarPerfilPublicoPara(a, b));
        assertEquals(a, service.bloqueados(b, 0).getContent().get(0).userId());
    }

    @Test
    void desbloqueioNaoRemoveBloqueioAlheioNemReabreContato() {
        var pedido = solicitar(a, b);
        service.bloquear(a, b);
        service.bloquear(b, a);
        service.desbloquear(a, b);
        assertTrue(bloqueios.bloqueados(a, b));
        service.desbloquear(b, a);
        assertFalse(bloqueios.bloqueados(a, b));
        assertEquals(SocialContactStatus.ENCERRADA, contatos.findById(pedido.id()).orElseThrow().getStatus());
        status(403, () -> service.validarConversaAceita(a, pedido.id()));
    }

    @Test
    void cartaoRespeitaVisibilidadeRegionalAtual() {
        localizacao.atualizarVisibilidade(a, true);
        solicitar(a, b);
        var recebido = service.listar(b, SocialContactBox.RECEBIDAS, 0).getContent().get(0);
        assertEquals(a, recebido.outraPessoa().userId());
        assertEquals("MESMA_CIDADE", recebido.outraPessoa().proximidade());
        assertNotNull(recebido.outraPessoa().afinidade());
        localizacao.atualizarVisibilidade(a, false);
        assertNull(service.listar(b, SocialContactBox.RECEBIDAS, 0).getContent().get(0).outraPessoa().proximidade());
    }

    @Test
    void usuarioDesativadoOuMenorNaoPodeIniciarContato() {
        status(400, () -> solicitar(a, a));
        var perfil = sociais.findByUser_Id(b).orElseThrow();
        perfil.setEnabled(false); sociais.saveAndFlush(perfil);
        status(403, () -> solicitar(a, b));
        status(403, () -> solicitar(b, a));
        var menor = usuarios.findById(terceiro).orElseThrow();
        menor.setDataNascimento(LocalDate.now().minusYears(17)); usuarios.saveAndFlush(menor);
        status(403, () -> solicitar(terceiro, a));
        status(403, () -> solicitar(a, terceiro));
    }

    @Test
    void listasSaoPrivadasEPaginadas() {
        for (int i = 0; i < 13; i++) solicitar(usuario(), b);
        var pagina = service.listar(b, SocialContactBox.RECEBIDAS, 0);
        assertEquals(13, pagina.getTotalElements());
        assertEquals(12, pagina.getContent().size());
        assertEquals(1, service.listar(b, SocialContactBox.RECEBIDAS, 1).getContent().size());
        assertTrue(service.listar(terceiro, SocialContactBox.RECEBIDAS, 0).isEmpty());
    }

    @Test
    void limiteDiarioImpedeDisparoDeSolicitacoes() {
        for (int i = 0; i < 10; i++) solicitar(a, usuario());
        status(429, () -> solicitar(a, b));
        assertEquals(10, contatos.count());
    }

    @Test
    void solicitacoesSimultaneasGeramUmUnicoPar() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(1);
        try {
            var primeira = executor.submit(() -> { inicio.await(); return solicitar(a, b); });
            var segunda = executor.submit(() -> { inicio.await(); return solicitar(b, a); });
            inicio.countDown();
            assertEquals(primeira.get(20, TimeUnit.SECONDS).id(), segunda.get(20, TimeUnit.SECONDS).id());
            assertEquals(1, contatos.count());
        } finally { inicio.countDown(); executor.shutdownNow(); }
    }

    @Test
    void bloqueioConcorrenteComAceiteSempreEncerraContato() throws Exception {
        var pedido = solicitar(a, b);
        var executor = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(1);
        try {
            var aceita = executor.submit(() -> {
                inicio.await();
                try { service.responder(b, pedido.id(), true); }
                catch (ResponseStatusException e) { assertTrue(e.getStatusCode().value() == 403 || e.getStatusCode().value() == 409); }
                return true;
            });
            var bloqueia = executor.submit(() -> { inicio.await(); service.bloquear(a, b); return true; });
            inicio.countDown();
            aceita.get(20, TimeUnit.SECONDS); bloqueia.get(20, TimeUnit.SECONDS);
            assertTrue(bloqueios.bloqueados(a, b));
            assertEquals(SocialContactStatus.ENCERRADA, contatos.findById(pedido.id()).orElseThrow().getStatus());
        } finally { inicio.countDown(); executor.shutdownNow(); }
    }

    @Test
    void exclusaoDeContaRemoveContatosEBloqueiosDependentes() {
        solicitar(a, b);
        service.bloquear(a, b);
        usuarios.deleteById(a);
        assertEquals(0, contatos.count());
        assertEquals(0, bloqueios.count());
    }

    @Test
    void contatosAceitosEmLotePreservamPerfisAfinidadePrivacidadeEOrdem() {
        assertTrue(service.listar(a, SocialContactBox.ACEITAS, 0).isEmpty());
        var pedido = solicitar(a, b);
        assertEquals(pedido.id(), service.listar(b, SocialContactBox.RECEBIDAS, 0).getContent().get(0).id());
        service.responder(b, pedido.id(), true);

        var interest = jdbc.queryForObject("INSERT INTO interest(nome) VALUES('Arte teste') RETURNING id", Long.class);
        jdbc.update("INSERT INTO user_interest(user_id,interest_id,peso) VALUES (?,?,3),(?,?,3)", a, interest, b, interest);
        jdbc.update("UPDATE social_profiles SET interesses_visibilidade='CONTATOS' WHERE user_id IN (?,?)", a, b);
        localizacao.atualizarVisibilidade(b, true);
        var categoria = jdbc.queryForObject("INSERT INTO hobby_category(nome) VALUES('Contatos teste') RETURNING id", Long.class);
        for (String nome : java.util.List.of("Pintura", "Escultura", "Cerâmica")) {
            var hobby = jdbc.queryForObject("""
                    INSERT INTO hobby(nome,ambiente,nivel_atividade_fisica,tipo_socializacao,requer_equipamento,category_id,tempo_necessario)
                    VALUES (?,'CASA','BAIXO','SOCIAL',false,?,2) RETURNING id
                    """, Long.class, nome, categoria);
            if (!nome.equals("Escultura")) jdbc.update("""
                    INSERT INTO user_hobbies(user_id,hobby_id,nivel_atual,status_atual,criado_em,atualizado_em)
                    VALUES (?,?,'INICIANTE','PRATICANDO',now(),now()),(?,?,'INICIANTE','PRATICANDO',now(),now())
                    """, a, hobby, b, hobby);
            jdbc.update("""
                    INSERT INTO user_recommendation_feedbacks(user_id,hobby_id,tipo,created_at)
                    VALUES (?,?,'INTERESSADO',now()),(?,?,'INTERESSADO',now())
                    """, a, hobby, b, hobby);
        }
        var unica = service.listar(a, SocialContactBox.ACEITAS, 0).getContent().get(0);
        assertEquals(hub.buscarPerfilPublicoPara(a,b), unica.outraPessoa());
        assertEquals(9, unica.outraPessoa().afinidade().score());
        assertEquals("MESMA_CIDADE", unica.outraPessoa().proximidade());
        var rejeitada = solicitar(terceiro,a);
        service.responder(a, rejeitada.id(), false);
        for (int i=0; i<12; i++) {
            var outro = usuario();
            jdbc.update("""
                    INSERT INTO social_contacts(low_user_id,high_user_id,requester_id,recipient_id,status,requested_at,updated_at)
                    VALUES (?,?,?,?,'ACEITA',now(),now())
                    """, a, outro, a, outro);
        }
        var primeira = service.listar(a, SocialContactBox.ACEITAS, 0);
        var segunda = service.listar(a, SocialContactBox.ACEITAS, 1);
        assertEquals(13, primeira.getTotalElements());
        assertEquals(12, primeira.getNumberOfElements());
        assertEquals(1, segunda.getNumberOfElements());
        assertEquals(unica, segunda.getContent().get(0));
        assertTrue(service.listar(terceiro, SocialContactBox.ACEITAS, 0).isEmpty());
        assertEquals(rejeitada.id(), service.listar(a, SocialContactBox.HISTORICO, 0).getContent().get(0).id());

        jdbc.update("UPDATE social_profiles SET interesses_visibilidade='SOMENTE_EU' WHERE user_id=?", b);
        var privado = service.listar(a, SocialContactBox.ACEITAS, 1).getContent().get(0).outraPessoa();
        assertTrue(privado.interesses().isEmpty());
        assertEquals(hub.buscarPerfilPublicoPara(a,b), privado);
        assertEquals(8, privado.afinidade().score());

        jdbc.update("UPDATE social_profiles SET enabled=false WHERE user_id=?", b);
        var inativo = service.listar(a, SocialContactBox.ACEITAS, 1).getContent().get(0);
        assertFalse(inativo.disponivel());
        assertNull(inativo.outraPessoa());
        status(403, () -> service.listar(b, SocialContactBox.ACEITAS, 0));
    }

    private Long usuario() {
        var user = new User();
        user.setNome("Participante de teste");
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setSenha("senha-do-banco-descartavel");
        user.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuarios.saveAndFlush(user);
        var profile = new RecommendationProfile();
        profile.setUser(user); profile.setEstado("MG"); profile.setCidade("São João");
        profile.setTempoDisponivelSemanal(5.0); profile.setOrcamentoInicial(100.0);
        profile.setTipoSocializacao(TipoSocializacao.SOCIAL);
        profile.setNivelAtividadeFisicaDesejada(NivelAtividadeFisica.BAIXO);
        profile.setAmbientePreferido(AmbientePreferido.CASA);
        perfis.saveAndFlush(profile);
        var social = new SocialProfile(); social.setUser(user); social.setEnabled(true);
        sociais.saveAndFlush(social);
        return user.getId();
    }
}

