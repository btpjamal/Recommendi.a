package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.Enum.Role;

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
@SpringBootTest(classes=CommunityInteractionIntegrationTest.Config.class,webEnvironment=SpringBootTest.WebEnvironment.NONE,
 properties={"spring.config.name=community-posts-isolated","spring.jpa.hibernate.ddl-auto=validate",
 "spring.flyway.enabled=true","spring.flyway.baseline-on-migrate=false","spring.sql.init.mode=never"})
class CommunityInteractionIntegrationTest {
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
    @Import({CommunityInteractionService.class,CommunityModerationService.class,CommunityPostService.class,CommunityService.class,SocialProfileService.class,SocialLocationService.class})
    static class Config {}
    @Autowired CommunityInteractionService interactions;
    @Autowired CommunityModerationService moderation;
    @Autowired CommunityCommentRepository comments;
    @Autowired CommunityPostLikeRepository likes;
    @Autowired CommunityReportRepository reports;
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
        var admin=users.findById(outsider).orElseThrow();admin.setRole(Role.ADMIN);users.saveAndFlush(admin);
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

    CommunityCommentDTO comment(Long author,Long post,String text) {
        return interactions.comentar(community,author,post,new CreateCommunityCommentDTO(text,UUID.randomUUID()));
    }
    CreateCommunityReportDTO reason(){return new CreateCommunityReportDTO(CommunityReportReason.SPAM,"Justificativa da denúncia",0L);}
    CommunityReportDTO reportView(Long id){return moderation.listar(outsider,CommunityReportStatus.ABERTA,null,20).denuncias().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();}
    ReviewCommunityReportDTO review(CommunityReportDTO r,CommunityModerationDecision decision){return new ReviewCommunityReportDTO(decision,"Decisão de teste",r.versaoDenuncia(),r.versaoConteudo());}

    @Test void curtidasIdempotentesNaoAlteramVersaoDaPublicacao(){
        var p=post(a,"Texto");assertEquals(1,interactions.curtir(community,b,p.id(),true).curtidas());
        assertEquals(1,interactions.curtir(community,b,p.id(),true).curtidas());assertEquals(1,likes.count());
        assertEquals(p.versao(),posts.findById(p.id()).orElseThrow().getVersion());
        assertEquals(0,interactions.curtir(community,b,p.id(),false).curtidas());assertEquals(0,interactions.curtir(community,b,p.id(),false).curtidas());
    }
    @Test void comentariosPersistemEExigemAutoriaEVersao(){
        var p=post(a,"Texto");var c=comment(b,p.id(),"  Comentário  ");assertEquals("Comentário",c.conteudo());
        status(404,()->interactions.editar(community,a,p.id(),c.id(),new EditCommunityCommentDTO("Ataque",0L)));
        var updated=interactions.editar(community,b,p.id(),c.id(),new EditCommunityCommentDTO("Editado",0L));
        assertEquals(1L,updated.versao());assertNotNull(updated.editadoEm());
        status(409,()->interactions.excluir(community,b,p.id(),c.id(),0L));
        interactions.excluir(community,b,p.id(),c.id(),1L);assertEquals("",comments.findById(c.id()).orElseThrow().getConteudo());
        assertEquals(0,interactions.resumo(community,a,p.id()).comentarios());
    }
    @Test void comentariosSaoPaginadosSemDuplicacao(){
        var p=post(a,"Texto");for(int i=0;i<25;i++)comment(b,p.id(),"Comentário "+i);
        var x=interactions.listar(community,a,p.id(),null,10);var y=interactions.listar(community,a,p.id(),x.proximoCursor(),10);
        var z=interactions.listar(community,a,p.id(),y.proximoCursor(),10);var ids=new HashSet<Long>();
        for(var page:List.of(x,y,z))for(var c:page.comentarios())assertTrue(ids.add(c.id()));
        assertEquals(25,ids.size());assertFalse(z.temMais());
    }
    @Test void bloqueioFiltraComentariosEContagensAntesDePaginar(){
        var p=post(a,"Texto");comment(a,p.id(),"Visível");for(int i=0;i<5;i++)comment(b,p.id(),"Oculto "+i);
        interactions.curtir(community,b,p.id(),true);var block=new SocialUserBlock();block.setBlockerId(a);block.setBlockedId(b);block.setCreatedAt(Instant.now());blocks.saveAndFlush(block);
        var page=interactions.listar(community,a,p.id(),null,1);assertEquals(1,page.comentarios().size());assertFalse(page.temMais());
        assertEquals(1,interactions.resumo(community,a,p.id()).comentarios());assertEquals(0,interactions.resumo(community,a,p.id()).curtidas());
        status(404,()->interactions.resumo(community,b,p.id()));blocks.delete(block);assertEquals(6,interactions.resumo(community,a,p.id()).comentarios());
    }
    @Test void desativacaoOcultaInteracoesDoAutorEImpedeNovas(){
        var p=post(a,"Texto");comment(b,p.id(),"Comentário");interactions.curtir(community,b,p.id(),true);participation.desativar(b);
        assertEquals(0,interactions.resumo(community,a,p.id()).curtidas());assertEquals(0,interactions.resumo(community,a,p.id()).comentarios());
        status(403,()->comment(b,p.id(),"Outro"));status(403,()->interactions.curtir(community,b,p.id(),true));
    }
    @Test void naoMembroNaoInterageNemDenuncia(){
        var p=post(a,"Texto");status(403,()->interactions.curtir(community,outsider,p.id(),true));
        status(403,()->interactions.denunciar(community,outsider,p.id(),null,reason()));
    }
    @Test void reenvioDeComentarioEUnicoEExclusaoNaoRessuscita(){
        var p=post(a,"Texto");var req=new CreateCommunityCommentDTO("Único",UUID.randomUUID());var c=interactions.comentar(community,b,p.id(),req);
        assertEquals(c.id(),interactions.comentar(community,b,p.id(),req).id());assertEquals(1,comments.count());
        interactions.excluir(community,b,p.id(),c.id(),0L);status(409,()->interactions.comentar(community,b,p.id(),req));
    }
    @Test void denunciaPorVersaoNaoDuplicaMasEdicaoPermiteNova(){
        var p=post(a,"Texto");var r=interactions.denunciar(community,b,p.id(),null,reason());
        assertEquals(r.id(),interactions.denunciar(community,b,p.id(),null,reason()).id());
        var view=reportView(r.id());moderation.analisar(outsider,r.id(),review(view,CommunityModerationDecision.MANTER));
        assertEquals(r.id(),interactions.denunciar(community,b,p.id(),null,reason()).id());
        service.editar(community,a,p.id(),new EditCommunityPostDTO("Alterada",0L,false),null);
        assertNotEquals(r.id(),interactions.denunciar(community,b,p.id(),null,new CreateCommunityReportDTO(CommunityReportReason.SPAM,"",1L)).id());assertEquals(2,reports.count());
    }
    @Test void ocultarPublicacaoImpedeListaImagemEInteracoes(){
        var p=service.criar(community,a,new CreateCommunityPostDTO("Foto",UUID.randomUUID()),new byte[]{1});
        var r=interactions.denunciar(community,b,p.id(),null,reason());var view=reportView(r.id());
        moderation.analisar(outsider,r.id(),review(view,CommunityModerationDecision.OCULTAR));
        assertTrue(service.listar(community,a,null,20).publicacoes().isEmpty());status(404,()->service.imagem(community,a,p.id()));
        status(404,()->interactions.resumo(community,b,p.id()));status(404,()->comment(a,p.id(),"Novo"));
        status(404,()->service.editar(community,a,p.id(),new EditCommunityPostDTO("Contorno",1L,false),null));
        assertArrayEquals(new byte[]{1},moderation.imagem(outsider,r.id()));
    }
    @Test void ocultarComentarioNaoOcultaPublicacao(){
        var p=post(a,"Texto");var c=comment(b,p.id(),"Comentário");var r=interactions.denunciar(community,a,p.id(),c.id(),reason());
        moderation.analisar(outsider,r.id(),review(reportView(r.id()),CommunityModerationDecision.OCULTAR));
        assertEquals(1,service.listar(community,a,null,20).publicacoes().size());assertEquals(0,interactions.resumo(community,a,p.id()).comentarios());
        status(404,()->interactions.editar(community,b,p.id(),c.id(),new EditCommunityCommentDTO("Contorno",1L)));
    }
    @Test void evidenciaDaPublicacaoSobreviveAEdicaoEExclusaoEAposPrazoExpira() {
        var p = service.criar(community, a, new CreateCommunityPostDTO("Original", UUID.randomUUID()), new byte[]{1});
        var r = interactions.denunciar(community, b, p.id(), null, reason());
        var edit = service.editar(community, a, p.id(), new EditCommunityPostDTO("Editada", p.versao(), false), new byte[]{2});
        assertEquals("Editada", reportView(r.id()).publicacao());
        assertEquals("Original", reportView(r.id()).evidencia().publicacao());
        status(403, () -> moderation.imagemDenunciada(b, r.id()));
        service.excluir(community, a, p.id(), edit.versao());
        var evidence = reportView(r.id()).evidencia();
        assertEquals("Original", evidence.publicacao());
        assertTrue(evidence.sha256().matches("[a-f0-9]{64}"));
        assertTrue(evidence.possuiImagem());
        assertArrayEquals(new byte[]{1}, moderation.imagemDenunciada(outsider, r.id()));

        jdbc.update("UPDATE community_report_evidence SET expires_at = now() - interval '1 second' WHERE report_id = ?", r.id());
        assertNull(reportView(r.id()).evidencia());
        status(404, () -> moderation.imagemDenunciada(outsider, r.id()));
        assertEquals(1, reports.deleteExpiredEvidence());
        assertTrue(reports.existsById(r.id()));
    }

    @Test void evidenciaDoComentarioSobreviveAExclusao() {
        var p = post(a, "Contexto");
        var c = comment(b, p.id(), "Comentário original");
        var r = interactions.denunciar(community, a, p.id(), c.id(), reason());
        var edit = interactions.editar(community, b, p.id(), c.id(), new EditCommunityCommentDTO("Editado", c.versao()));
        interactions.excluir(community, b, p.id(), c.id(), edit.versao());
        var evidence = reportView(r.id()).evidencia();
        assertEquals("Contexto", evidence.publicacao());
        assertEquals("Comentário original", evidence.comentario());
        assertFalse(evidence.possuiImagem());
    }

    @Test void moderadorPrecisaRevisarVersaoAtual(){
        var p=post(a,"Texto");var r=interactions.denunciar(community,b,p.id(),null,reason());var old=reportView(r.id());
        service.editar(community,a,p.id(),new EditCommunityPostDTO("Nova versão",0L,false),null);
        status(409,()->moderation.analisar(outsider,r.id(),review(old,CommunityModerationDecision.OCULTAR)));
        var current=reportView(r.id());assertNotEquals(current.versaoDenunciada(),current.versaoConteudo());
        moderation.analisar(outsider,r.id(),review(current,CommunityModerationDecision.MANTER));
        status(409,()->moderation.analisar(outsider,r.id(),review(current,CommunityModerationDecision.OCULTAR)));
    }
    @Test void usuarioComumNaoLeDenunciasNemModeraMesmoPeloServico(){
        status(403,()->moderation.listar(a,CommunityReportStatus.ABERTA,null,20));status(403,()->moderation.imagem(a,1L));
    }
    @Test void exclusaoDeContaLimpaComentariosCurtidasEDenuncias(){
        var p=post(a,"Texto");comment(b,p.id(),"Comentário");interactions.curtir(community,b,p.id(),true);interactions.denunciar(community,b,p.id(),null,reason());
        users.deleteById(b);assertEquals(0,comments.count());assertEquals(0,likes.count());assertEquals(0,reports.count());assertEquals(1,posts.count());
    }
    @Test void mesmoComentarioConcorrenteNaoDuplica() throws Exception {
        var p=post(a,"Texto");var req=new CreateCommunityCommentDTO("Concorrente",UUID.randomUUID());
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{Callable<Long> task=()->{start.await();return interactions.comentar(community,b,p.id(),req).id();};var x=pool.submit(task);var y=pool.submit(task);start.countDown();assertEquals(x.get(20,TimeUnit.SECONDS),y.get(20,TimeUnit.SECONDS));assertEquals(1,comments.count());}
        finally{start.countDown();pool.shutdownNow();}
    }
    @Test void mesmaCurtidaConcorrenteNaoDuplica() throws Exception {
        var p=post(a,"Texto");var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{Callable<Long> task=()->{start.await();return interactions.curtir(community,b,p.id(),true).curtidas();};var x=pool.submit(task);var y=pool.submit(task);start.countDown();assertEquals(1L,x.get(20,TimeUnit.SECONDS));assertEquals(1L,y.get(20,TimeUnit.SECONDS));assertEquals(1,likes.count());}
        finally{start.countDown();pool.shutdownNow();}
    }
    @Test void doisModeradoresNaoEncerramAMesmaDenunciaDuasVezes() throws Exception {
        var p=post(a,"Texto");var r=interactions.denunciar(community,b,p.id(),null,reason());var snapshot=reportView(r.id());
        Long second=user();var u=users.findById(second).orElseThrow();u.setRole(Role.ADMIN);users.saveAndFlush(u);
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            java.util.function.Function<Long,Callable<Integer>> task=id->()->{start.await();try {
                moderation.analisar(id,r.id(),review(snapshot,CommunityModerationDecision.MANTER));return 200;
            }catch(ResponseStatusException e){return e.getStatusCode().value();}};
            var x=pool.submit(task.apply(outsider));var y=pool.submit(task.apply(second));start.countDown();
            assertEquals(Set.of(200,409),Set.of(x.get(20,TimeUnit.SECONDS),y.get(20,TimeUnit.SECONDS)));
        }finally{start.countDown();pool.shutdownNow();}
    }
}
