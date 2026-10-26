package dev.jamal.projetotcc.Controllers;
import dev.jamal.projetotcc.DTO.RecommendationProfile.RecommendationProfileCreateRequestDTO;
import dev.jamal.projetotcc.DTO.RecommendationProfile.RecommendationProfileResponseDTO;
import dev.jamal.projetotcc.Service.RecommendationProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.jamal.projetotcc.Entities.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/recommendation-profiles")
@RequiredArgsConstructor
public class RecommendationProfileControler {
    private final RecommendationProfileService
            recommendationProfileService;

    @PostMapping("/{userId}")
    public ResponseEntity<RecommendationProfileResponseDTO> salvarQuestionario(
            @PathVariable Long userId,
            @Valid @RequestBody RecommendationProfileCreateRequestDTO dto,
            @AuthenticationPrincipal User usuarioAutenticado
    ) {
        if (!usuarioAutenticado.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        RecommendationProfileResponseDTO response =
                recommendationProfileService.salvarQuestionario(userId, dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<RecommendationProfileResponseDTO> buscarPorUsuario(
            @PathVariable Long userId,
            @AuthenticationPrincipal User usuarioAutenticado
    ) {
        if (!usuarioAutenticado.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                recommendationProfileService.buscarPorUsuario(userId)
        );
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<Boolean> verificarQuestionario(
            @PathVariable Long userId,
            @AuthenticationPrincipal User usuarioAutenticado
    ) {
        if (!usuarioAutenticado.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                recommendationProfileService.questionarioConcluido(userId)
        );
    }
}
