package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Enum.SocialContactBox;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.Social.SocialContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/contacts")
public class SocialContactController {
    private final SocialContactService service;
    private final UserRepository users;

    private Long ator(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        return users.findByEmail(authentication.getName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário indisponível.")).getId();
    }

    @PostMapping("/requests")
    public SocialContactDTO solicitar(Authentication authentication, @Valid @RequestBody SocialContactRequestDTO dto) {
        return service.solicitar(ator(authentication), dto);
    }

    @GetMapping
    public Page<SocialContactDTO> listar(Authentication authentication,
            @RequestParam(defaultValue = "RECEBIDAS") SocialContactBox caixa,
            @RequestParam(defaultValue = "0") int pagina) {
        return service.listar(ator(authentication), caixa, pagina);
    }

    @PostMapping("/{id}/accept")
    public SocialContactDTO aceitar(Authentication authentication, @PathVariable Long id) {
        return service.responder(ator(authentication), id, true);
    }
    @PostMapping("/{id}/decline")
    public SocialContactDTO recusar(Authentication authentication, @PathVariable Long id) {
        return service.responder(ator(authentication), id, false);
    }
    @PostMapping("/{id}/cancel")
    public SocialContactDTO cancelar(Authentication authentication, @PathVariable Long id) {
        return service.cancelar(ator(authentication), id);
    }
    @PostMapping("/blocks/{outroId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bloquear(Authentication authentication, @PathVariable Long outroId) {
        service.bloquear(ator(authentication), outroId);
    }
    @DeleteMapping("/blocks/{outroId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desbloquear(Authentication authentication, @PathVariable Long outroId) {
        service.desbloquear(ator(authentication), outroId);
    }
    @GetMapping("/blocks")
    public Page<SocialBlockDTO> bloqueados(Authentication authentication, @RequestParam(defaultValue = "0") int pagina) {
        return service.bloqueados(ator(authentication), pagina);
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> falha(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("error",
                erro.getReason() == null ? "Não foi possível concluir a operação." : erro.getReason()));
    }
}
