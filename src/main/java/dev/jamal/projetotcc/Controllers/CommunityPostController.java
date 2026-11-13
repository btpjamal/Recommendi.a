package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.Social.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController @RequestMapping("/api/v1/community/communities/{communityId}/posts") @RequiredArgsConstructor
public class CommunityPostController {
    private final CommunityPostService service;
    private final CommunityPostImageService imageService;
    private final UserRepository users;
    private Long ator(Authentication auth) {
        return users.findByEmail(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
    }
    @GetMapping
    public ResponseEntity<CommunityPostPageDTO> listar(@PathVariable Long communityId,Authentication auth,
        @RequestParam(required=false) Long antesDe,@RequestParam(defaultValue="20") int limite) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listar(communityId,ator(auth),antesDe,limite));
    }
    @GetMapping("/{postId}")
    public ResponseEntity<CommunityPostDTO> buscar(@PathVariable Long communityId,@PathVariable Long postId,Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.buscar(communityId,ator(auth),postId));
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommunityPostDTO> criar(@PathVariable Long communityId,Authentication auth,
        @Valid @RequestPart("publicacao") CreateCommunityPostDTO dto,
        @RequestPart(value="imagem",required=false) MultipartFile image) {
        Long id = ator(auth);
        if (image == null) {
            service.validarAcesso(communityId, id);
        } else {
            service.reservarProcessamentoImagem(communityId, id);
        }
        byte[] data=imageService.preparar(image);
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(service.criar(communityId,id,dto,data));
    }
    @PutMapping(value="/{postId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommunityPostDTO> editar(@PathVariable Long communityId,@PathVariable Long postId,Authentication auth,
        @Valid @RequestPart("publicacao") EditCommunityPostDTO dto,
        @RequestPart(value="imagem",required=false) MultipartFile image) {
        Long id = ator(auth);
        if (image == null) {
            service.validarAcesso(communityId, id);
        } else {
            service.reservarProcessamentoImagem(communityId, id);
        }
        byte[] data=imageService.preparar(image);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.editar(communityId,id,postId,dto,data));
    }
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> excluir(@PathVariable Long communityId,@PathVariable Long postId,
        @RequestParam Long versao,Authentication auth) {
        service.excluir(communityId,ator(auth),postId,versao);return ResponseEntity.noContent().build();
    }
    @GetMapping("/{postId}/image")
    public ResponseEntity<byte[]> imagem(@PathVariable Long communityId,@PathVariable Long postId,Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
            .header("X-Content-Type-Options","nosniff").body(service.imagem(communityId,ator(auth),postId));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).cacheControl(CacheControl.noStore())
            .body(Map.of("error",e.getReason()==null?"Publicação indisponível.":e.getReason()));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String,String>> tamanho() {
        return ResponseEntity.status(413).body(Map.of("error","Envie uma imagem PNG ou JPEG de até 1 MB."));
    }
    @ExceptionHandler(dev.jamal.projetotcc.Service.Social.SocialQuotaExceededException.class)
    public ResponseEntity<java.util.Map<String, String>> quotaExceeded() {
        return ResponseEntity.status(429).header("Retry-After", "60")
                .body(java.util.Map.of("error",
                        "Limite de novas ações atingido. Aguarde até 60 segundos e tente novamente."));
    }
}
