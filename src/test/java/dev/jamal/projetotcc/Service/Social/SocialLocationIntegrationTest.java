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
@SpringBootTest(classes = SocialLocationIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.name=social-location-integration-isolated",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.sql.init.mode=never"
        })
class SocialLocationIntegrationTest {
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
            SocialAffinityService.class, SocialProfileResumeService.class})
    static class Config {}

    @Autowired SocialHubService hub;
    @Autowired SocialLocationService localizacao;
    @Autowired CommunityService comunidades;
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
    void geralPreservaPerfisPrivadosSemExporIndicador() {
        Long atual = usuario("MG", "São João", true, true);
        Long mesmaCidade = usuario("mg", " sao   joao ", true, true);
        Long mesmoEstado = usuario("MG", "Outra cidade", true, true);
        Long privado = usuario("MG", "São João", true, false);
        Long outraUf = usuario("SP", "São João", true, true);
        Long desativado = usuario("MG", "São João", false, true);

        var lista = hub.buscarUsuarios(atual, SocialLocationScope.GERAL);
        assertEquals(4, lista.size());
        assertFalse(lista.stream().anyMatch(p -> p.userId().equals(atual) || p.userId().equals(desativado)));
        assertEquals("MESMA_CIDADE", lista.stream().filter(p -> p.userId().equals(mesmaCidade)).findFirst().orElseThrow().proximidade());
        assertEquals("MESMO_ESTADO", lista.stream().filter(p -> p.userId().equals(mesmoEstado)).findFirst().orElseThrow().proximidade());
        assertNull(lista.stream().filter(p -> p.userId().equals(privado)).findFirst().orElseThrow().proximidade());
        assertNull(lista.stream().filter(p -> p.userId().equals(outraUf)).findFirst().orElseThrow().proximidade());
    }

    @Test
    void filtrosConsideramConsentimentoCidadeNormalizadaEUf() {
        Long atual = usuario("MG", "São João", true, false);
        Long cidade = usuario("MG", "Sao Joao", true, true);
        Long estado = usuario("MG", "Outra cidade", true, true);
        usuario("SP", "São João", true, true);
        usuario("MG", "São João", true, false);

        assertEquals(java.util.Set.of(cidade, estado), hub.buscarUsuarios(atual, SocialLocationScope.ESTADUAL)
                .stream().map(p -> p.userId()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.List.of(cidade), hub.buscarUsuarios(atual, SocialLocationScope.MUNICIPAL)
                .stream().map(p -> p.userId()).toList());
    }

    @Test
    void localizacaoAusenteNaoImpedeListaGeral() {
        Long atual = usuario(null, null, true, false);
        usuario("MG", "São João", true, true);
        assertEquals(1, hub.buscarUsuarios(atual, SocialLocationScope.GERAL).size());
        assertThrows(IllegalArgumentException.class, () -> hub.buscarUsuarios(atual, SocialLocationScope.ESTADUAL));
        assertThrows(IllegalArgumentException.class, () -> comunidades.listarMunicipais(atual));
        assertFalse(localizacao.configuracao(atual).cidadeDisponivel());
    }

    @Test
    void autorizacaoDeRegiaoPodeSerAtivadaERetirada() {
        Long atual = usuario("MG", "São João", true, false);
        Long outro = usuario("MG", "São João", true, false);
        assertFalse(localizacao.configuracao(outro).compartilharRegiao());
        assertTrue(hub.buscarUsuarios(atual, SocialLocationScope.MUNICIPAL).isEmpty());
        localizacao.atualizarVisibilidade(outro, true);
        assertEquals(1, hub.buscarUsuarios(atual, SocialLocationScope.MUNICIPAL).size());
        localizacao.atualizarVisibilidade(outro, false);
        assertTrue(hub.buscarUsuarios(atual, SocialLocationScope.MUNICIPAL).isEmpty());
    }

    @Test
    void comunidadesMunicipaisNaoDuplicamENaoMisturamUfs() {
        Long primeiro = usuario("MG", "São João", true, false);
        Long segundo = usuario("mg", " sao joao ", true, false);
        Long outroEstado = usuario("SP", "São João", true, false);
        var inicial = comunidades.listarMunicipais(primeiro).get(0);
        assertEquals("MUNICIPAL", inicial.tipo());
        assertEquals("SAO JOAO", inicial.cidade());
        assertEquals(inicial.id(), comunidades.listarMunicipais(segundo).get(0).id());
        assertEquals(inicial.id(), comunidades.listarMunicipais(primeiro).get(0).id());
        assertNotEquals(inicial.id(), comunidades.listarMunicipais(outroEstado).get(0).id());
        assertEquals(2L, jdbc.queryForObject("SELECT count(*) FROM communities WHERE tipo='MUNICIPAL'", Long.class));
    }

    @Test
    void entradaMunicipalValidaCidadeNoServidor() {
        Long morador = usuario("MG", "São João", true, false);
        Long externo = usuario("MG", "Outra cidade", true, true);
        Long comunidadeId = comunidades.listarMunicipais(morador).get(0).id();
        assertThrows(IllegalStateException.class, () -> comunidades.entrar(comunidadeId, externo));
        assertTrue(comunidades.entrar(comunidadeId, morador).participando());
        assertEquals("SAO JOAO", comunidades.buscarDetalhes(comunidadeId, morador).cidade());
        assertFalse(comunidades.sair(comunidadeId, morador).participando());
    }

    @Test
    void comunidadesAnterioresContinuamDisponiveis() {
        Long atual = usuario("MG", "São João", true, false);
        comunidades.garantirComunidadesGerais();
        comunidades.garantirComunidadesRegionais(atual);
        assertEquals("GERAL", comunidades.listarGerais(atual).get(0).tipo());
        assertEquals("REGIONAL", comunidades.listarRegionais(atual).get(0).tipo());
        assertNull(comunidades.listarRegionais(atual).get(0).cidade());
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
