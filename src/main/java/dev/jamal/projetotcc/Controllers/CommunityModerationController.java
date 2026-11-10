package dev.jamal.projetotcc.Controllers;
import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Enum.CommunityReportStatus;
import dev.jamal.projetotcc.Repository.UserRepository;import dev.jamal.projetotcc.Service.Social.CommunityModerationService;
import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;
import org.springframework.http.*;import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;import java.util.Map;

@RestController @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/community-reports")
public class CommunityModerationController {
 private final CommunityModerationService service;private final UserRepository users;
 private Long actor(Authentication auth){return users.findByEmail(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();}
 @GetMapping public ResponseEntity<CommunityReportPageDTO> listar(Authentication auth,@RequestParam(defaultValue="ABERTA") CommunityReportStatus status,
  @RequestParam(required=false) Long antesDe,@RequestParam(defaultValue="20") int limite){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listar(actor(auth),status,antesDe,limite));
 }
 @PutMapping("/{reportId}/review") public CommunityReportDTO analisar(Authentication auth,@PathVariable Long reportId,@Valid @RequestBody ReviewCommunityReportDTO dto){
  return service.analisar(actor(auth),reportId,dto);
 }
 @GetMapping("/{reportId}/image") public ResponseEntity<byte[]> imagem(Authentication auth,@PathVariable Long reportId){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG).header("X-Content-Type-Options","nosniff").body(service.imagem(actor(auth),reportId));
 }
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> status(ResponseStatusException e){
  return ResponseEntity.status(e.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("error",e.getReason()==null?"Denúncia indisponível.":e.getReason()));
 }
}
