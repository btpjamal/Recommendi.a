package dev.jamal.projetotcc.Controllers;
import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Repository.UserRepository;import dev.jamal.projetotcc.Service.Social.SocialNotificationService;
import lombok.RequiredArgsConstructor;import org.springframework.http.*;import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;import java.util.Map;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/social/notifications")
public class SocialNotificationController {
 private final SocialNotificationService service;private final UserRepository users;
 private Long actor(Authentication auth){return users.findByEmail(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();}
 @GetMapping public ResponseEntity<SocialNotificationPageDTO> listar(Authentication auth,@RequestParam(required=false) Long antesDe,@RequestParam(defaultValue="20") int limite){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listar(actor(auth),antesDe,limite));
 }
 @GetMapping("/unread") public ResponseEntity<SocialNotificationCountDTO> contar(Authentication auth){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.contar(actor(auth)));
 }
 @PutMapping("/{id}/read") public ResponseEntity<Void> ler(Authentication auth,@PathVariable Long id){
  service.marcarLida(actor(auth),id);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
 }
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> status(ResponseStatusException e){
  return ResponseEntity.status(e.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("error",e.getReason()==null?"Notificação indisponível.":e.getReason()));
 }
}
