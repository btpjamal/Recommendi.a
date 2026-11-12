package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Social.SocialPeoplePageDTO;
import dev.jamal.projetotcc.Enum.SocialLocationScope;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.Social.SocialPeopleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/community/users/page")
@RequiredArgsConstructor
public class SocialPeopleController {
    private final UserRepository users;
    private final SocialPeopleService people;

    @GetMapping
    public ResponseEntity<SocialPeoplePageDTO> listar(Authentication auth,
            @RequestParam(defaultValue = "GERAL") SocialLocationScope escopo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int limite) {
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var user = users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(people.listar(user.getId(), escopo, pagina, limite));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> erro(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of("error", error.getReason() == null ? "Não foi possível carregar as pessoas." : error.getReason()));
    }
}
