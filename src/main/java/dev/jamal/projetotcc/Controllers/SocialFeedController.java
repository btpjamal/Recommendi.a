package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.Social.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class SocialFeedController {
    private final SocialFeedService feed;
    private final SocialFollowService follows;
    private final UserRepository users;

    private Long ator(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
    }

    @GetMapping("/feed")
    public ResponseEntity<SocialFeedPageDTO> feed(Authentication auth,
            @RequestParam(defaultValue = "false") boolean seguindo,
            @RequestParam(required = false) Long antesDe, @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(feed.listar(ator(auth), seguindo, antesDe, limite));
    }

    @GetMapping("/users/{userId}/follow")
    public ResponseEntity<SocialFollowDTO> consultar(Authentication auth, @PathVariable Long userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(follows.consultar(ator(auth), userId));
    }

    @PutMapping("/users/{userId}/follow")
    public ResponseEntity<SocialFollowDTO> seguir(Authentication auth, @PathVariable Long userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(follows.alterar(ator(auth), userId, true));
    }

    @DeleteMapping("/users/{userId}/follow")
    public ResponseEntity<SocialFollowDTO> deixarDeSeguir(Authentication auth, @PathVariable Long userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(follows.alterar(ator(auth), userId, false));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> erro(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of("error", e.getReason() == null ? "Acesso indisponível." : e.getReason()));
    }
}
