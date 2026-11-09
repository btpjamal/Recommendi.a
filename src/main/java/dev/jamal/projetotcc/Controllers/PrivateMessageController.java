package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.Social.PrivateMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/private-messages")
public class PrivateMessageController {
    private final PrivateMessageService service;
    private final UserRepository users;

    private Long ator(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        return users.findByEmail(authentication.getName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário indisponível.")).getId();
    }

    @GetMapping("/unread")
    public PrivateUnreadDTO naoLidas(Authentication authentication) { return service.naoLidas(ator(authentication)); }

    @GetMapping("/{contatoId}")
    public SocialContactDTO detalhes(Authentication authentication, @PathVariable Long contatoId) {
        return service.detalhes(ator(authentication), contatoId);
    }
    @GetMapping("/{contatoId}/messages")
    public PrivateMessagePageDTO listar(Authentication authentication, @PathVariable Long contatoId,
            @RequestParam(required = false) Long antesDe, @RequestParam(required = false) Long aposId,
            @RequestParam(defaultValue = "30") int limite) {
        return service.listar(ator(authentication), contatoId, antesDe, aposId, limite);
    }
    @PostMapping("/{contatoId}/messages")
    public PrivateMessageDTO enviar(Authentication authentication, @PathVariable Long contatoId,
            @Valid @RequestBody SendPrivateMessageDTO pedido) {
        return service.enviar(ator(authentication), contatoId, pedido);
    }
    @PostMapping("/{contatoId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLidas(Authentication authentication, @PathVariable Long contatoId,
            @Valid @RequestBody ReadPrivateMessagesDTO pedido) {
        service.marcarLidas(ator(authentication), contatoId, pedido.ateId());
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> erro(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("error",
                erro.getReason() == null ? "Não foi possível concluir a operação." : erro.getReason()));
    }
}
