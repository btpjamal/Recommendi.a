package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.AI.AIConfigurationStatusDTO;
import dev.jamal.projetotcc.DTO.AI.AIKeyRequestDTO;
import dev.jamal.projetotcc.Service.AI.UserAIConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/configuration")
@RequiredArgsConstructor
public class UserAIConfigurationController {

    private final UserAIConfigurationService service;



    @GetMapping("/status")
    public ResponseEntity<AIConfigurationStatusDTO> status(
            Authentication authentication
    ) {

        Long userId =
                obterUserId(authentication);

        return ResponseEntity.ok(
                new AIConfigurationStatusDTO(
                        service.configurada(userId)
                )
        );
    }


    @PutMapping("/api-key")
    public ResponseEntity<Void> salvar(
            @RequestBody AIKeyRequestDTO request,
            Authentication authentication
    ) {

        Long userId =
                obterUserId(authentication);

        service.salvar(
                userId,
                request.apiKey()
        );

        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/api-key")
    public ResponseEntity<Void> remover(
            Authentication authentication
    ) {

        Long userId =
                obterUserId(authentication);

        service.remover(userId);

        return ResponseEntity.noContent().build();
    }


    private Long obterUserId(
            Authentication authentication
    ) {

        return AuthenticatedUser.id(authentication);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>>
    handleIllegalArgument(
            IllegalArgumentException exception
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "error",
                                exception.getMessage()
                        )
                );
    }
}