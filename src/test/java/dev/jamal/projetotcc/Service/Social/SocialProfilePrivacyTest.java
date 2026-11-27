package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.SocialProfileEditDTO;
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
    SocialProfilePhotoRepository photos;
    Map<Long, byte[]> fotografias;
    Map<Long, byte[]> miniaturas;

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
        photos = mock(SocialProfilePhotoRepository.class);

        fotografias = new HashMap<>();
        miniaturas = new HashMap<>();

        editor = new SocialProfileEditorService(
                profiles, users, participation, hub, blocks, photos
        );
        org.springframework.test.util.ReflectionTestUtils.setField(editor, "entityManager", mock(jakarta.persistence.EntityManager.class));
        a = profile(1L); b = profile(2L);

        when(photos.salvarOuAtualizar(
                anyLong(),
                any(byte[].class),
                any(byte[].class),
                anyString(),
                any()
        )).thenAnswer(call -> {

            Long profileId = call.getArgument(0);

            byte[] imagem = call.getArgument(1);
            byte[] miniatura = call.getArgument(2);

            fotografias.put(profileId, imagem);
            miniaturas.put(profileId, miniatura);

            return 1;
        });
        when(photos.findThumbnail(anyLong()))
                .thenAnswer(call -> {

                    Long profileId = call.getArgument(0);

                    return Optional.ofNullable(
                            miniaturas.get(profileId)
                    );
                });
        when(photos.findPhotoData(anyLong()))
                .thenAnswer(call -> {

                    Long profileId = call.getArgument(0);

                    byte[] imagem = fotografias.get(profileId);

                    if (imagem == null) {
                        return Optional.empty();
                    }

                    var data = mock(
                            SocialProfilePhotoRepository.PhotoData.class
                    );

                    when(data.getFoto()).thenReturn(imagem);

                    when(data.getContentType())
                            .thenReturn("image/jpeg");

                    return Optional.of(data);
                });
        when(photos.remover(anyLong()))
                .thenAnswer(call -> {

                    Long profileId = call.getArgument(0);

                    miniaturas.remove(profileId);

                    return fotografias.remove(profileId) != null
                            ? 1 : 0;
                });
        when(profiles.findByUser_Id(1L)).thenReturn(Optional.of(a));
        when(profiles.findByUser_Id(2L)).thenReturn(Optional.of(b));
        when(profiles.findPublicSummary(anyLong())).thenAnswer(call -> {
            SocialProfile p = call.getArgument(0).equals(1L) ? a : b;
            return p.isEnabled() ? Optional.of(new SocialProfileSummary(p.getUser().getId(), p.getUser().getNome(),
                    p.getUser().getDataNascimento(), p.isCompartilharRegiao(), p.getBiografia(), p.getFotoVisibilidade(),
                    p.getBiografiaVisibilidade(), p.getInteressesVisibilidade(), p.getHobbiesVisibilidade(),
                    fotografias.containsKey(p.getId()))) : Optional.empty();
        });
        when(profiles.existsByUser_Id(1L)).thenReturn(true);
        when(profiles.findEditableByUserId(1L)).thenReturn(Optional.of(new SocialProfileEditDTO(
                a.getBiografia(), a.getFotoVisibilidade(), a.getBiografiaVisibilidade(),
                a.getInteressesVisibilidade(), a.getHobbiesVisibilidade())));
        when(participation.elegivel(anyLong())).thenReturn(true);
        when(participation.participa(anyLong())).thenReturn(true);
        var interest = new Interest(); interest.setNome("Astronomia");
        var ui = new UserInterest(); ui.setInterest(interest);
        when(interests.findByUserIdWithInterest(anyLong())).thenReturn(List.of(ui));
        var hobby = new Hobby(); hobby.setId(10L); hobby.setNome("Xadrez");
        var uh = new UserHobby(); uh.setHobby(hobby); uh.setStatusAtual(UserHobbyStatus.PRATICANDO);
        when(hobbies.findByUser_Id(anyLong())).thenReturn(List.of(uh));

        editor.salvarFoto(a.getUser().getId(), gerarFoto());
        editor.salvarFoto(b.getUser().getId(), gerarFoto());

        clearInvocations(photos, profiles);
    }

    private SocialProfile profile(Long id) {

        var user = new User();

        user.setId(id);
        user.setNome("Pessoa " + id);
        user.setDataNascimento(
                java.time.LocalDate.of(1990, 1, 1)
        );

        var p = new SocialProfile();

        p.setId(id);
        p.setUser(user);
        p.setEnabled(true);
        p.setBiografia("Minha biografia");

        return p;
    }
    @Test void perfilEFotoExigemMaioridadeDoTitular() {
        b.getUser().setDataNascimento(java.time.LocalDate.now().minusYears(17));
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> hub.buscarPerfilPublicoPara(1L, 2L)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> editor.foto(1L, 2L)).getStatusCode().value());
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
        assertArrayEquals(
                fotografias.get(b.getId()),
                editor.foto(2L, 2L)
        );
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
        assertEquals(768,decoded.getWidth()); assertEquals(512,decoded.getHeight());
        assertEquals((byte) 0xFF, result[0]);
        assertEquals((byte) 0xD8, result[1]);
        assertEquals((byte) 0xFF, result[2]);
    }
    @Test void rejeitaArquivoFalsoEVazioEOversize() {
        for(byte[] content:List.of(new byte[0],"<svg></svg>".getBytes(),new byte[1024*1024+1]))
            assertThrows(ResponseStatusException.class,()->editor.prepararFoto(new MockMultipartFile("file","foto.png","image/png",content)));
    }
    @Test void atualizaBiografiaEPreferenciasApenasDoTitular() {
        var dto = new dev.jamal.projetotcc.DTO.Social.SocialProfileEditDTO("  Minha nova bio  ",
                SocialVisibility.SOMENTE_EU,SocialVisibility.CONTATOS,SocialVisibility.CONTATOS,SocialVisibility.SOMENTE_EU);
        when(profiles.updateTextSettings(eq(1L), eq("Minha nova bio"), eq(dto.fotoVisibilidade()),
                eq(dto.biografiaVisibilidade()), eq(dto.interessesVisibilidade()),
                eq(dto.hobbiesVisibilidade()), any())).thenReturn(1);
        var saved=editor.salvar(1L,dto);
        verify(profiles, never()).findByUser_Id(anyLong());
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
    @Test
    void trocaERemoveFotoSemAlterarBiografiaNemVisibilidade() {

        privateFields(a, SocialVisibility.SOMENTE_EU);

        byte[] novaFoto = gerarFoto();

        editor.salvarFoto(1L, novaFoto);

        assertArrayEquals(
                novaFoto,
                editor.foto(1L, 1L)
        );

        editor.removerFoto(1L);

        assertFalse(
                fotografias.containsKey(a.getId())
        );

        assertThrows(
                ResponseStatusException.class,
                () -> editor.foto(1L, 1L)
        );

        assertEquals(
                "Minha biografia",
                a.getBiografia()
        );

        assertEquals(
                SocialVisibility.SOMENTE_EU,
                a.getFotoVisibilidade()
        );

        assertNotNull(
                fotografias.get(b.getId())
        );
    }

    private byte[] gerarFoto() {

        try {
            var imagem = new BufferedImage(
                    100, 100,
                    BufferedImage.TYPE_INT_RGB
            );

            var output = new ByteArrayOutputStream();

            ImageIO.write(imagem, "jpeg", output);

            return output.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void fotoPrivadaNaoConsultaDadosBinarios() {

        privateFields(b, SocialVisibility.SOMENTE_EU);

        assertThrows(
                ResponseStatusException.class,
                () -> editor.foto(1L, 2L)
        );

        verify(photos, never())
                .findPhotoData(anyLong());
    }

    @Test
    void bloqueioImpedeConsultaDosDadosBinarios() {

        when(blocks.bloqueados(1L, 2L))
                .thenReturn(true);

        assertThrows(
                ResponseStatusException.class,
                () -> editor.foto(1L, 2L)
        );

        verify(photos, never())
                .findPhotoData(anyLong());
    }

    @Test
    void participantePodeVisualizarMiniatura() {

        assertArrayEquals(
                miniaturas.get(b.getId()),
                editor.miniatura(1L, 2L)
        );

        verify(photos).findThumbnail(b.getId());

        verify(photos, never())
                .findPhotoData(anyLong());
    }

    @Test
    void miniaturaPrivadaNaoConsultaDadosBinarios() {

        privateFields(b, SocialVisibility.SOMENTE_EU);

        assertEquals(404, assertThrows(
                ResponseStatusException.class,
                () -> editor.miniatura(1L, 2L)
        ).getStatusCode().value());

        verificarMiniaturaCondicionalNegada(404);

        verify(photos, never())
                .findThumbnail(anyLong());

        verify(photos, never())
                .findPhotoData(anyLong());
    }

    @Test
    void titularPodeVisualizarMiniaturaPrivada() {

        privateFields(b, SocialVisibility.SOMENTE_EU);

        assertArrayEquals(
                miniaturas.get(b.getId()),
                editor.miniatura(2L, 2L)
        );
    }

    @Test
    void bloqueioImpedeAcessoAMiniatura() {

        when(blocks.bloqueados(1L, 2L))
                .thenReturn(true);

        assertEquals(404, assertThrows(
                ResponseStatusException.class,
                () -> editor.miniatura(1L, 2L)
        ).getStatusCode().value());

        verificarMiniaturaCondicionalNegada(404);

        verify(photos, never())
                .findThumbnail(anyLong());
    }

    @Test
    void miniaturaExigeContatoAceito() {

        privateFields(b, SocialVisibility.CONTATOS);

        var contato = new SocialContact();

        contato.setStatus(SocialContactStatus.PENDENTE);

        when(contacts.findByLowUserIdAndHighUserId(1L, 2L))
                .thenReturn(Optional.of(contato));

        assertThrows(
                ResponseStatusException.class,
                () -> editor.miniatura(1L, 2L)
        );

        contato.setStatus(SocialContactStatus.ACEITA);

        assertArrayEquals(
                miniaturas.get(b.getId()),
                editor.miniatura(1L, 2L)
        );

        contato.setStatus(SocialContactStatus.ENCERRADA);
        verificarMiniaturaCondicionalNegada(404);

        assertThrows(
                ResponseStatusException.class,
                () -> editor.miniatura(1L, 2L)
        );
    }

    @Test
    void miniaturaExigeMaioridadeDoTitular() {

        b.getUser().setDataNascimento(
                java.time.LocalDate.now().minusYears(17)
        );

        assertEquals(404, assertThrows(
                ResponseStatusException.class,
                () -> editor.miniatura(1L, 2L)
        ).getStatusCode().value());

        verificarMiniaturaCondicionalNegada(404);

        verify(photos, never())
                .findThumbnail(anyLong());
    }

    @Test
    void miniaturaExigeParticipacaoSocial() {

        when(participation.participa(1L))
                .thenReturn(false);

        assertEquals(403, assertThrows(
                ResponseStatusException.class,
                () -> editor.miniatura(1L, 2L)
        ).getStatusCode().value());

        verificarMiniaturaCondicionalNegada(403);

        verify(photos, never())
                .findThumbnail(anyLong());
    }

    @Test
    void perfilDesativadoNaoDisponibilizaMiniatura() {

        b.setEnabled(false);

        assertEquals(404, assertThrows(
                ResponseStatusException.class,
                () -> editor.miniatura(1L, 2L)
        ).getStatusCode().value());

        verificarMiniaturaCondicionalNegada(404);

        verify(photos, never())
                .findThumbnail(anyLong());
    }
    private void verificarMiniaturaCondicionalNegada(int status) {
        assertEquals(status, assertThrows(ResponseStatusException.class,
                () -> editor.miniaturaCondicional(1L, 2L, "a".repeat(64)))
                .getStatusCode().value());
        verify(photos, never()).findThumbnailConditional(anyLong(), anyString());
        verify(photos, never()).findPhotoData(anyLong());
    }
}
