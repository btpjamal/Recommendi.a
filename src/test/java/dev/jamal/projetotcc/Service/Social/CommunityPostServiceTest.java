package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Pageable;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommunityPostServiceTest {
    CommunityPostRepository posts;
    CommunityPostImageRepository images;
    CommunityRepository communities;
    CommunityMemberRepository members;
    UserRepository users;
    SocialProfileRepository profiles;
    SocialUserBlockRepository blocks;
    CommunityChatLockRepository locks;
    CommunityPostService service;
    User user,other;
    SocialProfile social;
    Community community;
    CommunityPost stored;

    @BeforeEach void setup() {
        posts=mock(CommunityPostRepository.class);images=mock(CommunityPostImageRepository.class);
        communities=mock(CommunityRepository.class);members=mock(CommunityMemberRepository.class);
        users=mock(UserRepository.class);profiles=mock(SocialProfileRepository.class);
        blocks=mock(SocialUserBlockRepository.class);locks=mock(CommunityChatLockRepository.class);
        service=new CommunityPostService(posts,images,communities,members,users,profiles,blocks,locks);
        var manager = mock(jakarta.persistence.EntityManager.class);
        var quotaQuery = mock(jakarta.persistence.Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(quotaQuery);
        when(quotaQuery.setParameter(anyString(), any())).thenReturn(quotaQuery);
        when(quotaQuery.getResultList()).thenReturn(List.of(1));
        ReflectionTestUtils.setField(service, "entityManager", manager);
        user=new User();user.setId(1L);user.setNome("Autora");user.setDataNascimento(LocalDate.of(1990,1,1));
        other=new User();other.setId(2L);other.setNome("Outro");other.setDataNascimento(LocalDate.of(1990,1,1));
        social=new SocialProfile();social.setUser(user);social.setEnabled(true);
        community=new Community();community.setId(10L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(profiles.findByUser_Id(1L)).thenReturn(Optional.of(social));
        when(members.existsByCommunity_IdAndUser_Id(10L,1L)).thenReturn(true);
        when(locks.lockUser(1L)).thenReturn(Optional.of(1L));when(locks.lockCommunity(10L)).thenReturn(Optional.of(10L));
        when(communities.getReferenceById(10L)).thenReturn(community);
        when(posts.saveAndFlush(any())).thenAnswer(call->{
            CommunityPost p=call.getArgument(0);p.setId(50L);p.setVersion(p.getVersion()==null?0L:p.getVersion()+1);stored=p;return p;
        });
    }
    @Test
    void cotaDeImagemBloqueiaNovaGravacaoSemImpedirPostDeTexto() {
        when(images.bytesUsed(1L, 0L)).thenReturn(100L * 1024 * 1024);

        var rejected = assertThrows(ResponseStatusException.class,
                () -> service.criar(10L, 1L, request("Com foto"), new byte[]{1}));
        assertEquals(413, rejected.getStatusCode().value());
        verify(posts, never()).saveAndFlush(any());

        assertFalse(service.criar(10L, 1L, request("Só texto"), null).possuiImagem());
    }

    CreateCommunityPostDTO request(String text) {return new CreateCommunityPostDTO(text,UUID.randomUUID());}
    CommunityPost ownPost(boolean image) {
        var p=new CommunityPost();p.setId(50L);p.setCommunity(community);p.setUser(user);p.setConteudo("Inicial");
        p.setPossuiImagem(image);p.setVersion(0L);p.setCreatedAt(Instant.now());
        when(posts.findByIdAndCommunity_Id(50L,10L)).thenReturn(Optional.of(p));return p;
    }
    void status(int code,org.junit.jupiter.api.function.Executable f) {
        assertEquals(code,assertThrows(ResponseStatusException.class,f).getStatusCode().value());
    }
    @Test void textoEImagemSaoOpcionaisMasNaoPodemFaltarJuntos() {
        status(400,()->service.criar(10L,1L,request("   "),null));
        assertEquals("Só texto",service.criar(10L,1L,request("  Só texto  "),null).conteudo());
        assertTrue(service.criar(10L,1L,request(null),new byte[]{1}).possuiImagem());
        verify(images).save(argThat(p->Arrays.equals(new byte[]{1},p.getDados())));
    }
    @Test void desativadoMenorENaoMembroNaoPodemAcessar() {
        social.setEnabled(false);status(403,()->service.listar(10L,1L,null,20));
        status(403,()->service.criar(10L,1L,request("Texto"),null));social.setEnabled(true);
        user.setDataNascimento(LocalDate.now().minusYears(17));status(403,()->service.listar(10L,1L,null,20));
        user.setDataNascimento(LocalDate.of(1990,1,1));when(members.existsByCommunity_IdAndUser_Id(10L,1L)).thenReturn(false);
        status(403,()->service.listar(10L,1L,null,20));
        status(403,()->service.imagem(10L,1L,50L));
    }
    @Test void repeticaoDoMesmoEnvioNaoDuplica() {
        var request=request("Texto");var first=service.criar(10L,1L,request,null);
        when(posts.findByUser_IdAndClientId(1L,request.clientId())).thenReturn(Optional.of(stored));
        assertEquals(first.id(),service.criar(10L,1L,request,null).id());verify(posts,times(1)).saveAndFlush(any());
        status(409,()->service.criar(10L,1L,new CreateCommunityPostDTO("Outro",request.clientId()),null));
    }
    @Test void imagemDiferenteComMesmoIdDeEnvioEConflito() {
        var request=request("Texto");service.criar(10L,1L,request,new byte[]{1});
        when(posts.findByUser_IdAndClientId(1L,request.clientId())).thenReturn(Optional.of(stored));
        status(409,()->service.criar(10L,1L,request,new byte[]{2}));
    }
    @Test void publicacaoExcluidaNaoRenascePorReenvio() {
        var request=request("Texto");service.criar(10L,1L,request,null);stored.setDeletedAt(Instant.now());
        when(posts.findByUser_IdAndClientId(1L,request.clientId())).thenReturn(Optional.of(stored));
        status(409,()->service.criar(10L,1L,request,null));
    }
    @Test void apenasAutorPodeEditarOuExcluir() {
        var p=ownPost(false);p.setUser(other);
        status(404,()->service.editar(10L,1L,50L,new EditCommunityPostDTO("Invasão",0L,false),null));
        status(404,()->service.excluir(10L,1L,50L,0L));verify(posts,never()).saveAndFlush(any());
    }
    @Test void versaoAntigaNaoSobrescreveNemExclui() {
        var p=ownPost(false);p.setVersion(3L);
        status(409,()->service.editar(10L,1L,50L,new EditCommunityPostDTO("Outro",2L,false),null));
        status(409,()->service.excluir(10L,1L,50L,2L));assertEquals("Inicial",p.getConteudo());
    }
    @Test void edicaoPreservaImagemOuPermiteSubstituirERemover() {
        var p=ownPost(true);
        assertTrue(service.editar(10L,1L,50L,new EditCommunityPostDTO("Novo",0L,false),null).possuiImagem());
        assertTrue(service.editar(10L,1L,50L,new EditCommunityPostDTO("Novo",1L,false),new byte[]{9}).possuiImagem());
        assertFalse(service.editar(10L,1L,50L,new EditCommunityPostDTO("Novo",2L,true),null).possuiImagem());
        verify(images).deleteById(50L);assertNotNull(p.getEditedAt());
    }
    @Test void removerUnicoConteudoOuCombinarRemocaoComSubstituicaoEInvalido() {
        ownPost(true);
        status(400,()->service.editar(10L,1L,50L,new EditCommunityPostDTO("",0L,true),null));
        status(400,()->service.editar(10L,1L,50L,new EditCommunityPostDTO("Texto",0L,true),new byte[]{9}));
    }
    @Test void exclusaoApagaTextoEImagemMasMantemIdentidadeDeEnvio() {
        var p=ownPost(true);p.setClientId(UUID.randomUUID());var id=p.getClientId();
        service.excluir(10L,1L,50L,0L);
        assertEquals("",p.getConteudo());assertFalse(p.isPossuiImagem());assertNotNull(p.getDeletedAt());assertEquals(id,p.getClientId());
        verify(images).deleteById(50L);
    }
    @Test void imagemTambemRespeitaBloqueioEDesativacaoDoAutor() {
        var p=ownPost(true);p.setUser(other);
        var otherSocial=new SocialProfile();otherSocial.setEnabled(true);
        when(profiles.findByUser_Id(2L)).thenReturn(Optional.of(otherSocial));
        when(blocks.bloqueados(1L,2L)).thenReturn(true);status(404,()->service.imagem(10L,1L,50L));
        when(blocks.bloqueados(1L,2L)).thenReturn(false);otherSocial.setEnabled(false);status(404,()->service.imagem(10L,1L,50L));
        otherSocial.setEnabled(true);var image=new CommunityPostImage();image.setPostId(50L);image.setDados(new byte[]{9});
        when(images.findById(50L)).thenReturn(Optional.of(image));assertArrayEquals(new byte[]{9},service.imagem(10L,1L,50L));
    }
    @Test void paginaCalculaCursorNoUltimoItemEntregue() {
        var first=ownPost(false);var next=new CommunityPost();next.setId(49L);next.setCommunity(community);next.setUser(user);
        when(posts.buscarVisiveis(eq(10L),eq(1L),eq(Long.MAX_VALUE),any(),any(Pageable.class))).thenReturn(List.of(first,next));
        var page=service.listar(10L,1L,null,1);assertEquals(1,page.publicacoes().size());assertTrue(page.temMais());assertEquals(50L,page.proximoCursor());
    }
    @Test void rejeitaTextoLongoEParametrosDePaginacaoInvalidos() {
        status(400,()->service.criar(10L,1L,request("x".repeat(4001)),null));
        status(400,()->service.listar(10L,1L,0L,20));status(400,()->service.listar(10L,1L,null,31));
    }
}
