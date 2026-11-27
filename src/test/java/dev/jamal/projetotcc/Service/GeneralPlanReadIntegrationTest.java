package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.AI.AIGeneralPlanContext;
import dev.jamal.projetotcc.DTO.AI.AIGeneralPlanContext.HobbySummaryContext;
import dev.jamal.projetotcc.Enum.RecommendationFeedbackType;
import dev.jamal.projetotcc.Repository.UserHobbyRepository;
import dev.jamal.projetotcc.Repository.UserRecommendationFeedbackRepository;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.AI.AIGeneralPlanContextHashService;
import dev.jamal.projetotcc.Service.AI.AIGeneralPlanContextService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class GeneralPlanReadIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("general_plan_read").withUsername("integration").withPassword("integration");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired TransactionTemplate transactions;
    @Autowired AIGeneralPlanContextService contextService;
    @Autowired AIGeneralPlanContextHashService hashService;
    @Autowired RecommendationService recommendationService;
    @Autowired UserRecommendationFeedbackRepository feedbacks;
    @Autowired UserHobbyRepository userHobbies;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired WebApplicationContext web;

    @Test
    void contextMatchesIndividualScoresAndAnExistingHashRemainsValid() throws Exception {
        long user = createUser("owner-plan@example.test");
        long other = createUser("other-plan@example.test");
        jdbc.update("""
                INSERT INTO recommendation_profile(user_id, ambiente_preferido, nivel_atividade_fisica_desejada,
                    orcamento_inicial, questionario_concluido, tempo_disponivel_semanal, tipo_socializacao,
                    cidade, estado, updated_at)
                VALUES (?, 'CASA', 'BAIXO', 100, true, 4, 'INDIVIDUAL', 'São Paulo', 'SP', CURRENT_TIMESTAMP)
                """, user);
        long category = jdbc.queryForObject("INSERT INTO hobby_category(nome) VALUES ('Categoria plano') RETURNING id", Long.class);
        long interest = jdbc.queryForObject("INSERT INTO interest(nome) VALUES ('Interesse plano') RETURNING id", Long.class);
        long objective = jdbc.queryForObject("INSERT INTO objectives(nome) VALUES ('Objetivo plano') RETURNING id", Long.class);
        List<Long> hobbies = jdbc.queryForList("""
                INSERT INTO hobby(nome, ambiente, nivel_atividade_fisica, tipo_socializacao, requer_equipamento,
                    category_id, custo_estimado, tempo_necessario)
                SELECT 'Hobby plano ' || g, 'CASA', 'BAIXO', 'INDIVIDUAL', false, ?, g * 10, 2
                FROM generate_series(1, 14) g RETURNING id
                """, Long.class, category);
        jdbc.update("INSERT INTO user_interest(peso, interest_id, user_id) VALUES(3, ?, ?)", interest, user);
        jdbc.update("INSERT INTO user_objectives(peso, objective_id, user_id) VALUES(2, ?, ?)", objective, user);
        for (Long hobby : hobbies) {
            jdbc.update("INSERT INTO hobby_interests(peso, hobby_id, interest_id) VALUES(2, ?, ?)", hobby, interest);
            jdbc.update("INSERT INTO hobby_objectives(peso, hobby_id, objective_id) VALUES(3, ?, ?)", hobby, objective);
        }
        for (Long hobby : hobbies.subList(0, 2)) addInterest(user, hobby);

        var stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean previousStats = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        try {
            stats.clear();
            var pequeno = transactions.execute(transaction -> contextService.construir(user));
            long sqlPequeno = stats.getPrepareStatementCount();
            assertEquals(2, pequeno.hobbies().size());
            assertTrue(sqlPequeno > 0);
            for (Long hobby : hobbies.subList(2, hobbies.size())) addInterest(user, hobby);
            jdbc.update("""
                    INSERT INTO user_hobbies(atualizado_em, criado_em, nivel_atual, status_atual, hobby_id, user_id)
                    VALUES(CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'AVANCADO', 'PRATICANDO', ?, ?)
                    """, hobbies.get(hobbies.size() - 1), user);
            jdbc.update("INSERT INTO user_hobby_feedback(rating, hobby_id, user_id) VALUES(4, ?, ?)", hobbies.get(0), user);

            stats.clear();
            var atual = transactions.execute(transaction -> contextService.construir(user));
            long sqlAmpliado = stats.getPrepareStatementCount();
            assertTrue(sqlAmpliado <= sqlPequeno + 1, () -> sqlPequeno + " → " + sqlAmpliado);
            assertEquals(10, atual.hobbies().size());
            assertEquals(hobbies.get(hobbies.size() - 1), atual.hobbies().get(0).hobbyId());

            var anterior = contextoComPontuacaoIndividual(user, atual.usuario().idade());
            assertEquals(anterior, atual);
            String hashAnterior = hashService.calcular(anterior);
            assertEquals(hashAnterior, hashService.calcular(atual));
            var mvc = MockMvcBuilders.webAppContextSetup(web).apply(springSecurity()).build();
            String route = "/api/v1/ai/general-plan";
            mvc.perform(get(route).header("Authorization", bearer(user))).andExpect(status().isNotFound());
            jdbc.update("""
                    INSERT INTO general_personalized_plan(conteudo, context_hash, data_atualizacao, data_criacao, user_id)
                    VALUES('Plano anterior', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                    """, hashAnterior, user);
            mvc.perform(get(route)).andExpect(status().isUnauthorized());
            mvc.perform(get(route).header("Authorization", bearer(other))).andExpect(status().isNotFound());
            mvc.perform(get(route).header("Authorization", bearer(user))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo").value("Plano anterior"))
                    .andExpect(jsonPath("$.stale").value(false));
            jdbc.update("UPDATE recommendation_profile SET orcamento_inicial = 200 WHERE user_id = ?", user);
            mvc.perform(get(route).header("Authorization", bearer(user))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo").value("Plano anterior"))
                    .andExpect(jsonPath("$.stale").value(true));
            assertEquals(hashAnterior, jdbc.queryForObject(
                    "SELECT context_hash FROM general_personalized_plan WHERE user_id=?", String.class, user));
        } finally {
            stats.setStatisticsEnabled(previousStats);
        }
    }

    private AIGeneralPlanContext contextoComPontuacaoIndividual(long user, int idade) {
        return transactions.execute(transaction -> {
            var hobbies = feedbacks.findByUser_IdAndTipo(user, RecommendationFeedbackType.INTERESSADO)
                    .stream().map(feedback -> {
                        var hobby = feedback.getHobby();
                        var vinculo = userHobbies.findByUser_IdAndHobby_Id(user, hobby.getId()).orElse(null);
                        var score = recommendationService.calcularRecomendacao(user, hobby.getId()).getScore();
                        return new HobbySummaryContext(hobby.getId(), hobby.getNome(), hobby.getCategory().getNome(),
                                hobby.getTempoNecessario(), hobby.getNivelAtividadeFisica().name(),
                                hobby.getAmbiente().name(), score, vinculo == null ? null : vinculo.getNivelAtual().name(),
                                vinculo == null ? null : vinculo.getStatusAtual().name());
                    }).sorted(Comparator.comparingInt((HobbySummaryContext h) -> "PRATICANDO".equals(h.statusAtual()) ? 0 : 3)
                            .thenComparing(HobbySummaryContext::score, Comparator.reverseOrder()))
                    .limit(10).toList();
            return new AIGeneralPlanContext(new AIGeneralPlanContext.UserContext(idade, "São Paulo", "SP"),
                    new AIGeneralPlanContext.ProfileContext(4.0, 100.0, "INDIVIDUAL", "BAIXO", "CASA",
                            List.of("Interesse plano"), List.of("Objetivo plano")), hobbies);
        });
    }

    private long createUser(String email) {
        return jdbc.queryForObject("""
                INSERT INTO users(nome, email, senha, data_nascimento)
                VALUES ('Pessoa teste', ?, 'hash-ficticio', DATE '1990-01-01') RETURNING id
                """, Long.class, email);
    }

    private String bearer(long user) {
        return "Bearer " + jwt.gerarToken(users.findById(user).orElseThrow());
    }

    private void addInterest(long user, long hobby) {
        jdbc.update("INSERT INTO user_recommendation_feedbacks(tipo, hobby_id, user_id) VALUES('INTERESSADO', ?, ?)",
                hobby, user);
    }
}

