package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.*;
import dev.jamal.projetotcc.Repository.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocialProfilePrivacyTest {
    SocialProfileRepository profiles;
    SocialContactRepository contacts;
    SocialUserBlockRepository blocks;
    SocialHubService hub;
    SocialProfileEditorService editor;
    SocialProfileService participation;
    UserRepository users;
    SocialProfile a, b;

    @BeforeEach void setup() {
        profiles = mock(SocialProfileRepository.class);
        contacts = mock(SocialContactRepository.class);
        blocks = mock(SocialUserBlockRepository.class);
        var interests = mock(UserInterestRepository.class);
        var hobbies = mock(UserHobbyRepository.class);
        var feedback = mock(UserRecommendationFeedbackRepository.class);
        var location = mock(SocialLocationService.class);
        hub = new SocialHubService(mock(UserRepository.class), profiles, interests, hobbies, feedback,
                new SocialProfileResumeService(), new SocialAffinityService(), location, blocks, contacts);
        participation = mock(SocialProfileService.class);
        users = mock(UserRepository.class);
        when(users.lockTokenVersion(anyLong())).thenReturn(Optional.of(0L));
        editor = new SocialProfileEditorService(profiles, users, participation, hub, blocks);
        org.springframework.test.util.ReflectionTestUtils.setField(editor, "entityManager", mock(jakarta.persistence.EntityManager.class));
        a = profile(1L); b = profile(2L);
        when(profiles.findByUser_Id(1L)).thenReturn(Optional.of(a));
        when(profiles.findByUser_Id(2L)).thenReturn(Optional.of(b));
        when(participation.elegivel(anyLong())).thenReturn(true);
        when(participation.participa(anyLong())).thenReturn(true);
        var interest = new Interest(); interest.setNome("Astronomia");
        var ui = new UserInterest(); ui.setInterest(interest);
        when(interests.findByUserIdWithInterest(anyLong())).thenReturn(List.of(ui));
        var hobby = new Hobby(); hobby.setId(10L); hobby.setNome("Xadrez");
        var uh = new UserHobby(); uh.setHobby(hobby); uh.setStatusAtual(UserHobbyStatus.PRATICANDO);
        when(hobbies.findByUser_Id(anyLong())).thenReturn(List.of(uh));
    }

    private SocialProfile profile(Long id) {
        var user = new User(); user.setId(id); user.setNome("Pessoa " + id);
        var p = new SocialProfile(); p.setUser(user); p.setEnabled(true);
        p.setBiografia("Minha biografia"); p.setFoto(new byte[]{1,2,3}); return p;
    }
    private void privateFields(SocialProfile p, SocialVisibility v) {
        p.setFotoVisibilidade(v); p.setBiografiaVisibilidade(v);
        p.setInteressesVisibilidade(v); p.setHobbiesVisibilidade(v);
    }
    @Test void participantesVeemCamposEAfinidade() {
        var dto = hub.buscarPerfilPublicoPara(1L,2L);
        assertEquals("Minha biografia",dto.biografia()); assertTrue(dto.possuiFoto());
        assertEquals(List.of("Astronomia"),dto.interesses());
        assertTrue(dto.resumo().contains("Xadrez")); assertTrue(dto.afinidade().score()>0);
    }
    @Test void somenteEuOcultaListasResumoFotoEAfinidade() {
        privateFields(b,SocialVisibility.SOMENTE_EU);
        var dto = hub.buscarPerfilPublicoPara(1L,2L);
        assertNull(dto.biografia()); assertFalse(dto.possuiFoto()); assertTrue(dto.interesses().isEmpty());
        assertTrue(dto.praticando().isEmpty()); assertFalse(dto.resumo().contains("Astronomia"));
        assertFalse(dto.resumo().contains("Xadrez")); assertEquals(0,dto.afinidade().score());
        assertThrows(ResponseStatusException.class,()->editor.foto(1L,2L));
    }
    @Test void titularVeCamposPrivados() {
        privateFields(b,SocialVisibility.SOMENTE_EU);
        var dto=hub.buscarPerfilPublicoPara(2L,2L);
        assertTrue(dto.possuiFoto()); assertNotNull(dto.biografia()); assertFalse(dto.praticando().isEmpty());
        assertArrayEquals(b.getFoto(),editor.foto(2L,2L));
    }
    @Test void contatosPrecisamDeAceite() {
        privateFields(b,SocialVisibility.CONTATOS);
        var contact=new SocialContact(); contact.setStatus(SocialContactStatus.PENDENTE);
        when(contacts.findByLowUserIdAndHighUserId(1L,2L)).thenReturn(Optional.of(contact));
        assertFalse(hub.buscarPerfilPublicoPara(1L,2L).possuiFoto());
        contact.setStatus(SocialContactStatus.ACEITA);
        assertTrue(hub.buscarPerfilPublicoPara(1L,2L).possuiFoto());
        contact.setStatus(SocialContactStatus.ENCERRADA);
        assertNull(hub.buscarPerfilPublicoPara(1L,2L).biografia());
    }
    @Test void preferenciasDoVisitanteNaoVazamNaAfinidade() {
        privateFields(a,SocialVisibility.SOMENTE_EU);
        assertEquals(0,hub.buscarPerfilPublicoPara(1L,2L).afinidade().score());
    }
    @Test void bloqueioImpedePerfilEFotoMesmoComAceite() {
        when(blocks.bloqueados(1L,2L)).thenReturn(true);
        assertThrows(ResponseStatusException.class,()->hub.buscarPerfilPublicoPara(1L,2L));
        assertThrows(ResponseStatusException.class,()->editor.foto(1L,2L));
    }
    @Test void desativacaoImpedeAcessoAosDadosEFoto() {
        when(participation.participa(1L)).thenReturn(false);
        assertThrows(ResponseStatusException.class,()->editor.buscar(1L));
        assertThrows(ResponseStatusException.class,()->editor.foto(1L,2L));
        b.setEnabled(false);
        assertThrows(RuntimeException.class,()->hub.buscarPerfilPublicoPara(2L,2L));
    }
    @Test void fotoEReprocessadaEReduzida() throws Exception {
        var original=new BufferedImage(900,600,BufferedImage.TYPE_INT_RGB);
        var bytes=new ByteArrayOutputStream(); ImageIO.write(original,"jpg",bytes);
        var result=editor.prepararFoto(new MockMultipartFile("file","foto.jpg","image/jpeg",bytes.toByteArray()));
        var decoded=ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(256,decoded.getWidth()); assertTrue(decoded.getHeight()<=256);
        assertEquals((byte)0x89,result[0]);
    }
    @Test void rejeitaArquivoFalsoEVazioEOversize() {
        for(byte[] content:List.of(new byte[0],"<svg></svg>".getBytes(),new byte[1024*1024+1]))
            assertThrows(ResponseStatusException.class,()->editor.prepararFoto(new MockMultipartFile("file","foto.png","image/png",content)));
    }
    @Test void atualizaBiografiaEPreferenciasApenasDoTitular() {
        var dto = new dev.jamal.projetotcc.DTO.Social.SocialProfileEditDTO("  Minha nova bio  ",
                SocialVisibility.SOMENTE_EU,SocialVisibility.CONTATOS,SocialVisibility.CONTATOS,SocialVisibility.SOMENTE_EU);
        var saved=editor.salvar(1L,dto);
        assertEquals("Minha nova bio",saved.biografia()); assertEquals(SocialVisibility.SOMENTE_EU,saved.fotoVisibilidade());
        assertEquals("Minha biografia",b.getBiografia()); assertEquals(SocialVisibility.PARTICIPANTES,b.getFotoVisibilidade());
    }
    @Test void rejeitaBioLongaEVisibilidadeAusente() {
        var longBio = new dev.jamal.projetotcc.DTO.Social.SocialProfileEditDTO("x".repeat(501),
            SocialVisibility.PARTICIPANTES,SocialVisibility.PARTICIPANTES,SocialVisibility.PARTICIPANTES,SocialVisibility.PARTICIPANTES);
        assertThrows(ResponseStatusException.class,()->editor.salvar(1L,longBio));
        var missing = new dev.jamal.projetotcc.DTO.Social.SocialProfileEditDTO("bio",null,null,null,null);
        assertThrows(ResponseStatusException.class,()->editor.salvar(1L,missing));
        assertEquals("Minha biografia",a.getBiografia());
    }
    @Test void trocaERemoveFotoSemAlterarBiografiaNemVisibilidade() {
        privateFields(a,SocialVisibility.SOMENTE_EU);
        editor.salvarFoto(1L,new byte[]{9}); assertArrayEquals(new byte[]{9},editor.foto(1L,1L));
        editor.removerFoto(1L); assertNull(a.getFoto());
        assertEquals("Minha biografia",a.getBiografia()); assertEquals(SocialVisibility.SOMENTE_EU,a.getFotoVisibilidade());
        assertNotNull(b.getFoto());
    }
}
