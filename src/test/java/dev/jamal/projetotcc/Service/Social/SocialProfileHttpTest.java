package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.Controllers.SocialProfileEditorController;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Exception.GlobalExceptionHandler;
import dev.jamal.projetotcc.Repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SocialProfileHttpTest {
    private static final String HASH = "a".repeat(64);
    private static final String TAG = "\"" + HASH + "\"";
    SocialProfileEditorService editor;
    MockMvc mvc;
    final java.security.Principal principal = new UsernamePasswordAuthenticationToken("owner@example.test",null,List.of());
    @BeforeEach void setup() {
        editor=mock(SocialProfileEditorService.class);
        var users=mock(UserRepository.class);
        var owner=new User(); owner.setId(7L);
        when(users.findByEmail("owner@example.test")).thenReturn(Optional.of(owner));
        mvc=MockMvcBuilders.standaloneSetup(new SocialProfileEditorController(editor,mock(SocialHubService.class),users))
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(corsFilter()).build();
    }
    private org.springframework.web.filter.CorsFilter corsFilter() {
        var config = new dev.jamal.projetotcc.config.SecurityConfig(null, null, null);
        org.springframework.test.util.ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:5173");
        return new org.springframework.web.filter.CorsFilter(config.corsConfigurationSource());
    }

    @Test void corsAceitaPreflightCondicionalSomenteDaOrigemConfigurada() throws Exception {
        for (String origem : List.of("http://localhost:5173", "https://other.example")) {
            var resultado = mvc.perform(options("/api/v1/social/profiles/9/thumbnail")
                    .header("Origin", origem)
                    .header("Access-Control-Request-Method", "GET")
                    .header("Access-Control-Request-Headers", "authorization,if-none-match"));
            if (origem.contains("localhost")) {
                resultado.andExpect(status().isOk())
                        .andExpect(header().string("Access-Control-Allow-Origin", origem))
                        .andExpect(header().string("Access-Control-Allow-Headers", "authorization, if-none-match"));
            } else {
                resultado.andExpect(status().isForbidden());
            }
        }
        verifyNoInteractions(editor);
    }

    @Test void edicaoUsaUsuarioAutenticado() throws Exception {
        mvc.perform(put("/api/v1/social/me/profile").principal(principal).contentType(MediaType.APPLICATION_JSON).content("""
            {"biografia":"Bio","fotoVisibilidade":"CONTATOS","biografiaVisibilidade":"SOMENTE_EU",
             "interessesVisibilidade":"PARTICIPANTES","hobbiesVisibilidade":"CONTATOS"}
            """)).andExpect(status().isOk());
        verify(editor).salvar(eq(7L),argThat(dto->dto.biografia().equals("Bio")));
    }
    @Test void validaPreferenciasObrigatorias() throws Exception {
        mvc.perform(put("/api/v1/social/me/profile").principal(principal).contentType(MediaType.APPLICATION_JSON)
            .content("{\"biografia\":\"Bio\"}")).andExpect(status().isBadRequest());
        verify(editor,never()).salvar(anyLong(),any());
    }
    @Test void fotoEAutenticadaENaoPermiteCache() throws Exception {
        when(editor.fotoCondicional(7L, 9L, "")).thenReturn(new SocialProfileEditorService.FotoCondicional("photo-" + HASH, new byte[]{1,2}));
        mvc.perform(get("/api/v1/social/profiles/9/photo").principal(principal))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("X-Content-Type-Options","nosniff"));
        verify(editor).fotoCondicional(7L, 9L, "");
    }
    @Test void fotoComVersaoAutorizadaRetorna304SemCorpo() throws Exception {
        String versao = "photo-" + HASH;
        when(editor.fotoCondicional(7L, 9L, HASH + "," + versao))
                .thenReturn(new SocialProfileEditorService.FotoCondicional(versao, null));
        mvc.perform(get("/api/v1/social/profiles/9/photo").principal(principal)
                        .header("Origin", "http://localhost:5173")
                        .header("If-None-Match", TAG + ", W/\"" + versao + "\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", "\"" + versao + "\""))
                .andExpect(header().string("Access-Control-Expose-Headers", "ETag"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(new byte[0]));
    }

    @Test void fotoOcultaRetorna404() throws Exception {
        when(editor.fotoCondicional(7L, 9L, "photo-" + HASH)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/v1/social/profiles/9/photo").principal(principal).header("If-None-Match", "\"photo-" + HASH + "\""))
                        .andExpect(status().isNotFound()).andExpect(header().string("Cache-Control","no-store"));
    }
    @Test
    void fotoJpegRetornaContentTypeCorreto() throws Exception {

        byte[] jpeg = {
                (byte) 0xFF,
                (byte) 0xD8,
                (byte) 0xFF
        };

        when(editor.fotoCondicional(7L, 9L, "")).thenReturn(new SocialProfileEditorService.FotoCondicional("photo-" + HASH, jpeg));

        mvc.perform(
                        get("/api/v1/social/profiles/9/photo")
                                .principal(principal)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(jpeg))
                .andExpect(header().string(
                        "Cache-Control", "no-store"
                ))
                .andExpect(header().string(
                        "X-Content-Type-Options", "nosniff"
                ));

        verify(editor).fotoCondicional(7L, 9L, "");
    }

    @Test
    void miniaturaRetornaJpegENaoPermiteCache()
            throws Exception {

        byte[] jpeg = {
                (byte) 0xFF,
                (byte) 0xD8,
                (byte) 0xFF
        };

        when(editor.miniaturaCondicional(7L, 9L, ""))
                .thenReturn(new SocialProfileEditorService.MiniaturaCondicional(HASH, jpeg));

        mvc.perform(
                        get("/api/v1/social/profiles/9/thumbnail")
                                .principal(principal)
                )
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", TAG))
                .andExpect(content().contentType(
                        MediaType.IMAGE_JPEG
                ))
                .andExpect(content().bytes(jpeg))
                .andExpect(header().string(
                        "Cache-Control", "no-store"
                ))
                .andExpect(header().string(
                        "X-Content-Type-Options", "nosniff"
                ));

        verify(editor).miniaturaCondicional(7L, 9L, "");

        verify(editor, never())
                .foto(anyLong(), anyLong());
    }

    @Test
    void miniaturaComVersaoCorrespondenteRetorna304SemCorpo() throws Exception {
        String antiga = "b".repeat(64);
        for (String versoes : List.of(antiga + "," + HASH, "*")) {
            when(editor.miniaturaCondicional(7L, 9L, versoes))
                    .thenReturn(new SocialProfileEditorService.MiniaturaCondicional(HASH, null));
        }
        for (String condicao : List.of("\"" + antiga + "\", W/" + TAG, "*")) {
            mvc.perform(get("/api/v1/social/profiles/9/thumbnail").principal(principal)
                            .header("Origin", "http://localhost:5173")
                            .header("If-None-Match", condicao))
                    .andExpect(status().isNotModified())
                    .andExpect(header().string("Access-Control-Expose-Headers", "ETag"))
                    .andExpect(header().string("ETag", TAG))
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(content().bytes(new byte[0]));
        }
        verify(editor, never()).foto(anyLong(), anyLong());
    }

    @Test
    void miniaturaComVersaoAntigaRetornaImagemAtual() throws Exception {
        String antiga = "b".repeat(64);
        byte[] jpeg = {1, 2};
        when(editor.miniaturaCondicional(7L, 9L, antiga))
                .thenReturn(new SocialProfileEditorService.MiniaturaCondicional(HASH, jpeg));
        mvc.perform(get("/api/v1/social/profiles/9/thumbnail").principal(principal)
                        .header("If-None-Match", "\"" + antiga + "\""))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", TAG))
                .andExpect(content().bytes(jpeg));
    }

    @Test
    void miniaturaIndisponivelRetorna404()
            throws Exception {

        when(editor.miniaturaCondicional(7L, 9L, HASH))
                .thenThrow(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND
                        )
                );

        mvc.perform(
                        get("/api/v1/social/profiles/9/thumbnail")
                                .principal(principal)
                                .header("If-None-Match", TAG)
                )
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("ETag"))
                .andExpect(header().string(
                        "Cache-Control", "no-store"
                ));

        verify(editor, never())
                .foto(anyLong(), anyLong());
    }


}
