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
    SocialProfileEditorService editor;
    MockMvc mvc;
    final java.security.Principal principal = new UsernamePasswordAuthenticationToken("owner@example.test",null,List.of());
    @BeforeEach void setup() {
        editor=mock(SocialProfileEditorService.class);
        var users=mock(UserRepository.class);
        var owner=new User(); owner.setId(7L);
        when(users.findByEmail("owner@example.test")).thenReturn(Optional.of(owner));
        mvc=MockMvcBuilders.standaloneSetup(new SocialProfileEditorController(editor,mock(SocialHubService.class),users))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
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
        when(editor.foto(7L,9L)).thenReturn(new byte[]{1,2});
        mvc.perform(get("/api/v1/social/profiles/9/photo").principal(principal))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("X-Content-Type-Options","nosniff"));
        verify(editor).foto(7L,9L);
    }
    @Test void fotoOcultaRetorna404() throws Exception {
        when(editor.foto(7L,9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/v1/social/profiles/9/photo").principal(principal))
            .andExpect(status().isNotFound()).andExpect(header().string("Cache-Control","no-store"));
    }
}
