package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import db.migration.V31__reabrir_questionarios_inconsistentes;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
class QuestionarioConclusaoIntegrationTest {

    private static final String PREFERENCIAS = """
            {"tempoDisponivelSemanal":3,"orcamentoInicial":0,"tipoSocializacao":"INDIVIDUAL",
             "nivelAtividadeFisicaDesejada":"BAIXO","ambientePreferido":"CASA"
            """.stripTrailing();
    private static final String PERFIL = PREFERENCIAS + ",\"nivelExperiencia\":\"INICIANTE\"}";
    private static final String INSERT_USER = """
            INSERT INTO users (nome, email, senha, data_nascimento)
            VALUES ('Pessoa teste', ?, 'hash-ficticio', DATE '1990-01-01') RETURNING id
            """;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("questionario").withUsername("integration").withPassword("integration");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired Flyway flyway;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void putUsaEstadoResultanteEPostConservaOContrato() throws Exception {
        Long id = jdbc.queryForObject(INSERT_USER, Long.class, "questionario@example.test");
        User user = users.findById(id).orElseThrow();
        String auth = "Bearer " + jwt.gerarToken(user);
        String perfil = "/api/v1/users/" + id + "/profile";
        String questionario = "/api/v1/recommendation-profiles/" + id;
        String status = questionario + "/status";

        mvc.perform(get(status).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(content().string("false"));
        mvc.perform(put(perfil).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(PREFERENCIAS + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(perfil).header("Authorization", auth).contentType(APPLICATION_JSON).content(PERFIL))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questionarioConcluido").value(false));

        for (String localizacao : List.of("", ",\"cidade\":\"São Paulo\"", ",\"estado\":\"SP\"",
                ",\"cidade\":\" \",\"estado\":\"SP\"", ",\"cidade\":\"São Paulo\",\"estado\":\"\"",
                ",\"cidade\":\"Cidade 1\",\"estado\":\"SP\"", ",\"cidade\":\"São Paulo\",\"estado\":\"ZZ\"")) {
            mvc.perform(post(questionario).header("Authorization", auth).contentType(APPLICATION_JSON)
                            .content(PREFERENCIAS + localizacao + "}"))
                    .andExpect(status().isBadRequest());
        }

        jdbc.update("UPDATE recommendation_profile SET cidade = 'São Paulo' WHERE user_id = ?", id);
        mvc.perform(put(perfil).header("Authorization", auth).contentType(APPLICATION_JSON).content(PERFIL))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questionarioConcluido").value(false));
        jdbc.update("UPDATE recommendation_profile SET cidade = NULL, estado = 'SP' WHERE user_id = ?", id);
        mvc.perform(put(perfil).header("Authorization", auth).contentType(APPLICATION_JSON).content(PERFIL))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questionarioConcluido").value(false));
        mvc.perform(get(status).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(content().string("false"));

        mvc.perform(post(questionario).header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content(PREFERENCIAS + ",\"cidade\":\" São Paulo \",\"estado\":\"sp\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.questionarioConcluido").value(true));
        mvc.perform(put(perfil).header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content(PERFIL.replace(":3", ":7")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questionarioConcluido").value(true));
        mvc.perform(get(status).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(content().string("true"));
        mvc.perform(get(questionario).header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cidade").value("São Paulo")).andExpect(jsonPath("$.estado").value("SP"))
                .andExpect(jsonPath("$.tempoDisponivelSemanal").value(7));
    }

    @Test
    void jsonNumericoMantemCoercaoERejeitaEntradasInvalidasSemAlterarPerfil() throws Exception {
        Long id = jdbc.queryForObject(INSERT_USER, Long.class, "numeros-json@example.test");
        String auth = "Bearer " + jwt.gerarToken(users.findById(id).orElseThrow());
        String questionario = "/api/v1/recommendation-profiles/" + id;
        String payload = PREFERENCIAS + ",\"cidade\":\"São Paulo\",\"estado\":\"SP\"}";

        mvc.perform(post(questionario).header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content(payload.replace(":3", ":\"3\"").replace(":0", ":\"0\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tempoDisponivelSemanal").isNumber())
                .andExpect(jsonPath("$.orcamentoInicial").isNumber());
        for (String budget : List.of("null", "-1", "\"abc\"")) {
            mvc.perform(post(questionario).header("Authorization", auth).contentType(APPLICATION_JSON)
                            .content(payload.replace(":0", ":" + budget)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(questionario).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orcamentoInicial").value(0))
                .andExpect(jsonPath("$.questionarioConcluido").value(true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void referenciasInexistentesPreservamOQuestionarioEPermitemNovaSubmissao(boolean concluido) throws Exception {
        Long id = jdbc.queryForObject(INSERT_USER, Long.class, "referencias-" + concluido + "@example.test");
        String auth = "Bearer " + jwt.gerarToken(users.findById(id).orElseThrow());
        String questionario = "/api/v1/recommendation-profiles/" + id;
        Long interesseAnterior = jdbc.queryForObject(
                "INSERT INTO interest (nome) VALUES (?) RETURNING id", Long.class, "Anterior " + concluido);
        Long interesseNovo = jdbc.queryForObject(
                "INSERT INTO interest (nome) VALUES (?) RETURNING id", Long.class, "Novo " + concluido);
        Long objetivoAnterior = jdbc.queryForObject(
                "INSERT INTO objectives (nome) VALUES (?) RETURNING id", Long.class, "Anterior " + concluido);
        Long objetivoNovo = jdbc.queryForObject(
                "INSERT INTO objectives (nome) VALUES (?) RETURNING id", Long.class, "Novo " + concluido);
        long inexistente = Long.MAX_VALUE;
        String inicial = PREFERENCIAS + """
                ,"cidade":"São Paulo","estado":"SP",
                "interestAssociations":[{"interestId":%d,"peso":2}],
                "objectiveAssociations":[{"objectiveId":%d,"peso":3}]}
                """.formatted(interesseAnterior, objetivoAnterior);
        mvc.perform(post(questionario).header("Authorization", auth).contentType(APPLICATION_JSON).content(inicial))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interestAssociations[0].peso").value(2))
                .andExpect(jsonPath("$.objectiveAssociations[0].peso").value(3));
        if (!concluido) {
            jdbc.update("UPDATE recommendation_profile SET questionario_concluido = false WHERE user_id = ?", id);
        }
        String antes = estadoPersistido(id);
        String alterado = PREFERENCIAS.replace(":3", ":8") + ",\"cidade\":\"Curitiba\",\"estado\":\"PR\"";
        String interesseValido = ",\"interestIds\":[%d]".formatted(interesseNovo);
        String objetivoValido = ",\"objectiveIds\":[%d]".formatted(objetivoNovo);

        for (String interesses : List.of(
                ",\"interestIds\":[%d]".formatted(inexistente),
                ",\"interestIds\":[%d,%d]".formatted(interesseNovo, inexistente),
                ",\"interestAssociations\":[{\"interestId\":%d,\"peso\":3}]".formatted(inexistente))) {
            mvc.perform(post(questionario).header("Authorization", auth).contentType(APPLICATION_JSON)
                            .content(alterado + interesses + objetivoValido + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Interesse não encontrado: " + inexistente));
            assertEquals(antes, estadoPersistido(id));
        }

        for (String objetivos : List.of(
                ",\"objectiveIds\":[%d]".formatted(inexistente),
                ",\"objectiveIds\":[%d,%d]".formatted(objetivoNovo, inexistente),
                ",\"objectiveAssociations\":[{\"objectiveId\":%d,\"peso\":2}]".formatted(inexistente))) {
            mvc.perform(post(questionario).header("Authorization", auth).contentType(APPLICATION_JSON)
                            .content(alterado + interesseValido + objetivos + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Objetivo não encontrado: " + inexistente));
            assertEquals(antes, estadoPersistido(id));
        }

        mvc.perform(post(questionario).header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content(alterado + interesseValido + objetivoValido + "}"))
                .andExpect(status().isCreated());
        mvc.perform(get(questionario).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempoDisponivelSemanal").value(8))
                .andExpect(jsonPath("$.cidade").value("Curitiba"))
                .andExpect(jsonPath("$.estado").value("PR"))
                .andExpect(jsonPath("$.questionarioConcluido").value(true))
                .andExpect(jsonPath("$.interestIds.length()").value(1))
                .andExpect(jsonPath("$.interestIds[0]").value(interesseNovo.intValue()))
                .andExpect(jsonPath("$.objectiveIds.length()").value(1))
                .andExpect(jsonPath("$.objectiveIds[0]").value(objetivoNovo.intValue()))
                .andExpect(jsonPath("$.interestAssociations[0].peso").value(1))
                .andExpect(jsonPath("$.objectiveAssociations[0].peso").value(1));
    }

    private String estadoPersistido(Long userId) {
        return jdbc.queryForObject("""
                SELECT jsonb_build_object(
                    'perfil', to_jsonb(p),
                    'interesses', (SELECT jsonb_agg(to_jsonb(i) ORDER BY i.interest_id)
                                   FROM user_interest i WHERE i.user_id = p.user_id),
                    'objetivos', (SELECT jsonb_agg(to_jsonb(o) ORDER BY o.objective_id)
                                  FROM user_objectives o WHERE o.user_id = p.user_id)
                )::text
                FROM recommendation_profile p WHERE p.user_id = ?
                """, String.class, userId);
    }

    @Test
    void v31EmInstalacaoLimpaEUpgradeCorrigeApenasFlagsEPermiteRollbackTransacional() throws Exception {
        flyway.validate();
        assertEquals("31", flyway.info().current().getVersion().getVersion());
        jdbc.execute("CREATE DATABASE questionario_upgrade");
        String url = POSTGRES.getJdbcUrl().replace("/questionario", "/questionario_upgrade");
        var config = Flyway.configure().dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
        assertEquals(30, config.target("30").load().migrate().migrationsExecuted);

        JdbcTemplate legado = new JdbcTemplate(
                new org.springframework.jdbc.datasource.DriverManagerDataSource(
                        url, POSTGRES.getUsername(), POSTGRES.getPassword()));
        inserir(legado, "valido", "Sa\u0303o Paulo", "rj", 3, 0, true);
        inserir(legado, "incompleto", "São Paulo", "SP", 3, 0, false);
        inserir(legado, "cidade", null, "SP", 3, 0, true);
        inserir(legado, "estado", "São Paulo", null, 3, 0, true);
        inserir(legado, "vazia", "  ", "SP", 3, 0, true);
        inserir(legado, "formato", "Cidade 1", "SP", 3, 0, true);
        inserir(legado, "uf", "São Paulo", "ZZ", 3, 0, true);
        inserir(legado, "tempo", "São Paulo", "SP", 0, 0, true);
        inserir(legado, "orcamento", "São Paulo", "SP", 3, -1, true);
        var dados = dadosSemFlag(legado);
        var flagsIniciais = flags(legado);
        var migration = new V31__reabrir_questionarios_inconsistentes();

        try (var connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            connection.setAutoCommit(false);
            Context contexto = mock(Context.class);
            when(contexto.getConnection()).thenReturn(connection);
            migration.migrate(contexto);
            try (var consulta = connection.createStatement();
                 var resultado = consulta.executeQuery("SELECT count(*) FROM recommendation_profile WHERE questionario_concluido")) {
                assertTrue(resultado.next());
                assertEquals(1, resultado.getInt(1));
            }
            connection.rollback();
        }
        assertEquals(flagsIniciais, flags(legado));

        Flyway upgrade = config.target("31").load();
        assertEquals(1, upgrade.migrate().migrationsExecuted);
        upgrade.validate();
        assertEquals(0, upgrade.migrate().migrationsExecuted);
        assertEquals(dados, dadosSemFlag(legado));
        assertEquals(List.of(true, false, false, false, false, false, false, false, false), flags(legado));

        try (var connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            Context contexto = mock(Context.class);
            when(contexto.getConnection()).thenReturn(connection);
            migration.migrate(contexto);
        }
        assertEquals(dados, dadosSemFlag(legado));
        assertEquals(List.of(true, false, false, false, false, false, false, false, false), flags(legado));
    }

    private static void inserir(JdbcTemplate banco, String nome, String cidade, String estado,
                                double tempo, double orcamento, boolean concluido) {
        Long id = banco.queryForObject(INSERT_USER, Long.class, nome + "@example.test");
        banco.update("""
                INSERT INTO recommendation_profile (user_id, cidade, estado, tempo_disponivel_semanal,
                    orcamento_inicial, tipo_socializacao, nivel_atividade_fisica_desejada, ambiente_preferido,
                    questionario_concluido, updated_at)
                VALUES (?, ?, ?, ?, ?, 'INDIVIDUAL', 'BAIXO', 'CASA', ?, CURRENT_TIMESTAMP)
                """, id, cidade, estado, tempo, orcamento, concluido);
    }

    private static List<Map<String, Object>> dadosSemFlag(JdbcTemplate banco) {
        return banco.queryForList("""
                SELECT to_jsonb(p) - 'questionario_concluido' AS dados
                FROM recommendation_profile p ORDER BY id
                """);
    }

    private static List<Boolean> flags(JdbcTemplate banco) {
        return banco.queryForList("""
                SELECT p.questionario_concluido FROM recommendation_profile p
                ORDER BY p.id
                """, Boolean.class);
    }
}
