package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.Controllers.SocialProfileEditorController;
import dev.jamal.projetotcc.DTO.Social.SocialProfileEditDTO;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Enum.SocialVisibility;
import dev.jamal.projetotcc.Repository.SocialProfilePhotoRepository;
import dev.jamal.projetotcc.Repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.*;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.awt.Color;
import java.util.Arrays;

@Testcontainers
@SpringBootTest(classes=SocialProfileIntegrationTest.Config.class, webEnvironment=SpringBootTest.WebEnvironment.NONE,
 properties={"spring.config.name=social-profile-isolated", "spring.jpa.hibernate.ddl-auto=validate",
 "spring.flyway.enabled=true", "spring.flyway.baseline-on-migrate=false", "spring.sql.init.mode=never"})
class SocialProfileIntegrationTest {
    @Container static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES =
        new org.testcontainers.postgresql.PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("social_profile_test").withUsername("integration").withPassword("integration");
    public static class SqlCapture implements StatementInspector {
        static final ThreadLocal<List<String>> statements = new ThreadLocal<>();

        @Override
        public String inspect(String sql) {
            var captured = statements.get();
            if (captured != null) captured.add(sql);
            return sql;
        }
    }

    @DynamicPropertySource static void banco(DynamicPropertyRegistry r) {
        r.add("spring.jpa.properties.hibernate.session_factory.statement_inspector", SqlCapture.class::getName);
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl); r.add("spring.datasource.username",POSTGRES::getUsername);
        r.add("spring.datasource.password",POSTGRES::getPassword); r.add("spring.flyway.url",POSTGRES::getJdbcUrl);
        r.add("spring.flyway.user",POSTGRES::getUsername); r.add("spring.flyway.password",POSTGRES::getPassword);
    }
    @Configuration(proxyBeanMethods=false) @EnableAutoConfiguration
    @EntityScan("dev.jamal.projetotcc.Entities") @EnableJpaRepositories("dev.jamal.projetotcc.Repository")
    @Import({SocialProfileService.class,SocialProfileEditorService.class,SocialHubService.class,
        SocialLocationService.class,SocialAffinityService.class,SocialProfileResumeService.class})
    static class Config {}
    @Autowired SocialProfileEditorService editor;
    @Autowired SocialProfileService participation;
    @Autowired SocialHubService hub;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired SocialProfilePhotoRepository photos;

    @Test
    void consultaCondicionalConfirmaVersaoSemRetornarBytes() throws Exception {
        editor.salvarFoto(id, gerarFoto());
        var consultas = new ArrayList<String>();
        SqlCapture.statements.set(consultas);
        try {
            var atual = editor.miniaturaCondicional(id, id, "");
            assertEquals(java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(atual.imagem())), atual.versao());
            assertNull(editor.miniaturaCondicional(id, id, "0".repeat(64) + "," + atual.versao()).imagem());
            assertNull(editor.miniaturaCondicional(id, id, "*").imagem());
            assertEquals(3, consultas.stream().filter(sql -> sql.contains("sha256(miniatura)")).count());
            assertFalse(consultas.stream().anyMatch(sql -> sql.matches("(?is).*\\bfoto\\b.*")), consultas.toString());
        } finally {
            SqlCapture.statements.remove();
        }
    }

    @Test
    void auditoriaHttpContaCorposBinariosEConsultasComPostgresReal() throws Exception {
        editor.salvarFoto(id, gerarFoto(768, 512, Color.BLUE));
        var principal = new UsernamePasswordAuthenticationToken(
                users.findById(id).orElseThrow().getEmail(), null, List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new SocialProfileEditorController(editor, hub, users)).build();
        String endpoint = "/api/v1/social/profiles/" + id;
        var consultas = new ArrayList<String>();
        MockHttpServletResponse inicial;
        MockHttpServletResponse revalidada;
        SqlCapture.statements.set(consultas);
        int consultasIniciais;
        try {
            inicial = mvc.perform(get(endpoint + "/thumbnail").principal(principal)).andReturn().getResponse();
            consultasIniciais = consultas.size();
            revalidada = mvc.perform(get(endpoint + "/thumbnail").principal(principal)
                    .header("If-None-Match", inicial.getHeader("ETag"))).andReturn().getResponse();
        } finally {
            SqlCapture.statements.remove();
        }
        assertEquals(200, inicial.getStatus());
        assertEquals(304, revalidada.getStatus());
        assertEquals(0, revalidada.getContentAsByteArray().length);
        assertEquals(consultasIniciais * 2, consultas.size());
        assertEquals(2, consultas.stream().filter(sql -> sql.contains("sha256(miniatura)")).count());
        assertFalse(consultas.stream().anyMatch(sql -> sql.matches("(?is).*\\bfoto\\b.*")), consultas.toString());
        assertTrue(inicial.getContentAsByteArray().length > 0);

        var foto = mvc.perform(get(endpoint + "/photo").principal(principal)).andReturn().getResponse();
        assertEquals(200, foto.getStatus());
        assertTrue(foto.getContentAsByteArray().length > inicial.getContentAsByteArray().length);

        editor.salvarFoto(id, gerarFoto(768, 512, Color.RED));
        var alterada = mvc.perform(get(endpoint + "/thumbnail").principal(principal)
                .header("If-None-Match", inicial.getHeader("ETag"))).andReturn().getResponse();
        assertEquals(200, alterada.getStatus());
        assertNotEquals(inicial.getHeader("ETag"), alterada.getHeader("ETag"));
        assertFalse(Arrays.equals(inicial.getContentAsByteArray(), alterada.getContentAsByteArray()));

        editor.removerFoto(id);
        var removida = mvc.perform(get(endpoint + "/thumbnail").principal(principal)
                .header("If-None-Match", alterada.getHeader("ETag"))).andReturn().getResponse();
        assertEquals(404, removida.getStatus());
        assertNull(removida.getHeader("ETag"));

        System.out.printf("Auditoria etapa 8: GET inicial e revalidacao %d/%d, corpos %d/%d bytes, "
                        + "SQL %d/%d, ampliacao %d bytes%n", inicial.getStatus(), revalidada.getStatus(),
                inicial.getContentAsByteArray().length, revalidada.getContentAsByteArray().length,
                consultasIniciais, consultas.size() - consultasIniciais, foto.getContentAsByteArray().length);
    }
    @Test
    void revalidaOriginalSemDevolverBinarioEConfirmaMudancas() throws Exception {
        byte[] original = gerarFoto(768, 512, Color.BLUE);
        editor.salvarFoto(id, original);
        var primeira = editor.fotoCondicional(id, id, "");
        assertArrayEquals(original, primeira.imagem());
        assertEquals("photo-" + java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(original)), primeira.versao());
        assertNull(editor.fotoCondicional(id, id, primeira.versao()).imagem());
        assertNotNull(editor.fotoCondicional(id, id, editor.miniaturaCondicional(id, id, "").versao()).imagem());

        byte[] nova = gerarFoto(768, 512, Color.RED);
        editor.salvarFoto(id, nova);
        var alterada = editor.fotoCondicional(id, id, primeira.versao());
        assertNotEquals(primeira.versao(), alterada.versao());
        assertArrayEquals(nova, alterada.imagem());

        editor.removerFoto(id);
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> editor.fotoCondicional(id, id, alterada.versao())).getStatusCode().value());
    }

    Long id;
    @BeforeEach void setup() {
        jdbc.execute("TRUNCATE users CASCADE");
        var u=new User(); u.setNome("Pessoa de teste"); u.setEmail(UUID.randomUUID()+"@example.test");
        u.setSenha("test-only"); u.setDataNascimento(LocalDate.of(1990,1,1));
        id=users.saveAndFlush(u).getId(); participation.ativar(id);
    }
    @Test
    void migrationPersisteBioVisibilidadeEFotoEPermiteRemocao() {

        var settings = new SocialProfileEditDTO(
                "Minha biografia",
                SocialVisibility.CONTATOS,
                SocialVisibility.SOMENTE_EU,
                SocialVisibility.PARTICIPANTES,
                SocialVisibility.CONTATOS
        );

        assertEquals(settings, editor.salvar(id, settings));

        byte[] imagem = gerarFoto();

        editor.salvarFoto(id, imagem);

        assertArrayEquals(imagem, editor.foto(id, id));

        assertTrue(
                hub.buscarPerfilPublicoPara(id, id).possuiFoto()
        );

        editor.removerFoto(id);

        assertFalse(
                hub.buscarPerfilPublicoPara(id, id).possuiFoto()
        );

        assertThrows(
                ResponseStatusException.class,
                () -> editor.foto(id, id)
        );
    }
    @Test void operacoesTextuaisEDescobertaNaoTransferemBytesDaFoto() {
        byte[] png = gerarFoto();
        editor.salvarFoto(id, png);
        var viewer = new User();
        viewer.setNome("Visitante");
        viewer.setEmail(UUID.randomUUID() + "@example.test");
        viewer.setSenha("test-only");
        viewer.setDataNascimento(LocalDate.of(1990, 1, 1));
        Long viewerId = users.saveAndFlush(viewer).getId();
        participation.ativar(viewerId);

        var settings = new SocialProfileEditDTO("Bio revisada", SocialVisibility.PARTICIPANTES,
                SocialVisibility.PARTICIPANTES, SocialVisibility.CONTATOS, SocialVisibility.SOMENTE_EU);
        var statements = new ArrayList<String>();
        SqlCapture.statements.set(statements);
        try {
            assertTrue(users.findById(id).isPresent());
            assertTrue(participation.elegivel(id));
            assertTrue(participation.participa(id));
            assertEquals(settings, editor.salvar(id, settings));
            assertEquals(settings, editor.buscar(id));
            var visible = hub.buscarPerfilPublicoPara(viewerId, id);
            assertEquals("Bio revisada", visible.biografia());
            assertTrue(visible.possuiFoto());
            assertTrue(hub.buscarUsuarios(viewerId).stream().anyMatch(p -> p.userId().equals(id) && p.possuiFoto()));
        } finally {
            SqlCapture.statements.remove();
        }

        var profileSql = statements.stream()
                .filter(sql -> sql.contains("social_profiles"))
                .toList();

        assertTrue(
                profileSql.stream()
                        .anyMatch(sql -> sql.startsWith("update")),
                profileSql::toString
        );

        assertTrue(
                profileSql.stream()
                        .anyMatch(sql -> sql.contains("social_profile_photos")),
                profileSql::toString
        );

        for (String sql : statements) {

            assertFalse(
                    sql.matches("(?is).*\\b(foto|miniatura)\\b.*"),
                    sql
            );
        }
        assertArrayEquals(png, editor.foto(id, id));
    }

    @Test
    void exclusaoDoUsuarioRemovePerfilSocialEmCascata() {

        editor.salvarFoto(id, gerarFoto());

        Long profileId = jdbc.queryForObject("""
        SELECT id
        FROM social_profiles
        WHERE user_id = ?
        """, Long.class, id);

        assertEquals(1L, jdbc.queryForObject("""
        SELECT count(*)
        FROM social_profile_photos
        WHERE profile_id = ?
        """, Long.class, profileId));

        users.deleteById(id);

        assertFalse(users.existsById(id));
        assertFalse(participation.participa(id));

        assertEquals(0L, jdbc.queryForObject("""
        SELECT count(*)
        FROM social_profile_photos
        WHERE profile_id = ?
        """, Long.class, profileId));
    }

    @Test void desativacaoImpedeAlteracaoDaBiografiaEDaFoto() {
        var settings=editor.buscar(id); participation.desativar(id);
        assertThrows(ResponseStatusException.class,()->editor.salvar(id,settings));
        assertThrows(ResponseStatusException.class,()->editor.salvarFoto(id, gerarFoto()));
    }
    @Test void defaultsPreservamVisibilidadeAnteriorEReativacaoPreservaBio() {
        assertEquals(SocialVisibility.PARTICIPANTES,editor.buscar(id).hobbiesVisibilidade());
        var p=new SocialProfileEditDTO("Bio preservada",SocialVisibility.SOMENTE_EU,
            SocialVisibility.CONTATOS,SocialVisibility.SOMENTE_EU,SocialVisibility.SOMENTE_EU);
        editor.salvar(id,p); participation.desativar(id); participation.ativar(id);
        assertEquals(p,editor.buscar(id));
    }

    private byte[] gerarFoto() {
        try {
            var imagem = new BufferedImage(
                    100, 100,
                    BufferedImage.TYPE_INT_RGB
            );

            var output = new ByteArrayOutputStream();

            ImageIO.write(imagem, "jpeg", output);

            return output.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] gerarFoto(
            int largura,
            int altura,
            Color cor
    ) throws IOException {

        var imagem = new BufferedImage(
                largura,
                altura,
                BufferedImage.TYPE_INT_RGB
        );

        var graphics = imagem.createGraphics();

        try {

            graphics.setColor(cor);
            graphics.fillRect(0, 0, largura, altura);

        } finally {
            graphics.dispose();
        }

        var output = new ByteArrayOutputStream();

        ImageIO.write(imagem, "jpeg", output);

        return output.toByteArray();
    }

    @Test
    void uploadGeraMiniaturaComDimensoesCorretas()
            throws Exception {

        byte[] original = gerarFoto(
                768, 512, Color.BLUE
        );

        editor.salvarFoto(id, original);

        byte[] miniatura = editor.miniatura(id, id);

        var imagem = ImageIO.read(
                new ByteArrayInputStream(miniatura)
        );

        assertNotNull(imagem);

        assertEquals(128, imagem.getWidth());
        assertEquals(85, imagem.getHeight());

        assertTrue(miniatura.length < original.length);

        assertArrayEquals(
                original,
                editor.foto(id, id)
        );

        assertEquals((byte) 0xFF, miniatura[0]);
        assertEquals((byte) 0xD8, miniatura[1]);
        assertEquals((byte) 0xFF, miniatura[2]);
    }

    @Test
    void substituicaoAtualizaFotoEMiniatura()
            throws Exception {

        byte[] primeira = gerarFoto(
                768, 512, Color.BLUE
        );

        editor.salvarFoto(id, primeira);

        byte[] miniaturaAnterior = editor.miniatura(id, id);

        byte[] segunda = gerarFoto(
                768, 512, Color.RED
        );

        editor.salvarFoto(id, segunda);

        byte[] novaMiniatura = editor.miniatura(id, id);

        assertArrayEquals(
                segunda,
                editor.foto(id, id)
        );

        assertFalse(
                Arrays.equals(primeira, segunda)
        );

        assertFalse(
                Arrays.equals(
                        miniaturaAnterior,
                        novaMiniatura
                )
        );
    }

    @Test
    void remocaoEliminaFotoEMiniatura() {

        editor.salvarFoto(id, gerarFoto());

        assertNotNull(editor.foto(id, id));
        assertNotNull(editor.miniatura(id, id));

        editor.removerFoto(id);

        assertThrows(
                ResponseStatusException.class,
                () -> editor.foto(id, id)
        );

        assertThrows(
                ResponseStatusException.class,
                () -> editor.miniatura(id, id)
        );

        assertEquals(0L, jdbc.queryForObject("""
        SELECT count(*)
        FROM social_profile_photos ph
        JOIN social_profiles p
            ON p.id = ph.profile_id
        WHERE p.user_id = ?
        """, Long.class, id));
    }

    @Test
    void backfillPreservaFotoOriginalEIdempotencia()
            throws Exception {

        byte[] original = gerarFoto(
                768, 512, Color.GREEN
        );

        editor.salvarFoto(id, original);

        jdbc.update("""
        UPDATE social_profile_photos
        SET miniatura = NULL
        WHERE profile_id = (
            SELECT id
            FROM social_profiles
            WHERE user_id = ?
        )
        """, id);

        assertThrows(
                ResponseStatusException.class,
                () -> editor.miniatura(id, id)
        );

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> editor.miniaturaCondicional(id, id, "*")).getStatusCode().value());

        var runner = new SocialThumbnailBackfillRunner(editor, photos);
        assertEquals(new SocialThumbnailBackfillRunner.Resultado(1, 0, 0, 0), runner.preencher());

        assertNotNull(editor.miniatura(id, id));

        assertArrayEquals(
                original,
                editor.foto(id, id)
        );

        assertEquals(new SocialThumbnailBackfillRunner.Resultado(0, 0, 0, 0), runner.preencher());
    }


    @Test
    void backfillIsolaImagensInvalidasNoInicioNoMeioENoFim() {
        byte[] original = gerarFoto();
        byte[] truncada = java.util.HexFormat.of().parseHex("89504e470d0a1a0a");
        var runner = new SocialThumbnailBackfillRunner(editor, photos);
        var invalidas = List.of(0, 20, 24);
        var ids = new ArrayList<Long>();

        for (int i = 0; i < 25; i++) {
            long profileId = novoPerfilParaBackfill();
            ids.add(profileId);
            byte[] foto = i == 20 ? new byte[]{0, 1, 2, 3} : invalidas.contains(i) ? truncada : original;
            jdbc.update("""
                    INSERT INTO social_profile_photos (profile_id, foto, content_type, updated_at)
                    VALUES (?, ?, ?, timestamp '2026-01-01 00:00:00')
                    """, profileId, foto, invalidas.contains(i) ? "image/png" : "image/jpeg");
        }

        var antes = originaisDoBackfill();
        assertEquals(new SocialThumbnailBackfillRunner.Resultado(22, 0, 3, 3), runner.preencher());
        for (int i = 0; i < ids.size(); i++) {
            assertEquals(invalidas.contains(i), photos.findThumbnail(ids.get(i)).isEmpty());
        }

        var miniaturas = jdbc.queryForList("""
                SELECT profile_id, encode(sha256(miniatura), 'hex') AS hash
                FROM social_profile_photos ORDER BY profile_id
                """);
        assertEquals(new SocialThumbnailBackfillRunner.Resultado(0, 0, 3, 3), runner.preencher());
        assertEquals(antes, originaisDoBackfill());
        assertEquals(miniaturas, jdbc.queryForList("""
                SELECT profile_id, encode(sha256(miniatura), 'hex') AS hash
                FROM social_profile_photos ORDER BY profile_id
                """));
    }

    @Test
    void backfillIgnoraSubstituicaoRemocaoEReinsercaoDepoisDaLeitura() throws IOException {
        byte[] original = gerarFoto();
        long substituida = inserirFotoPendente(original);
        long removida = inserirFotoPendente(original);
        long reinserida = inserirFotoPendente(original);
        byte[] novaMiniatura = gerarFoto(48, 32, Color.BLUE);

        var editorComIntercalacao = org.mockito.Mockito.mock(SocialProfileEditorService.class);
        var ordem = List.of(substituida, removida, reinserida).iterator();
        org.mockito.Mockito.doAnswer(invocation -> {
            long profileId = ordem.next();
            if (profileId == substituida) {
                jdbc.update("UPDATE social_profile_photos SET foto = ?, miniatura = ? WHERE profile_id = ?",
                        novaMiniatura, novaMiniatura, profileId);
            } else {
                jdbc.update("DELETE FROM social_profile_photos WHERE profile_id = ?", profileId);
                if (profileId == reinserida) {
                    jdbc.update("""
                            INSERT INTO social_profile_photos (profile_id, foto, content_type, updated_at)
                            VALUES (?, ?, 'image/jpeg', timestamp '2026-01-01 00:00:00')
                            """, profileId, original);
                }
            }
            return editor.gerarMiniatura(invocation.getArgument(0));
        }).when(editorComIntercalacao).gerarMiniatura(org.mockito.ArgumentMatchers.any(byte[].class));

        assertEquals(new SocialThumbnailBackfillRunner.Resultado(0, 3, 0, 1),
                new SocialThumbnailBackfillRunner(editorComIntercalacao, photos).preencher());
        assertArrayEquals(novaMiniatura, photos.findThumbnail(substituida).orElseThrow());
        assertTrue(photos.findPhotoData(removida).isEmpty());
        assertArrayEquals(original, photos.findPhotoData(reinserida).orElseThrow().getFoto());
        assertTrue(photos.findThumbnail(reinserida).isEmpty());
    }

    @Test
    void backfillPropagaFalhaDoBancoEPreservaEscritaAnterior() {
        byte[] original = gerarFoto();
        long primeira = inserirFotoPendente(original);
        long bloqueada = inserirFotoPendente(original);
        long seguinte = inserirFotoPendente(original);
        var runner = new SocialThumbnailBackfillRunner(editor, photos);

        jdbc.execute("""
                ALTER TABLE social_profile_photos ADD CONSTRAINT teste_falha_backfill
                CHECK (profile_id <> %d OR miniatura IS NULL)
                """.formatted(bloqueada));
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, runner::preencher);
            assertTrue(photos.findThumbnail(primeira).isPresent());
            assertTrue(photos.findThumbnail(bloqueada).isEmpty());
            assertTrue(photos.findThumbnail(seguinte).isEmpty());
        } finally {
            jdbc.execute("ALTER TABLE social_profile_photos DROP CONSTRAINT teste_falha_backfill");
        }

        assertEquals(new SocialThumbnailBackfillRunner.Resultado(2, 0, 0, 0), runner.preencher());
    }

    private List<java.util.Map<String, Object>> originaisDoBackfill() {
        return jdbc.queryForList("""
                SELECT profile_id, content_type, updated_at, encode(sha256(foto), 'hex') AS hash
                FROM social_profile_photos ORDER BY profile_id
                """);
    }

    private long inserirFotoPendente(byte[] original) {
        long profileId = novoPerfilParaBackfill();
        jdbc.update("""
                INSERT INTO social_profile_photos (profile_id, foto, content_type, updated_at)
                VALUES (?, ?, 'image/jpeg', timestamp '2026-01-01 00:00:00')
                """, profileId, original);
        return profileId;
    }

    private long novoPerfilParaBackfill() {
        var user = new User();
        user.setNome("Pessoa de teste");
        user.setEmail(UUID.randomUUID() + "@example.test");
        user.setSenha("test-only");
        user.setDataNascimento(LocalDate.of(1990, 1, 1));
        long userId = users.saveAndFlush(user).getId();
        participation.ativar(userId);
        return jdbc.queryForObject("SELECT id FROM social_profiles WHERE user_id = ?", Long.class, userId);
    }


    @Test
    void consultaDeMiniaturaNaoRecuperaFotoCompleta() {

        editor.salvarFoto(id, gerarFoto());

        var statements = new ArrayList<String>();

        SqlCapture.statements.set(statements);

        try {

            assertNotNull(editor.miniatura(id, id));

        } finally {

            SqlCapture.statements.remove();
        }

        var consultas = statements.stream()
                .filter(sql -> sql.contains(
                        "social_profile_photos"
                ))
                .toList();

        assertFalse(
                consultas.isEmpty(),
                statements::toString
        );

        assertTrue(
                consultas.stream().anyMatch(
                        sql -> sql.matches(
                                "(?is).*\\bminiatura\\b.*"
                        )
                ),
                consultas::toString
        );

        for (String sql : consultas) {

            assertFalse(
                    sql.matches("(?is).*\\bfoto\\b.*"),
                    sql
            );
        }
    }
}
