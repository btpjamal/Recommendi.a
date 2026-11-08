package dev.jamal.projetotcc.Service.Image;

import dev.jamal.projetotcc.Entities.HobbyImageUpload;
import dev.jamal.projetotcc.Enum.HobbyImageStatus;
import dev.jamal.projetotcc.Repository.HobbyImageUploadRepository;
import dev.jamal.projetotcc.Repository.HobbyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HobbyImageStatusServiceTest {

    private static final String PUBLIC_ID = "recommendia/hobbies/teste";
    private static final Instant LIMITE =
            Instant.parse("2026-11-08T12:00:00Z");

    private HobbyImageStatusService service;
    private HobbyImageUpload imagem;

    @BeforeEach
    void preparar() {
        var repository = mock(HobbyImageUploadRepository.class);
        var hobbyRepository = mock(HobbyRepository.class);
        service = new HobbyImageStatusService(repository, hobbyRepository);

        imagem = new HobbyImageUpload();
        imagem.setPublicId(PUBLIC_ID);
        imagem.setImagemUrl("https://example.com/teste.jpg");
        imagem.setStatus(HobbyImageStatus.EXCLUINDO);
        imagem.setExclusaoToken("token-antigo");
        imagem.setExclusaoIniciadaEm(LIMITE.minus(1, ChronoUnit.MINUTES));

        when(repository.findByPublicIdForUpdate(PUBLIC_ID))
                .thenReturn(Optional.of(imagem));
    }

    @Test
    void recuperaReservaExpiradaELimpaSeusDados() {
        assertTrue(service.recuperarReservaExpirada(PUBLIC_ID, LIMITE));
        assertEquals(HobbyImageStatus.AGUARDANDO_EXCLUSAO, imagem.getStatus());
        assertNull(imagem.getExclusaoToken());
        assertNull(imagem.getExclusaoIniciadaEm());
    }

    @Test
    void preservaReservaRecente() {
        var inicio = LIMITE.plus(1, ChronoUnit.MINUTES);
        imagem.setExclusaoIniciadaEm(inicio);

        assertFalse(service.recuperarReservaExpirada(PUBLIC_ID, LIMITE));
        assertEquals(HobbyImageStatus.EXCLUINDO, imagem.getStatus());
        assertEquals("token-antigo", imagem.getExclusaoToken());
        assertEquals(inicio, imagem.getExclusaoIniciadaEm());
    }

    @Test
    void preservaReservaQueComecouExatamenteNoLimite() {
        imagem.setExclusaoIniciadaEm(LIMITE);

        assertFalse(service.recuperarReservaExpirada(PUBLIC_ID, LIMITE));
        assertEquals(HobbyImageStatus.EXCLUINDO, imagem.getStatus());
        assertEquals("token-antigo", imagem.getExclusaoToken());
        assertEquals(LIMITE, imagem.getExclusaoIniciadaEm());
    }

    @Test
    void recuperaReservaLegadaSemDataNemToken() {
        imagem.setExclusaoIniciadaEm(null);
        imagem.setExclusaoToken(null);

        assertTrue(service.recuperarReservaExpirada(PUBLIC_ID, LIMITE));
        assertEquals(HobbyImageStatus.AGUARDANDO_EXCLUSAO, imagem.getStatus());
        assertNull(imagem.getExclusaoToken());
        assertNull(imagem.getExclusaoIniciadaEm());
    }

    @Test
    void naoRecuperaImagemQueJaFoiFinalizada() {
        imagem.setStatus(HobbyImageStatus.SUBSTITUIDA);
        imagem.setExclusaoToken(null);
        imagem.setExclusaoIniciadaEm(null);

        assertFalse(service.recuperarReservaExpirada(PUBLIC_ID, LIMITE));
        assertEquals(HobbyImageStatus.SUBSTITUIDA, imagem.getStatus());
    }

    @Test
    void tentativaAntigaNaoPodeFinalizarNemLiberarNovaReserva() {
        assertTrue(service.recuperarReservaExpirada(PUBLIC_ID, LIMITE));
        String novoToken = service.reservarParaExclusao(PUBLIC_ID);
        assertNotNull(novoToken);
        assertNotEquals("token-antigo", novoToken);
        var novoInicio = imagem.getExclusaoIniciadaEm();

        assertThrows(IllegalStateException.class,
                () -> service.marcarComoSubstituida(PUBLIC_ID, "token-antigo"));
        service.liberarReservaParaNovaTentativa(PUBLIC_ID, "token-antigo");

        assertEquals(HobbyImageStatus.EXCLUINDO, imagem.getStatus());
        assertEquals(novoToken, imagem.getExclusaoToken());
        assertEquals(novoInicio, imagem.getExclusaoIniciadaEm());

        service.marcarComoSubstituida(PUBLIC_ID, novoToken);
        assertEquals(HobbyImageStatus.SUBSTITUIDA, imagem.getStatus());
        assertNull(imagem.getExclusaoToken());
        assertNull(imagem.getExclusaoIniciadaEm());
    }
}
