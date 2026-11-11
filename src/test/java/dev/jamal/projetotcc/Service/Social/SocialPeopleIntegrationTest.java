package dev.jamal.projetotcc.Service.Social;

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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = SocialPeopleIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.name=social-location-integration-isolated",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.sql.init.mode=never"
        })
class SocialPeopleIntegrationTest {
    @Container
    static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES =
            new org.testcontainers.postgresql.PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("social_location_integration")
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
    @Import({SocialLocationService.class, SocialHubService.class, CommunityService.class,
            SocialAffinityService.class, SocialProfileResumeService.class, SocialPeopleService.class})
    static class Config {}

    @Autowired SocialHubService hub;
    @Autowired SocialPeopleService people;
    @Autowired SocialProfileRepository sociais;
    @Autowired RecommendationProfileRepository perfis;
    @Autowired UserRepository usuarios;
    @Autowired HobbyRepository hobbies;
    @Autowired HobbyCategoryRepository categorias;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepararBancoDescartavel() {
        jdbc.execute("TRUNCATE users, hobby, hobby_category CASCADE");
        var categoria = new HobbyCategory();
        categoria.setNome("Teste social");
        categorias.saveAndFlush(categoria);
        var hobby = new Hobby();
        hobby.setNome("Hobby social");
        hobby.setCategory(categoria);
        hobby.setTipoSocializacao(TipoSocializacao.SOCIAL);
        hobby.setNivelAtividadeFisica(NivelAtividadeFisica.BAIXO);
        hobby.setAmbiente(AmbientePreferido.CASA);
        hobbies.saveAndFlush(hobby);
    }

    @Test
    void paginaFiltraAntesDeCortarEOrdenaEmpatesPorId() {
        var actor = usuario("MG", "São João", true, false);
        var blocked = usuario("MG", "São João", true, true);
        var hiddenRegion = usuario("MG", "São João", true, false);
        var first = usuario("mg", "sao   joao", true, true);
        var second = usuario("MG", "São João", true, true);
        var inactive = usuario("MG", "São João", false, true);
        var minor = usuario("MG", "São João", true, true);
        jdbc.update("UPDATE users SET data_nascimento = ? WHERE id = ?", LocalDate.now().minusYears(17), minor);
        jdbc.update("INSERT INTO social_user_blocks(blocker_id, blocked_id, created_at) VALUES (?, ?, now())", blocked, actor);
        jdbc.update("INSERT INTO social_follows(follower_id, followed_id) VALUES (?, ?)", actor, first);

        var page = people.listar(actor, SocialLocationScope.MUNICIPAL, 0, 1);
        assertEquals(first, page.pessoas().get(0).perfil().userId());
        assertEquals("MESMA_CIDADE", page.pessoas().get(0).perfil().proximidade());
        assertTrue(page.pessoas().get(0).seguindo());
        assertTrue(page.temMais());
        var next = people.listar(actor, SocialLocationScope.MUNICIPAL, 1, 1);
        assertEquals(second, next.pessoas().get(0).perfil().userId());
        assertFalse(next.pessoas().get(0).seguindo());
        assertFalse(next.temMais());
        assertEquals(java.util.Set.of(hiddenRegion, first, second), people.listar(actor, SocialLocationScope.GERAL, 0, 20)
                .pessoas().stream().map(c -> c.perfil().userId()).collect(java.util.stream.Collectors.toSet()));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> people.listar(inactive, SocialLocationScope.GERAL, 0, 20));
    }

    @Test
    void preservaPrivacidadeEAfinidadeNasDuasDirecoes() {
        var actor = usuario("MG", "São João", true, true);
        var other = usuario("MG", "São João", true, true);
        var interest = jdbc.queryForObject("INSERT INTO interest(nome) VALUES (?) RETURNING id", Long.class, "Interesse comum");
        for (var id : java.util.List.of(actor, other)) {
            jdbc.update("INSERT INTO user_interest(user_id,interest_id,peso) VALUES (?, ?, 3)", id, interest);
        }
        jdbc.update("""
                UPDATE social_profiles SET biografia = 'Visível para contatos',
                  foto = decode('00', 'hex'), biografia_visibilidade = 'CONTATOS',
                  foto_visibilidade = 'CONTATOS', interesses_visibilidade = 'CONTATOS'
                WHERE user_id = ?
                """, other);
        jdbc.update("UPDATE social_profiles SET interesses_visibilidade = 'SOMENTE_EU' WHERE user_id = ?", actor);
        jdbc.update("INSERT INTO social_follows(follower_id, followed_id) VALUES (?, ?)", actor, other);
        var pending = people.listar(actor, SocialLocationScope.GERAL, 0, 20).pessoas().get(0);
        assertTrue(pending.seguindo());
        assertEquals(hub.buscarPerfilPublicoPara(actor, other), pending.perfil());
        assertNull(pending.perfil().biografia());
        assertFalse(pending.perfil().possuiFoto());
        jdbc.update("""
                INSERT INTO social_contacts(low_user_id, high_user_id, requester_id, recipient_id, status, requested_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACEITA', now(), now())
                """, actor, other, actor, other);
        var accepted = people.listar(actor, SocialLocationScope.GERAL, 0, 20).pessoas().get(0).perfil();
        assertEquals(hub.buscarPerfilPublicoPara(actor, other), accepted);
        assertTrue(accepted.possuiFoto());
        assertEquals(java.util.List.of("Interesse comum"), accepted.interesses());
        assertEquals(0, accepted.afinidade().score());
    }

    @Test
    void ordenaPelaAfinidadeComHobbiesVisiveisERespeitaStatusDePratica() {
        var actor = usuario("MG", "São João", true, true);
        var lower = usuario("MG", "São João", true, true);
        var higher = usuario("MG", "São João", true, true);
        var practiced = hobbies.findAll().get(0);
        var interested = new Hobby();
        interested.setNome("Outro hobby");
        interested.setCategory(categorias.findAll().get(0));
        interested.setTipoSocializacao(TipoSocializacao.SOCIAL);
        interested.setNivelAtividadeFisica(NivelAtividadeFisica.BAIXO);
        interested.setAmbiente(AmbientePreferido.CASA);
        hobbies.saveAndFlush(interested);

        for (Long id : java.util.List.of(actor, higher)) {
            jdbc.update("""
                    INSERT INTO user_hobbies(user_id,hobby_id,status_atual,nivel_atual,criado_em,atualizado_em)
                    VALUES (?, ?, 'PRATICANDO', 'INICIANTE', now(), now())
                    """, id, practiced.getId());
            for (Long hobbyId : java.util.List.of(practiced.getId(), interested.getId())) {
                jdbc.update("""
                        INSERT INTO user_recommendation_feedbacks(user_id,hobby_id,tipo,created_at)
                        VALUES (?, ?, 'INTERESSADO', now())
                        """, id, hobbyId);
            }
        }
        var best = people.listar(actor, SocialLocationScope.GERAL, 0, 1).pessoas().get(0).perfil();
        assertEquals(higher, best.userId());
        assertEquals(5, best.afinidade().score());
        assertEquals(java.util.List.of(interested.getId()), best.interessados().stream()
                .map(dev.jamal.projetotcc.DTO.Social.SocialHobbyDTO::hobbyId).toList());
        assertEquals(hub.buscarPerfilPublicoPara(actor, higher), best);
        jdbc.update("UPDATE social_profiles SET hobbies_visibilidade = 'SOMENTE_EU' WHERE user_id = ?", higher);
        assertEquals(lower, people.listar(actor, SocialLocationScope.GERAL, 0, 1).pessoas().get(0).perfil().userId());
    }

    private Long usuario(String uf, String cidade, boolean participa, boolean compartilhar) {
        var user = new User();
        user.setNome("Participante");
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setSenha("senha-exclusiva-do-teste");
        user.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuarios.saveAndFlush(user);

        var profile = new RecommendationProfile();
        profile.setUser(user);
        profile.setEstado(uf);
        profile.setCidade(cidade);
        profile.setTempoDisponivelSemanal(5.0);
        profile.setOrcamentoInicial(100.0);
        profile.setTipoSocializacao(TipoSocializacao.SOCIAL);
        profile.setNivelAtividadeFisicaDesejada(NivelAtividadeFisica.BAIXO);
        profile.setAmbientePreferido(AmbientePreferido.CASA);
        perfis.saveAndFlush(profile);

        var social = new SocialProfile();
        social.setUser(user);
        social.setEnabled(participa);
        social.setCompartilharRegiao(compartilhar);
        sociais.saveAndFlush(social);
        return user.getId();
    }
}

