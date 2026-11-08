package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.Hobby.HobbyCreateRequestDTO;
import dev.jamal.projetotcc.DTO.Hobby.HobbyInterestAssociationDTO;
import dev.jamal.projetotcc.DTO.Hobby.HobbyObjectiveAssociationDTO;
import dev.jamal.projetotcc.Entities.HobbyCategory;
import dev.jamal.projetotcc.Entities.Interest;
import dev.jamal.projetotcc.Entities.Objective;
import dev.jamal.projetotcc.Enum.AmbientePreferido;
import dev.jamal.projetotcc.Enum.NivelAtividadeFisica;
import dev.jamal.projetotcc.Enum.TipoSocializacao;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Exception.ResourceNotFoundException;
import dev.jamal.projetotcc.Mapper.HobbyCategoryMapper;
import dev.jamal.projetotcc.Mapper.HobbyMapper;
import dev.jamal.projetotcc.Repository.HobbyCategoryRepository;
import dev.jamal.projetotcc.Repository.InterestRepository;
import dev.jamal.projetotcc.Repository.ObjectiveRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = HobbyRecommendationAdminIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.name=hobby-weights-integration-isolated",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.sql.init.mode=never"
        })
class HobbyRecommendationAdminIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("hobby_weights_integration")
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
    @Import({HobbyService.class, HobbyMapper.class, HobbyCategoryMapper.class,
            HobbyRecommendationAdminService.class})
    static class Config {}

    @Autowired HobbyService hobbyService;
    @Autowired HobbyRecommendationAdminService consulta;
    @Autowired HobbyCategoryRepository categorias;
    @Autowired InterestRepository interesses;
    @Autowired ObjectiveRepository objetivos;

    @Test
    void consultaDevolvePesosAtuaisDepoisDeCriarEEditar() {
        var pedido = prepararPedido();
        Long id = hobbyService.salvar(pedido).getId();
        var inicial = consulta.buscar(id);
        assertEquals(pedido.getInterestAssociations(), inicial.interestAssociations());
        assertEquals(pedido.getObjectiveAssociations(), inicial.objectiveAssociations());

        // Altera o peso mantendo os mesmos IDs, como no modal de edicao.
        Long interesseId = pedido.getInterestAssociations().get(0).interestId();
        Long objetivoId = pedido.getObjectiveAssociations().get(0).objectiveId();
        pedido.setInterestAssociations(List.of(new HobbyInterestAssociationDTO(interesseId, 3)));
        pedido.setObjectiveAssociations(List.of(new HobbyObjectiveAssociationDTO(objetivoId, 2)));
        hobbyService.atualizar(id, pedido);

        var atual = consulta.buscar(id);
        assertEquals(pedido.getInterestAssociations(), atual.interestAssociations());
        assertEquals(pedido.getObjectiveAssociations(), atual.objectiveAssociations());
    }

    @Test
    void hobbyAtivoNaoPerdeAssociacoesQuandoAtualizacaoERejeitada() {
        var pedido = prepararPedido();
        Long id = hobbyService.salvar(pedido).getId();
        hobbyService.reativar(id);
        var antes = consulta.buscar(id);
        pedido.setInterestAssociations(List.of());

        assertThrows(BusinessException.class, () -> hobbyService.atualizar(id, pedido));
        assertEquals(antes, consulta.buscar(id));
    }

    @Test
    void hobbyInexistenteRetornaErroDeNaoEncontrado() {
        assertThrows(ResourceNotFoundException.class, () -> consulta.buscar(Long.MAX_VALUE));
    }

    private HobbyCreateRequestDTO prepararPedido() {
        String sufixo = UUID.randomUUID().toString();
        var categoria = new HobbyCategory();
        categoria.setNome("Categoria " + sufixo);
        categorias.saveAndFlush(categoria);
        var interesse = new Interest();
        interesse.setNome("Interesse " + sufixo);
        interesses.saveAndFlush(interesse);
        var objetivo = new Objective();
        objetivo.setNome("Objetivo " + sufixo);
        objetivos.saveAndFlush(objetivo);

        var pedido = new HobbyCreateRequestDTO();
        pedido.setNome("Hobby " + sufixo);
        pedido.setDescricao("Hobby exclusivo do banco descartavel de testes.");
        pedido.setCategoryId(categoria.getId());
        pedido.setCustoEstimado(0.0);
        pedido.setNivelDificuldade(1);
        pedido.setTempoNecessario(1.0);
        pedido.setTipoSocializacao(TipoSocializacao.INDIVIDUAL);
        pedido.setNivelAtividadeFisica(NivelAtividadeFisica.BAIXO);
        pedido.setAmbiente(AmbientePreferido.CASA);
        pedido.setInterestAssociations(List.of(new HobbyInterestAssociationDTO(interesse.getId(), 1)));
        pedido.setObjectiveAssociations(List.of(new HobbyObjectiveAssociationDTO(objetivo.getId(), 1)));
        return pedido;
    }
}