package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.SocialProfileEditDTO;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Enum.SocialVisibility;
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
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes=SocialProfileIntegrationTest.Config.class, webEnvironment=SpringBootTest.WebEnvironment.NONE,
 properties={"spring.config.name=social-profile-isolated", "spring.jpa.hibernate.ddl-auto=validate",
 "spring.flyway.enabled=true", "spring.flyway.baseline-on-migrate=false", "spring.sql.init.mode=never"})
class SocialProfileIntegrationTest {
    @Container static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES =
        new org.testcontainers.postgresql.PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("social_profile_test").withUsername("integration").withPassword("integration");
    @DynamicPropertySource static void banco(DynamicPropertyRegistry r) {
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
    Long id;
    @BeforeEach void setup() {
        jdbc.execute("TRUNCATE users CASCADE");
        var u=new User(); u.setNome("Pessoa de teste"); u.setEmail(UUID.randomUUID()+"@example.test");
        u.setSenha("test-only"); u.setDataNascimento(LocalDate.of(1990,1,1));
        id=users.saveAndFlush(u).getId(); participation.ativar(id);
    }
    @Test void migrationPersisteBioVisibilidadeEFotoEPermiteRemocao() {
        var settings=new SocialProfileEditDTO("Minha biografia",SocialVisibility.CONTATOS,
            SocialVisibility.SOMENTE_EU,SocialVisibility.PARTICIPANTES,SocialVisibility.CONTATOS);
        assertEquals(settings,editor.salvar(id,settings));
        byte[] png={1,2,3}; editor.salvarFoto(id,png);
        assertArrayEquals(png,editor.foto(id,id)); assertTrue(hub.buscarPerfilPublicoPara(id,id).possuiFoto());
        editor.removerFoto(id); assertFalse(hub.buscarPerfilPublicoPara(id,id).possuiFoto());
        assertThrows(ResponseStatusException.class,()->editor.foto(id,id));
    }
    @Test void desativacaoImpedeAlteracaoDaBiografiaEDaFoto() {
        var settings=editor.buscar(id); participation.desativar(id);
        assertThrows(ResponseStatusException.class,()->editor.salvar(id,settings));
        assertThrows(ResponseStatusException.class,()->editor.salvarFoto(id,new byte[]{1}));
    }
    @Test void defaultsPreservamVisibilidadeAnteriorEReativacaoPreservaBio() {
        assertEquals(SocialVisibility.PARTICIPANTES,editor.buscar(id).hobbiesVisibilidade());
        var p=new SocialProfileEditDTO("Bio preservada",SocialVisibility.SOMENTE_EU,
            SocialVisibility.CONTATOS,SocialVisibility.SOMENTE_EU,SocialVisibility.SOMENTE_EU);
        editor.salvar(id,p); participation.desativar(id); participation.ativar(id);
        assertEquals(p,editor.buscar(id));
    }
}
