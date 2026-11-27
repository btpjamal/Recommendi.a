package dev.jamal.projetotcc.security;

import dev.jamal.projetotcc.Controllers.AuthController;
import dev.jamal.projetotcc.Controllers.UserController;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Exception.GlobalExceptionHandler;
import dev.jamal.projetotcc.Mapper.UserMapper;
import dev.jamal.projetotcc.Service.AuthService;
import dev.jamal.projetotcc.Service.CanonicalEmail;
import dev.jamal.projetotcc.Service.JwtAuthenticationFilter;
import dev.jamal.projetotcc.Service.JwtService;
import dev.jamal.projetotcc.Service.LoginRateLimitService;
import dev.jamal.projetotcc.Service.PasswordPolicyService;
import dev.jamal.projetotcc.Service.UserDetailsServiceImpl;
import dev.jamal.projetotcc.Service.UserService;
import dev.jamal.projetotcc.config.PublicAuthRateLimitFilter;
import dev.jamal.projetotcc.config.SecurityConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = CanonicalEmailIntegrationTest.Config.class, properties = {
        "spring.config.name=canonical-email-isolated",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.baseline-on-migrate=false"
})
class CanonicalEmailIntegrationTest {
    private static final String PASSWORD = "Ficticia!Com-Entropia-2026";
    private static final String SECRET = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=";
    private static final String INSERT_USER = """
            INSERT INTO users (nome, email, senha, data_nascimento)
            VALUES ('Pessoa de teste', ?, 'hash-ficticio', DATE '1990-01-01')
            """;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("canonical_email").withUsername("integration").withPassword("integration");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("security.jwt.secret", () -> SECRET);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("dev.jamal.projetotcc.Entities")
    @EnableJpaRepositories("dev.jamal.projetotcc.Repository")
    @Import({AuthController.class, UserController.class, GlobalExceptionHandler.class, SecurityConfig.class,
            PublicAuthRateLimitFilter.class, JwtAuthenticationFilter.class, JwtService.class, AuthService.class,
            UserService.class, UserDetailsServiceImpl.class, UserMapper.class, LoginRateLimitService.class,
            PasswordPolicyService.class})
    static class Config {}

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired JwtService jwt;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void cleanInstallUsesTheSameCanonicalRuleAndChecksDirectWrites() {
        flyway.validate();
        assertEquals("31", flyway.info().current().getVersion().getVersion());
        assertEquals(0, flyway.migrate().migrationsExecuted);
        for (String input : List.of("", "   ", " I@EXAMPLE.TEST ", "\tA@EXAMPLE.TEST\n",
                "\u00a0A@example.test\u2003", "İẞＡÉe\u0301@EXAMPLE.test", "A B@EXAMPLE.test")) {
            String sql = jdbc.queryForObject("""
                    SELECT translate(btrim(?::text COLLATE "C", ' '),
                        'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')
                    """, String.class, input);
            assertEquals(CanonicalEmail.normalize(input), sql);
        }
        jdbc.update(INSERT_USER, "direct@example.test");
        jdbc.update(INSERT_USER, "é@example.test");
        jdbc.update(INSERT_USER, "e\u0301@example.test");
        jdbc.update(INSERT_USER, "É@example.test");
        assertSqlState("23514", () -> jdbc.update(INSERT_USER, " Direct@example.test "));
        assertSqlState("23514", () -> jdbc.update("UPDATE users SET email = 'DIRECT@example.test' "
                + "WHERE email = 'direct@example.test'"));
        assertSqlState("23505", () -> jdbc.update(INSERT_USER, "direct@example.test"));
    }

    @Test
    void concurrentRegistrationAndLoginUseCanonicalIdentity() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try (var lock = connect(POSTGRES.getJdbcUrl()); var sql = lock.createStatement()) {
            lock.setAutoCommit(false);
            sql.execute("LOCK TABLE users IN SHARE MODE");
            var a = pool.submit(() -> registerAtStart(start, " Race@example.TEST "));
            var b = pool.submit(() -> registerAtStart(start, "rACE@example.test"));
            start.countDown();
            // Both existence checks must finish before either INSERT can acquire its lock.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            int blockedWriters = 0;
            while (blockedWriters < 2 && System.nanoTime() < deadline) {
                try (var rows = sql.executeQuery("""
                        SELECT count(*) FROM pg_locks
                        WHERE relation = 'users'::regclass AND database =
                            (SELECT oid FROM pg_database WHERE datname = current_database())
                          AND mode = 'RowExclusiveLock' AND NOT granted
                        """)) {
                    rows.next();
                    blockedWriters = rows.getInt(1);
                }
                if (blockedWriters < 2) {
                    Thread.sleep(20);
                }
            }
            assertEquals(2, blockedWriters);
            lock.commit();
            var responses = List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(List.of(201, 400), responses.stream().map(r -> r.getStatus()).sorted().toList());
            assertTrue(responses.stream().anyMatch(r -> r.getStatus() == 400
                    && new String(r.getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8)
                            .contains("Email já cadastrado")));
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'race@example.test'", Integer.class));
        mvc.perform(post("/api/v1/users").contentType("application/json").content(registration("RACE@example.test")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/users").contentType("application/json").content(registration(" Second@EXAMPLE.TEST ")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("second@example.test"));
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(registration("\\tbad@example.test")))
                .andExpect(status().isBadRequest());
        String login = mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("""
                {"email":" RACE@Example.test ","senha":"%s"}
                """.formatted(PASSWORD))).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("race@example.test")).andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(login, "$.token");
        long id = jwt.extrairUserId(token);
        long other = jdbc.queryForObject("SELECT id FROM users WHERE email = 'second@example.test'", Long.class);
        mvc.perform(get("/api/v1/users/{id}", other).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/users/{id}", id).header("Authorization", "Bearer " + token)
                .contentType("application/json").content(registration(" SECOND@example.test ")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/users/{id}", id).header("Authorization", "Bearer " + token)
                .contentType("application/json").content(registration(" New@EXAMPLE.test ")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new@example.test"));
        mvc.perform(get("/api/v1/users/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upgradePreservesLinksAndAcceptsOnlyTheExistingIdBasedToken() throws Exception {
        String url = createDatabase("canonical_upgrade");
        assertEquals(29, atVersion(url, "29").migrate().migrationsExecuted);
        long id;
        try (var conn = connect(url); var sql = conn.createStatement()) {
            sql.executeUpdate("""
                    INSERT INTO users (nome, email, senha, data_nascimento, token_version)
                    VALUES ('Titular', ' Mixed@EXAMPLE.test ', 'original-hash', '1990-01-01', 7),
                           ('Outra pessoa', 'Other@EXAMPLE.test', 'another-hash', '1991-01-01', 2)
                    """);
            try (var rows = sql.executeQuery("SELECT id FROM users WHERE nome = 'Titular'")) {
                assertTrue(rows.next());
                id = rows.getLong(1);
            }
            sql.executeUpdate("INSERT INTO social_profiles (created_at, enabled, updated_at, user_id) "
                    + "VALUES (now(), true, now(), " + id + ")");
        }
        User before = user(id, " Mixed@EXAMPLE.test ", 7);
        String token = jwt.gerarToken(before);
        Instant now = Instant.now();
        String legacy = Jwts.builder().issuer("recommendia").subject(before.getEmail())
                .claim("userId", id).claim("tokenVersion", 7)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET))).compact();

        var upgraded = atVersion(url, "30");
        assertEquals(1, upgraded.migrate().migrationsExecuted);
        upgraded.validate();
        assertEquals(0, atVersion(url, "30").migrate().migrationsExecuted);
        try (var conn = connect(url); var sql = conn.createStatement(); var rows = sql.executeQuery("""
                SELECT u.id, u.email, u.senha, u.token_version, u.data_nascimento, p.user_id
                FROM users u JOIN social_profiles p ON p.user_id = u.id
                """)) {
            assertTrue(rows.next());
            assertEquals(id, rows.getLong("id"));
            assertEquals(id, rows.getLong("user_id"));
            assertEquals("original-hash", rows.getString("senha"));
            assertEquals("1990-01-01", rows.getString("data_nascimento"));
            assertEquals("mixed@example.test", rows.getString("email"));
            var after = user(rows.getLong("id"), rows.getString("email"), rows.getLong("token_version"));
            assertEquals(7, after.getTokenVersion());
            assertTrue(jwt.tokenValido(token, after));
            assertFalse(jwt.tokenValido(token, user(id + 1, after.getEmail(), 7)));
            assertThrows(IllegalArgumentException.class, () -> jwt.extrairUserId(legacy));
            assertFalse(rows.next());
        }
    }

    @Test
    void collisionRollsBackDataAndSchema() throws Exception {
        String url = createDatabase("canonical_collision");
        atVersion(url, "29").migrate();
        try (var conn = connect(url); var sql = conn.createStatement()) {
            sql.executeUpdate("""
                    INSERT INTO users (nome, email, senha, data_nascimento)
                    VALUES ('Um', 'CaseProbe@example.test', 'hash1', '1990-01-01'),
                           ('Dois', 'caseprobe@example.test', 'hash2', '1990-01-01'),
                           ('Três', ' Other@EXAMPLE.test ', 'hash3', '1990-01-01')
                    """);
        }
        var error = assertThrows(FlywayException.class, () -> atVersion(url, "30").migrate());
        assertTrue(error.getMessage().contains("canonical email collisions"));
        try (var conn = connect(url); var sql = conn.createStatement();
                var rows = sql.executeQuery("SELECT email, senha FROM users ORDER BY id")) {
            for (var expected : List.of("CaseProbe@example.test", "caseprobe@example.test", " Other@EXAMPLE.test ")) {
                assertTrue(rows.next());
                assertEquals(expected, rows.getString("email"));
                assertEquals("hash" + rows.getRow(), rows.getString("senha"));
            }
            assertFalse(rows.next());
        }
        try (var conn = connect(url); var sql = conn.createStatement(); var rows = sql.executeQuery("""
                SELECT
                  (SELECT count(*) FROM flyway_schema_history WHERE version = '30'),
                  (SELECT count(*) FROM pg_constraint WHERE conrelid = 'users'::regclass
                      AND conname IN ('users_email_canonical_key', 'users_email_canonical_check')),
                  (SELECT count(*) FROM pg_constraint WHERE conrelid = 'users'::regclass
                      AND conname = 'uk6dotkott2kjsp8vw4d0m25fb7')
                """)) {
            assertTrue(rows.next());
            assertEquals(0, rows.getInt(1));
            assertEquals(0, rows.getInt(2));
            assertEquals(1, rows.getInt(3));
        }
    }

    private org.springframework.mock.web.MockHttpServletResponse registerAtStart(CountDownLatch start, String email)
            throws Exception {
        assertTrue(start.await(5, TimeUnit.SECONDS));
        return mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(registration(email)))
                .andReturn().getResponse();
    }

    private static String registration(String email) {
        return """
                {"nome":"Pessoa fictícia","email":"%s","senha":"%s","dataNascimento":"1990-01-01"}
                """.formatted(email, PASSWORD);
    }

    private static void assertSqlState(String expected, Runnable operation) {
        var error = assertThrows(org.springframework.dao.DataIntegrityViolationException.class, operation::run);
        assertEquals(expected, assertInstanceOf(SQLException.class, error.getMostSpecificCause()).getSQLState());
    }

    private static String createDatabase(String database) throws SQLException {
        try (var conn = connect(POSTGRES.getJdbcUrl()); var sql = conn.createStatement()) {
            sql.executeUpdate("CREATE DATABASE " + database);
        }
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/" + database;
    }

    private static java.sql.Connection connect(String url) throws SQLException {
        return DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Flyway atVersion(String url, String version) {
        return Flyway.configure().dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword()).target(version)
                .load();
    }

    private static User user(long id, String email, long version) {
        var user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setTokenVersion(version);
        return user;
    }
}
