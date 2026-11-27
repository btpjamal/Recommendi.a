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
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String email) {
            return users.findByEmail(dev.jamal.projetotcc.Service.CanonicalEmail.normalize(email)).orElseThrow().getId();
        }
        return AuthenticatedUser.id(auth);
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
        byte[] imagem = editor.prepararFoto(file);
        editor.salvarFoto(id, imagem);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/me/profile/photo")
    public ResponseEntity<Void> remove(Authentication auth) {
        editor.removerFoto(userId(auth)); return ResponseEntity.noContent().build();
    }
    @GetMapping("/profiles/{owner}/photo")
    public ResponseEntity<byte[]> photo(
            Authentication auth,
            @PathVariable Long owner,
            @RequestHeader HttpHeaders headers
    ) {
        var resultado = editor.fotoCondicional(userId(auth), owner, extrairVersoes(headers));
        byte[] imagem = resultado.imagem();
        if (imagem == null) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(resultado.versao())
                    .cacheControl(CacheControl.noStore())
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        }

        boolean jpeg = imagem.length >= 3
                && (imagem[0] & 0xFF) == 0xFF
                && (imagem[1] & 0xFF) == 0xD8
                && (imagem[2] & 0xFF) == 0xFF;

        MediaType tipo = jpeg
                ? MediaType.IMAGE_JPEG
                : MediaType.IMAGE_PNG;

        return ResponseEntity.ok()
                .eTag(resultado.versao())
                .contentType(tipo)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(imagem);
    }

    private String extrairVersoes(HttpHeaders headers) {
        String valor = String.join(",", headers.getOrEmpty(HttpHeaders.IF_NONE_MATCH));
        if (valor.strip().equals("*")) {
            return "*";
        }
        return ETag.parse(valor).stream()
                .map(ETag::tag)
                .filter(tag -> tag.matches("(?:photo-)?[a-f0-9]{64}"))
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
    }

    @GetMapping("/profiles/{owner}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(
            Authentication auth,
            @PathVariable Long owner,
            @RequestHeader HttpHeaders headers
    ) {
        String versaoCliente = extrairVersoes(headers);

        var resultado = editor.miniaturaCondicional(
                userId(auth),
                owner,
                versaoCliente
        );

        if (resultado.naoModificada()) {

            return ResponseEntity
                    .status(HttpStatus.NOT_MODIFIED)
                    .eTag(resultado.versao())
                    .cacheControl(CacheControl.noStore())
                    .header(
                            "X-Content-Type-Options",
                            "nosniff"
                    )
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .eTag(resultado.versao())
                .cacheControl(CacheControl.noStore())
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .body(resultado.imagem());
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
