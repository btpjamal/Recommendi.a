package dev.jamal.projetotcc.Controllers;
import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.Social.CommunityInteractionService;
import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;
import org.springframework.http.*;import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/community/communities/{communityId}/posts/{postId}")
public class CommunityInteractionController {
 private final CommunityInteractionService service;private final UserRepository users;
 private Long actor(Authentication auth){return users.findByEmail(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();}
 @GetMapping("/interactions") public ResponseEntity<CommunityInteractionDTO> resumo(@PathVariable Long communityId,@PathVariable Long postId,Authentication auth){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.resumo(communityId,actor(auth),postId));
 }
 @PutMapping("/like") public CommunityInteractionDTO curtir(@PathVariable Long communityId,@PathVariable Long postId,Authentication auth){return service.curtir(communityId,actor(auth),postId,true);}
 @DeleteMapping("/like") public CommunityInteractionDTO descurtir(@PathVariable Long communityId,@PathVariable Long postId,Authentication auth){return service.curtir(communityId,actor(auth),postId,false);}
 @GetMapping("/comments") public ResponseEntity<CommunityCommentPageDTO> comentarios(@PathVariable Long communityId,@PathVariable Long postId,Authentication auth,
  @RequestParam(required=false) Long antesDe,@RequestParam(defaultValue="10") int limite){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listar(communityId,actor(auth),postId,antesDe,limite));
 }
 @GetMapping("/comments/{commentId}") public ResponseEntity<CommunityCommentDTO> buscarComentario(@PathVariable Long communityId,@PathVariable Long postId,@PathVariable Long commentId,Authentication auth){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.buscarComentario(communityId,actor(auth),postId,commentId));
 }
 @PostMapping("/comments") public ResponseEntity<CommunityCommentDTO> comentar(@PathVariable Long communityId,@PathVariable Long postId,Authentication auth,@Valid @RequestBody CreateCommunityCommentDTO dto){
  return ResponseEntity.status(201).body(service.comentar(communityId,actor(auth),postId,dto));
 }
 @PutMapping("/comments/{commentId}") public CommunityCommentDTO editar(@PathVariable Long communityId,@PathVariable Long postId,@PathVariable Long commentId,Authentication auth,@Valid @RequestBody EditCommunityCommentDTO dto){
  return service.editar(communityId,actor(auth),postId,commentId,dto);
 }
 @DeleteMapping("/comments/{commentId}") public ResponseEntity<Void> excluir(@PathVariable Long communityId,@PathVariable Long postId,@PathVariable Long commentId,Authentication auth,@RequestParam Long versao){
  service.excluir(communityId,actor(auth),postId,commentId,versao);return ResponseEntity.noContent().build();
 }
 @PostMapping("/reports") public ResponseEntity<CommunityReportReceiptDTO> denunciar(@PathVariable Long communityId,@PathVariable Long postId,Authentication auth,@Valid @RequestBody CreateCommunityReportDTO dto){
  return ResponseEntity.status(201).body(service.denunciar(communityId,actor(auth),postId,null,dto));
 }
 @PostMapping("/comments/{commentId}/reports") public ResponseEntity<CommunityReportReceiptDTO> denunciarComentario(@PathVariable Long communityId,@PathVariable Long postId,@PathVariable Long commentId,Authentication auth,@Valid @RequestBody CreateCommunityReportDTO dto){
  return ResponseEntity.status(201).body(service.denunciar(communityId,actor(auth),postId,commentId,dto));
 }
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> status(ResponseStatusException e){
  return ResponseEntity.status(e.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("error",e.getReason()==null?"Conteúdo indisponível.":e.getReason()));
 }
    @ExceptionHandler(dev.jamal.projetotcc.Service.Social.SocialQuotaExceededException.class)
    public ResponseEntity<java.util.Map<String, String>> quotaExceeded() {
        return ResponseEntity.status(429).header("Retry-After", "60")
                .body(java.util.Map.of("error",
                        "Limite de novas ações atingido. Aguarde até 60 segundos e tente novamente."));
    }
}
