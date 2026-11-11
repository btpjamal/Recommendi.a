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
@SpringBootTest(classes=CommunityPostIntegrationTest.Config.class,webEnvironment=SpringBootTest.WebEnvironment.NONE,
 properties={"spring.config.name=community-posts-isolated","spring.jpa.hibernate.ddl-auto=validate",
 "spring.flyway.enabled=true","spring.flyway.baseline-on-migrate=false","spring.sql.init.mode=never"})
class CommunityPostIntegrationTest {
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
    @Import({CommunityPostService.class,CommunityService.class,SocialProfileService.class,SocialLocationService.class})
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
    @Test void persisteTextoImagemEDatasUtc() {
        var before=Instant.now().minusSeconds(1);byte[] image={1,2,3};
        var p=service.criar(community,a,new CreateCommunityPostDTO("  Descoberta  ",UUID.randomUUID()),image);
        assertEquals("Descoberta",p.conteudo());assertEquals(0L,p.versao());assertTrue(p.propria());
        assertTrue(p.criadoEm().isAfter(before));assertArrayEquals(image,service.imagem(community,b,p.id()));
        assertEquals(a,service.listar(community,b,null,20).publicacoes().get(0).autorId());
    }
    @Test void somenteImagemTambemEPublicacaoValida() {
        var p=service.criar(community,a,new CreateCommunityPostDTO(null,UUID.randomUUID()),new byte[]{7});
        assertTrue(p.possuiImagem());assertEquals("",p.conteudo());
        status(400,()->service.editar(community,a,p.id(),new EditCommunityPostDTO("",p.versao(),true),null));
    }
    @Test void naoMembrosNaoLeemNemPublicamNemBaixamImagem() {
        var p=service.criar(community,a,new CreateCommunityPostDTO("Foto",UUID.randomUUID()),new byte[]{1});
        status(403,()->service.listar(community,outsider,null,20));
        status(403,()->post(outsider,"Texto"));status(403,()->service.imagem(community,outsider,p.id()));
    }
    @Test void desativadoOuMenorNaoAcessaNemAltera() {
        var p=post(a,"Inicial");participation.desativar(a);
        status(403,()->service.listar(community,a,null,20));status(403,()->post(a,"Outro"));
        status(403,()->service.editar(community,a,p.id(),new EditCommunityPostDTO("Editar",0L,false),null));
        status(403,()->service.excluir(community,a,p.id(),0L));
        participation.ativar(a);var u=users.findById(a).orElseThrow();u.setDataNascimento(LocalDate.now().minusYears(17));users.saveAndFlush(u);
        status(403,()->service.listar(community,a,null,20));
    }
    @Test void somenteAutorAlteraEComunidadeNaoPodeSerTrocada() {
        var p=post(a,"Original");
        status(404,()->service.editar(community,b,p.id(),new EditCommunityPostDTO("Ataque",0L,false),null));
        status(404,()->service.excluir(community,b,p.id(),0L));
        status(404,()->service.editar(otherCommunity,a,p.id(),new EditCommunityPostDTO("Movida",0L,false),null));
        status(404,()->service.imagem(otherCommunity,a,p.id()));
        assertEquals("Original",service.listar(community,b,null,20).publicacoes().get(0).conteudo());
    }
    @Test void paginacaoNaoRepeteNemPulaComNovasPublicacoes() {
        for(int i=0;i<25;i++)post(a,"Publicação "+i);
        var first=service.listar(community,b,null,10);assertTrue(first.temMais());
        var newPost=post(a,"Novíssima");
        var second=service.listar(community,b,first.proximoCursor(),10);
        var third=service.listar(community,b,second.proximoCursor(),10);
        var ids=new HashSet<Long>();
        for(var page:List.of(first,second,third))for(var p:page.publicacoes())assertTrue(ids.add(p.id()));
        assertEquals(25,ids.size());assertFalse(ids.contains(newPost.id()));assertFalse(third.temMais());
        assertEquals(newPost.id(),service.listar(community,b,null,10).publicacoes().get(0).id());
    }
    @Test void bloqueioOcultaAntesDePaginarEImpedeImagemNosDoisSentidos() {
        var photo=service.criar(community,a,new CreateCommunityPostDTO("Foto",UUID.randomUUID()),new byte[]{1});
        for(int i=0;i<5;i++)post(a,"Oculta "+i);
        post(b,"Visível");
        var block=new SocialUserBlock();block.setBlockerId(a);block.setBlockedId(b);block.setCreatedAt(Instant.now());blocks.saveAndFlush(block);
        assertEquals(1,service.listar(community,b,null,1).publicacoes().size());
        assertFalse(service.listar(community,b,null,1).temMais());
        assertTrue(service.listar(community,a,null,20).publicacoes().stream().allMatch(p->p.autorId().equals(a)));
        status(404,()->service.imagem(community,b,photo.id()));
        blocks.delete(block);assertEquals(7,service.listar(community,b,null,20).publicacoes().size());
    }
    @Test void desativacaoDoAutorOcultaEReativacaoRestauraPublicacoes() {
        var p=service.criar(community,a,new CreateCommunityPostDTO("Foto",UUID.randomUUID()),new byte[]{1});
        participation.desativar(a);assertTrue(service.listar(community,b,null,20).publicacoes().isEmpty());
        status(404,()->service.imagem(community,b,p.id()));
        participation.ativar(a);assertEquals(1,service.listar(community,b,null,20).publicacoes().size());
    }
    @Test void mesmoUuidRetornaMesmaPublicacaoEMudancasConflitam() {
        var request=new CreateCommunityPostDTO("Texto",UUID.randomUUID());
        var p=service.criar(community,a,request,new byte[]{1});
        assertEquals(p.id(),service.criar(community,a,request,new byte[]{1}).id());assertEquals(1,posts.count());
        status(409,()->service.criar(community,a,new CreateCommunityPostDTO("Outro",request.clientId()),new byte[]{1}));
        status(409,()->service.criar(otherCommunity,a,request,new byte[]{1}));
    }
    @Test void substituiERemoveImagemERecusaVersaoAntiga() {
        var p=service.criar(community,a,new CreateCommunityPostDTO("Foto",UUID.randomUUID()),new byte[]{1});
        var edited=service.editar(community,a,p.id(),new EditCommunityPostDTO("Editada",p.versao(),false),new byte[]{9});
        assertEquals(1L,edited.versao());assertNotNull(edited.editadoEm());assertArrayEquals(new byte[]{9},service.imagem(community,b,p.id()));
        status(409,()->service.editar(community,a,p.id(),new EditCommunityPostDTO("Perdida",p.versao(),false),null));
        status(409,()->service.excluir(community,a,p.id(),p.versao()));
        var removed=service.editar(community,a,p.id(),new EditCommunityPostDTO("Sem foto",edited.versao(),true),null);
        assertFalse(removed.possuiImagem());assertFalse(images.existsById(p.id()));status(404,()->service.imagem(community,b,p.id()));
    }
    @Test void exclusaoRemoveDadosDaListaEDaImagemENaoPermiteRessuscitar() {
        var request=new CreateCommunityPostDTO("Foto",UUID.randomUUID());var p=service.criar(community,a,request,new byte[]{1});
        service.excluir(community,a,p.id(),p.versao());assertTrue(service.listar(community,b,null,20).publicacoes().isEmpty());
        assertFalse(images.existsById(p.id()));assertEquals("",posts.findById(p.id()).orElseThrow().getConteudo());
        status(404,()->service.imagem(community,b,p.id()));status(409,()->service.criar(community,a,request,new byte[]{1}));
    }
    @Test void exclusaoDeContaRemovePublicacoesEImagensDependentes() {
        service.criar(community,a,new CreateCommunityPostDTO("Foto",UUID.randomUUID()),new byte[]{1});
        users.deleteById(a);assertEquals(0,posts.count());assertEquals(0,images.count());
    }
    @Test void enviosConcorrentesComMesmoUuidCriamUmaSoPublicacao() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        var request=new CreateCommunityPostDTO("Única",UUID.randomUUID());
        try {
            Callable<CommunityPostDTO> task=()->{start.await();return service.criar(community,a,request,null);};
            var x=pool.submit(task);var y=pool.submit(task);start.countDown();
            assertEquals(x.get(20,TimeUnit.SECONDS).id(),y.get(20,TimeUnit.SECONDS).id());assertEquals(1,posts.count());
        } finally {start.countDown();pool.shutdownNow();}
    }
    @Test void edicoesConcorrentesNaoPerdemAlteracaoSilenciosamente() throws Exception {
        var p=post(a,"Inicial");var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            Callable<Integer> task=()->{start.await();try {
                service.editar(community,a,p.id(),new EditCommunityPostDTO(UUID.randomUUID().toString(),0L,false),null);return 200;
            } catch(ResponseStatusException e) {return e.getStatusCode().value();}};
            var x=pool.submit(task);var y=pool.submit(task);start.countDown();
            assertEquals(Set.of(200,409),Set.of(x.get(20,TimeUnit.SECONDS),y.get(20,TimeUnit.SECONDS)));
        } finally {start.countDown();pool.shutdownNow();}
    }
    @Test void saidaConfirmadaAntesDoEnvioImpedeNovaPublicacao() throws Exception {
        var pool=Executors.newSingleThreadExecutor();var started=new CountDownLatch(1);
        try {
            Future<Integer> pending=new TransactionTemplate(transactions).execute(tx->{
                locks.lockUser(a);communities.sair(community,a);
                var future=pool.submit(()->{started.countDown();try {post(a,"Após saída");return 201;}
                    catch(ResponseStatusException e){return e.getStatusCode().value();}});
                try {assertTrue(started.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}
                return future;
            });
            assertEquals(403,pending.get(20,TimeUnit.SECONDS));assertEquals(0,posts.count());
        } finally {pool.shutdownNow();}
    }
}
