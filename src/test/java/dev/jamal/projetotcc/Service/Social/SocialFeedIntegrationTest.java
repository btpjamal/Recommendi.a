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
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes=SocialFeedIntegrationTest.Config.class,webEnvironment=SpringBootTest.WebEnvironment.NONE,
 properties={"spring.config.name=community-posts-isolated","spring.jpa.hibernate.ddl-auto=validate",
 "spring.flyway.enabled=true","spring.flyway.baseline-on-migrate=false","spring.sql.init.mode=never"})
class SocialFeedIntegrationTest {
    @Container static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES=
        new org.testcontainers.postgresql.PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("community_posts_test").withUsername("integration").withPassword("integration");
    @DynamicPropertySource static void banco(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);
        r.add("spring.datasource.password",POSTGRES::getPassword);r.add("spring.flyway.url",POSTGRES::getJdbcUrl);
        r.add("spring.flyway.user",POSTGRES::getUsername);r.add("spring.flyway.password",POSTGRES::getPassword);
    }
    @Configuration(proxyBeanMethods=false) @EnableAutoConfiguration
    @EntityScan("dev.jamal.projetotcc.Entities") @EnableJpaRepositories("dev.jamal.projetotcc.Repository")
    @Import({CommunityPostService.class,CommunityService.class,SocialProfileService.class,SocialLocationService.class, SocialFollowService.class, SocialFollowStore.class, SocialFeedService.class})
    static class Config {}
    @Autowired CommunityPostService service;
    @Autowired CommunityService communities;
    @Autowired SocialProfileService participation;
    @Autowired UserRepository users;
    @Autowired CommunityPostRepository posts;
    @Autowired CommunityPostImageRepository images;
    @Autowired SocialProfileRepository profiles;
    @Autowired SocialUserBlockRepository blocks;
    @Autowired HobbyRepository hobbies;
    @Autowired HobbyCategoryRepository categories;
    @Autowired CommunityChatLockRepository locks;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired SocialFeedService feed;
    @Autowired SocialFollowService follows;
    @Autowired SocialFollowStore followStore;
    Long a,b,outsider,community,otherCommunity;

    @BeforeEach void setup() {
        jdbc.execute("TRUNCATE users, hobby, hobby_category CASCADE");
        a=user();b=user();outsider=user();
        var category=new HobbyCategory();category.setNome("Categoria de teste");categories.saveAndFlush(category);
        for(String name:List.of("Hobby A","Hobby B")) {
            var h=new Hobby();h.setNome(name);h.setCategory(category);h.setTipoSocializacao(TipoSocializacao.SOCIAL);
            h.setNivelAtividadeFisica(NivelAtividadeFisica.BAIXO);h.setAmbiente(AmbientePreferido.CASA);hobbies.saveAndFlush(h);
        }
        communities.garantirComunidadesGerais();var list=communities.listarGerais(a);
        community=list.get(0).id();otherCommunity=list.get(1).id();
        communities.entrar(community,a);communities.entrar(community,b);communities.entrar(otherCommunity,a);
    }
    Long user() {
        var u=new User();u.setNome("Participante de teste");u.setEmail(UUID.randomUUID()+"@example.test");
        u.setSenha("banco-descartavel");u.setDataNascimento(LocalDate.of(1990,1,1));users.saveAndFlush(u);
        participation.ativar(u.getId());return u.getId();
    }
    CommunityPostDTO post(Long author,String text) {
        return service.criar(community,author,new CreateCommunityPostDTO(text,UUID.randomUUID()),null);
    }
    void status(int code,org.junit.jupiter.api.function.Executable f) {
        assertEquals(code,assertThrows(ResponseStatusException.class,f).getStatusCode().value());
    }

    @Test void seguirEIdempotenteEDirecionalSemConcederParticipacao() {
        assertFalse(follows.consultar(a,b).seguindo());
        assertTrue(follows.alterar(a,b,true).seguindo());
        assertTrue(follows.alterar(a,b,true).seguindo());
        assertFalse(follows.consultar(b,a).seguindo());
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM social_follows",Long.class));
        communities.entrar(otherCommunity,b);
        service.criar(otherCommunity,a,new CreateCommunityPostDTO("Reservada",UUID.randomUUID()),null);
        communities.sair(otherCommunity,b);
        follows.alterar(b,a,true);
        assertTrue(feed.listar(b,true,null,20).itens().isEmpty());
        status(403,()->service.listar(otherCommunity,b,null,20));
        assertFalse(follows.alterar(a,b,false).seguindo());
        assertFalse(follows.alterar(a,b,false).seguindo());
    }

    @Test void feedCombinaComunidadesPermitidasEFiltraSeguidosAntesDaPaginacao() {
        var first=post(b,"Da comunidade compartilhada");
        var own=service.criar(otherCommunity,a,new CreateCommunityPostDTO("Outra comunidade",UUID.randomUUID()),null);
        communities.entrar(otherCommunity,outsider);
        service.criar(otherCommunity,outsider,new CreateCommunityPostDTO("Também visível",UUID.randomUUID()),null);
        follows.alterar(a,b,true);
        var page=feed.listar(a,true,null,1);
        assertEquals(List.of(first.id()),page.itens().stream().map(i->i.publicacao().id()).toList());
        assertFalse(page.temMais());
        assertEquals(3,feed.listar(a,false,null,20).itens().size());
        assertEquals(1,feed.listar(b,false,null,20).itens().size());
        assertFalse(feed.listar(b,false,null,20).itens().stream().anyMatch(i->i.publicacao().id().equals(own.id())));
        var item=page.itens().get(0);
        assertNotNull(item.hobbyNome());assertEquals("GERAL",item.tipoComunidade());
        assertFalse(item.publicacao().propria());
    }

    @Test void bloqueioRemoveAmbasRelacoesENaoRestauraAoDesbloquear() {
        post(b,"Antes do bloqueio");
        follows.alterar(a,b,true);follows.alterar(b,a,true);
        var block=new SocialUserBlock();block.setBlockerId(b);block.setBlockedId(a);block.setCreatedAt(Instant.now());
        blocks.saveAndFlush(block);
        assertFalse(followStore.seguindo(a,b));assertFalse(followStore.seguindo(b,a));
        assertTrue(feed.listar(a,false,null,20).itens().isEmpty());
        status(404,()->follows.alterar(a,b,true));status(404,()->follows.consultar(a,b));
        blocks.delete(block);
        assertTrue(feed.listar(a,true,null,20).itens().isEmpty());
        assertEquals(1,feed.listar(a,false,null,20).itens().size());
    }

    @Test void ocultacaoExclusaoInatividadeEMenoridadeSaoAplicadasNoFeed() {
        var p=post(b,"Ocultada");follows.alterar(a,b,true);
        jdbc.update("UPDATE community_posts SET hidden_at = CURRENT_TIMESTAMP WHERE id = ?",p.id());
        assertTrue(feed.listar(a,true,null,20).itens().isEmpty());
        jdbc.update("UPDATE community_posts SET hidden_at = NULL WHERE id = ?",p.id());
        participation.desativar(b);
        assertTrue(feed.listar(a,false,null,20).itens().isEmpty());status(404,()->follows.alterar(a,b,true));
        participation.ativar(b);assertEquals(1,feed.listar(a,true,null,20).itens().size());
        jdbc.update("UPDATE users SET data_nascimento = ? WHERE id = ?",LocalDate.now().minusYears(17),b);
        assertTrue(feed.listar(a,false,null,20).itens().isEmpty());
        jdbc.update("UPDATE users SET data_nascimento = ? WHERE id = ?",LocalDate.of(1990,1,1),b);
        service.excluir(community,b,p.id(),p.versao());
        assertTrue(feed.listar(a,false,null,20).itens().isEmpty());
    }

    @Test void saidaDaComunidadeEParticipacaoDoLeitorSaoReavaliadas() {
        post(b,"Teste");communities.sair(community,a);
        assertTrue(feed.listar(a,false,null,20).itens().isEmpty());
        communities.entrar(community,a);assertEquals(1,feed.listar(a,false,null,20).itens().size());
        participation.desativar(a);
        status(403,()->feed.listar(a,false,null,20));status(403,()->follows.alterar(a,b,true));
        participation.ativar(a);
        jdbc.update("UPDATE users SET data_nascimento = ? WHERE id = ?",LocalDate.now().minusYears(17),a);
        status(403,()->feed.listar(a,false,null,20));
    }

    @Test void cursorNaoDuplicaPaginasComNovasPublicacoes() {
        for(int i=0;i<25;i++)post(b,"Publicação "+i);
        var first=feed.listar(a,false,null,10);var newest=post(b,"Nova");
        var second=feed.listar(a,false,first.proximoCursor(),10);
        var third=feed.listar(a,false,second.proximoCursor(),10);
        var ids=new HashSet<Long>();
        for(var page:List.of(first,second,third))for(var item:page.itens())assertTrue(ids.add(item.publicacao().id()));
        assertEquals(25,ids.size());assertFalse(ids.contains(newest.id()));assertFalse(third.temMais());
        assertEquals(newest.id(),feed.listar(a,false,null,10).itens().get(0).publicacao().id());
    }

    @Test void entradasInvalidasSaoRecusadas() {
        status(400,()->follows.alterar(a,a,true));status(400,()->follows.consultar(a,-1L));
        status(404,()->follows.alterar(a,Long.MAX_VALUE,true));
        status(400,()->feed.listar(a,false,0L,20));status(400,()->feed.listar(a,false,null,31));
        status(400,()->feed.listar(a,false,null,0));
    }

    @Test void exclusaoDeContaRemoveRelacoes() {
        follows.alterar(a,b,true);follows.alterar(b,a,true);users.deleteById(b);
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM social_follows",Long.class));
    }

    @Test void seguirConcorrenteNaoDuplica() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            Callable<SocialFollowDTO> task=()->{start.await();return follows.alterar(a,b,true);};
            var x=pool.submit(task);var y=pool.submit(task);start.countDown();
            assertTrue(x.get(20,TimeUnit.SECONDS).seguindo());assertTrue(y.get(20,TimeUnit.SECONDS).seguindo());
            assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM social_follows",Long.class));
        } finally {start.countDown();pool.shutdownNow();}
    }

    @Test void bloqueioConcorrenteComSeguirImpedeRelacaoResidual() throws Exception {
        var pool=Executors.newSingleThreadExecutor();var started=new CountDownLatch(1);
        try {
            Future<Integer> future=new TransactionTemplate(transactions).execute(tx->{
                locks.lockUser(Math.min(a,b));locks.lockUser(Math.max(a,b));
                var pending=pool.submit(()->{started.countDown();try {follows.alterar(a,b,true);return 200;}
                    catch(ResponseStatusException e){return e.getStatusCode().value();}});
                try {assertTrue(started.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}
                var block=new SocialUserBlock();block.setBlockerId(a);block.setBlockedId(b);block.setCreatedAt(Instant.now());
                blocks.saveAndFlush(block);return pending;
            });
            assertEquals(404,future.get(20,TimeUnit.SECONDS));assertFalse(followStore.seguindo(a,b));
        } finally {pool.shutdownNow();}
    }
}

