package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.Social.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
public class SocialProfileEditorController {
    private final SocialProfileEditorService editor;
    private final SocialHubService hub;
    private final UserRepository users;
    private Long userId(Authentication auth) {
        return users.findByEmail(auth.getName()).orElseThrow().getId();
    }
    @GetMapping("/me/profile")
    public SocialProfileEditDTO me(Authentication auth) { return editor.buscar(userId(auth)); }
    @PutMapping("/me/profile")
    public SocialProfileEditDTO save(Authentication auth, @Valid @RequestBody SocialProfileEditDTO dto) {
        return editor.salvar(userId(auth), dto);
    }
    @GetMapping("/me/profile/preview")
    public SocialProfileDTO preview(Authentication auth) {
        Long id = userId(auth); editor.validar(id);
        return hub.buscarPerfilPublicoPara(id, id);
    }
    @PutMapping(value="/me/profile/photo", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(Authentication auth, @RequestParam("file") MultipartFile file) {
        Long id = userId(auth); editor.validar(id);
        byte[] png = editor.prepararFoto(file);
        editor.salvarFoto(id, png);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/me/profile/photo")
    public ResponseEntity<Void> remove(Authentication auth) {
        editor.removerFoto(userId(auth)); return ResponseEntity.noContent().build();
    }
    @GetMapping("/profiles/{owner}/photo")
    public ResponseEntity<byte[]> photo(Authentication auth, @PathVariable Long owner) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .body(editor.foto(userId(auth), owner));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<java.util.Map<String, String>> status(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(java.util.Map.of("error", ex.getReason() == null ? "Perfil ou foto indisponível." : ex.getReason()));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<java.util.Map<String, String>> tamanho() {
        return ResponseEntity.status(413).body(java.util.Map.of("error", "Envie uma foto PNG ou JPEG de até 1 MB."));
    }
}
