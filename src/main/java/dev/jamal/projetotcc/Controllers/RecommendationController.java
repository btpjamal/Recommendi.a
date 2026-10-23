package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Recommendation.HobbyRecommendationDTO;
import dev.jamal.projetotcc.DTO.Recommendation.RecommendationDetailsDTO;
import dev.jamal.projetotcc.DTO.Response.InitialPlanResponseDTO;
//import dev.jamal.projetotcc.Service.InitialPlanService;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Service.RecommendationDetailsService;
import dev.jamal.projetotcc.Service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@Tag(name = "Recomendações", description = "Endpoints responsáveis por gerar recomendações personalizadas de hobbies")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationDetailsService recommendationDetailsService;


    @Operation(summary = "Gerar recomendações para um usuário", description = "Retorna uma lista de hobbies recomendados com base no perfil, interesses e feedback do usuário.")
    @ApiResponse(responseCode = "200", description = "Recomendações geradas com sucesso")
    @ApiResponse(responseCode = "404", description = "Perfil do usuário não encontrado")
    @GetMapping("/{userId}")
    public ResponseEntity<List<HobbyRecommendationDTO>> recomendar(
            @PathVariable Long userId,
            @AuthenticationPrincipal User usuarioAutenticado
    ) {
        if (!usuarioAutenticado.getId().equals(userId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        return ResponseEntity.ok(
                recommendationService.recomendar(userId)
        );
    }

    @GetMapping("/{hobbyId}/details")
    public ResponseEntity<RecommendationDetailsDTO> buscarDetalhes(
            @AuthenticationPrincipal User user,
            @PathVariable Long hobbyId
    ) {

        return ResponseEntity.ok(
                recommendationDetailsService.buscarDetalhes(
                        user.getId(),
                        hobbyId
                )
        );
    }

}
