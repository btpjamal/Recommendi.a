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
@SpringBootTest(classes=SocialNotificationIntegrationTest.Config.class,webEnvironment=SpringBootTest.WebEnvironment.NONE,
 properties={"spring.config.name=social-notifications-isolated","spring.jpa.hibernate.ddl-auto=validate",
 "spring.flyway.enabled=true","spring.flyway.baseline-on-migrate=false","spring.sql.init.mode=never"})
class SocialNotificationIntegrationTest {
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
    @Import({SocialNotificationService.class,SocialNotificationRecorder.class,SocialNotificationStore.class,SocialContactService.class,SocialHubService.class,SocialAffinityService.class,SocialProfileResumeService.class,CommunityInteractionService.class,CommunityModerationService.class,CommunityPostService.class,CommunityService.class,SocialProfileService.class,SocialLocationService.class})
    static class Config {}
    @Autowired SocialNotificationService notifications;
    @Autowired SocialContactService contacts;
    @Autowired SocialContactRepository contactRows;
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


    CommunityCommentDTO comment(Long author,Long post,String text){return interactions.comentar(community,author,post,new CreateCommunityCommentDTO(text,UUID.randomUUID()));}
    SocialContactDTO request(){return contacts.solicitar(a,new SocialContactRequestDTO(b,"Olá"));}
    long unread(Long user){return notifications.contar(user).naoLidas();}
    long stored(){return jdbc.queryForObject("SELECT count(*) FROM social_notifications",Long.class);}
    SocialNotificationDTO first(Long user){return notifications.listar(user,null,20).notificacoes().get(0);}

    @Test void solicitacaoNotificaSomenteDestinatarioUmaVez(){
        request();assertEquals(1,unread(b));assertEquals(0,unread(a));request();assertEquals(1,stored());
        assertEquals(SocialNotificationType.SOLICITACAO_CONTATO,first(b).tipo());
    }
    @Test void aceiteNotificaRemetenteUmaVezEEncerraAvisoPendente(){
        var r=request();contacts.responder(b,r.id(),true);contacts.responder(b,r.id(),true);
        assertEquals(1,unread(a));assertEquals(0,unread(b));assertEquals(2,stored());
        assertEquals(SocialNotificationType.CONTATO_ACEITO,first(a).tipo());
    }
    @Test void cancelamentoERecusaOcultamSolicitacao(){
        var r=request();contacts.cancelar(a,r.id());assertEquals(0,unread(b));
        assertTrue(notifications.listar(b,null,20).notificacoes().isEmpty());
        var row=contactRows.findById(r.id()).orElseThrow();row.setUpdatedAt(Instant.now().minus(2,java.time.temporal.ChronoUnit.DAYS));contactRows.saveAndFlush(row);
        var next=request();contacts.responder(b,next.id(),false);assertEquals(0,unread(b));
    }
    @Test void novaSolicitacaoNaoRessuscitaAvisoDoCicloAntigo(){
        var r=request();Long old=first(b).id();var oldCycle=contactRows.findById(r.id()).orElseThrow().getNotificationCycle();contacts.cancelar(a,r.id());
        var row=contactRows.findById(r.id()).orElseThrow();row.setUpdatedAt(Instant.now().minus(2,java.time.temporal.ChronoUnit.DAYS));contactRows.saveAndFlush(row);request();
        assertNotEquals(oldCycle,contactRows.findById(r.id()).orElseThrow().getNotificationCycle());assertEquals(1,unread(b));assertNotEquals(old,first(b).id());
        status(404,()->notifications.marcarLida(b,old));
    }
    @Test void comentarioNotificaDonoSemAvisarAcaoPropriaOuDuplicar(){
        var p=post(a,"Publicação");comment(a,p.id(),"Próprio");assertEquals(0,stored());
        var req=new CreateCommunityCommentDTO("Texto privado do comentário",UUID.randomUUID());interactions.comentar(community,b,p.id(),req);interactions.comentar(community,b,p.id(),req);
        assertEquals(1,unread(a));assertEquals(0,unread(b));assertEquals(1,stored());assertFalse(first(a).mensagem().contains("Texto privado"));
    }
    @Test void edicaoDeComentarioNaoGeraNovoAviso(){
        var p=post(a,"Publicação");var c=comment(b,p.id(),"Original");interactions.editar(community,b,p.id(),c.id(),new EditCommunityCommentDTO("Editado",0L));assertEquals(1,stored());
    }
    @Test void leituraEPrivadaEIdempotente(){
        var p=post(a,"Publicação");comment(b,p.id(),"Texto");Long id=first(a).id();status(404,()->notifications.marcarLida(b,id));
        notifications.marcarLida(a,id);assertEquals(0,unread(a));assertTrue(first(a).lida());
        var old=Instant.parse("2020-01-01T00:00:00Z");jdbc.update("UPDATE social_notifications SET read_at=? WHERE id=?",java.sql.Timestamp.from(old),id);
        notifications.marcarLida(a,id);assertEquals(old,first(a).lidaEm());
    }
    @Test void bloqueioOcultaListaContagemELeitura(){
        var p=post(a,"Publicação");comment(b,p.id(),"Texto");Long id=first(a).id();contacts.bloquear(a,b);
        assertEquals(0,unread(a));assertTrue(notifications.listar(a,null,20).notificacoes().isEmpty());status(404,()->notifications.marcarLida(a,id));
        contacts.desbloquear(a,b);assertEquals(1,unread(a));
    }
    @Test void desativacaoDoAutorOuDestinatarioEAplicada(){
        var p=post(a,"Publicação");comment(b,p.id(),"Texto");participation.desativar(b);assertEquals(0,unread(a));participation.ativar(b);assertEquals(1,unread(a));
        participation.desativar(a);status(403,()->notifications.contar(a));participation.ativar(a);assertEquals(1,unread(a));
    }
    @Test void saidaDaComunidadeOcultaAviso(){
        var p=post(a,"Publicação");comment(b,p.id(),"Texto");communities.sair(community,a);assertEquals(0,unread(a));communities.entrar(community,a);assertEquals(1,unread(a));
    }
    @Test void exclusaoEOcultacaoDoComentarioInvalidamAviso(){
        var p=post(a,"Publicação");var c=comment(b,p.id(),"Texto");Long id=first(a).id();
        var row=comments.findById(c.id()).orElseThrow();row.setHiddenAt(Instant.now());row=comments.saveAndFlush(row);assertEquals(0,unread(a));status(404,()->notifications.marcarLida(a,id));
        row.setHiddenAt(null);row=comments.saveAndFlush(row);interactions.excluir(community,b,p.id(),c.id(),row.getVersion());assertEquals(0,unread(a));
    }
    @Test void publicacaoOcultaNaoVazaPeloAvisoOuPeloLink(){
        var p=post(a,"Publicação");var c=comment(b,p.id(),"Texto");var row=posts.findById(p.id()).orElseThrow();row.setHiddenAt(Instant.now());posts.saveAndFlush(row);
        assertEquals(0,unread(a));status(404,()->service.buscar(community,a,p.id()));status(404,()->interactions.buscarComentario(community,a,p.id(),c.id()));
    }
    @Test void paginacaoUsaCursorSemDuplicatas(){
        var p=post(a,"Publicação");for(int i=0;i<25;i++)comment(b,p.id(),"Texto "+i);
        var x=notifications.listar(a,null,10);var y=notifications.listar(a,x.proximoCursor(),10);var z=notifications.listar(a,y.proximoCursor(),10);
        var ids=new HashSet<Long>();for(var page:List.of(x,y,z))for(var n:page.notificacoes())assertTrue(ids.add(n.id()));assertEquals(25,ids.size());assertFalse(z.temMais());
    }
    @Test void rollbackDoComentarioTambemDesfazAviso(){
        var p=post(a,"Publicação");new TransactionTemplate(transactions).execute(tx->{comment(b,p.id(),"Não confirmar");tx.setRollbackOnly();return null;});
        assertEquals(0,stored());assertEquals(0,comments.count());
    }
    @Test void exclusaoDeContaRemoveAvisosPelasFontes(){
        var p=post(a,"Publicação");comment(b,p.id(),"Texto");request();assertEquals(2,stored());users.deleteById(b);assertEquals(0,stored());
    }
    @Test void linkAbrePublicacaoEComentarioMesmoAntigos(){
        var p=post(a,"Antiga");var c=comment(b,p.id(),"Antigo");for(int i=0;i<25;i++){post(a,"Nova "+i);comment(b,p.id(),"Novo "+i);}
        assertEquals(p.id(),service.buscar(community,a,p.id()).id());assertEquals(c.id(),interactions.buscarComentario(community,a,p.id(),c.id()).id());
    }
    @Test void comentariosConcorrentesEmComunidadesDiferentesNotificamMesmoDono()throws Exception{
        var one=post(a,"Uma");var two=service.criar(otherCommunity,a,new CreateCommunityPostDTO("Outra",UUID.randomUUID()),null);communities.entrar(otherCommunity,outsider);
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{var x=pool.submit(()->{start.await();return comment(b,one.id(),"Primeira");});var y=pool.submit(()->{start.await();return interactions.comentar(otherCommunity,outsider,two.id(),new CreateCommunityCommentDTO("Segunda",UUID.randomUUID()));});start.countDown();x.get(20,TimeUnit.SECONDS);y.get(20,TimeUnit.SECONDS);assertEquals(2,unread(a));}
        finally{start.countDown();pool.shutdownNow();}
    }
    @Test void comentarioDoDonoConcorrenteComAvisoNaoBloqueiaSuaLinha()throws Exception{
        var p=post(a,"Publicação");var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{var x=pool.submit(()->{start.await();return comment(a,p.id(),"Próprio");});var y=pool.submit(()->{start.await();return comment(b,p.id(),"Outro");});start.countDown();x.get(20,TimeUnit.SECONDS);y.get(20,TimeUnit.SECONDS);assertEquals(1,unread(a));}
        finally{start.countDown();pool.shutdownNow();}
    }
}
