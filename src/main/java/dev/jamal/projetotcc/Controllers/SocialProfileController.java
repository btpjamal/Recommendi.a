package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Social.SocialProfileDTO;
import dev.jamal.projetotcc.DTO.Social.SocialProfileStatusDTO;
import dev.jamal.projetotcc.Service.Social.SocialHubService;
import dev.jamal.projetotcc.Service.Social.SocialProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social/me")
@RequiredArgsConstructor
public class SocialProfileController {

    private final SocialProfileService socialProfileService;

    private final SocialHubService socialHubService;


    @GetMapping("/status")
    public ResponseEntity<SocialProfileStatusDTO> status(
            Authentication authentication
    ) {

        Long userId = obterUserId(authentication);

        return ResponseEntity.ok(
                new SocialProfileStatusDTO(
                        socialProfileService.elegivel(userId),
                        socialProfileService.participa(userId)
                )
        );
    }


    @PostMapping("/enable")
    public ResponseEntity<SocialProfileStatusDTO> ativar(
            Authentication authentication
    ) {

        Long userId = obterUserId(authentication);

        socialProfileService.ativar(userId);

        return ResponseEntity.ok(
                new SocialProfileStatusDTO(
                        true,
                        true
                )
        );
    }


    @PostMapping("/disable")
    public ResponseEntity<SocialProfileStatusDTO> desativar(
            Authentication authentication
    ) {

        Long userId = obterUserId(authentication);

        socialProfileService.desativar(userId);

        return ResponseEntity.ok(
                new SocialProfileStatusDTO(
                        socialProfileService.elegivel(userId),
                        false
                )
        );
    }


    private Long obterUserId(
            Authentication authentication
    ) {

        return AuthenticatedUser.id(authentication);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(
            IllegalStateException exception
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        java.util.Map.of(
                                "error",
                                exception.getMessage()
                        )
                );
    }
}