package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Feedback.FeedbackCreateRequestDTO;
import dev.jamal.projetotcc.DTO.Feedback.FeedbackResponseDTO;
import dev.jamal.projetotcc.Service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Feedbacks", description = "Avaliações dos usuários sobre hobbies experimentados")
@RestController
@RequestMapping("/api/v1/users/{userId}/feedbacks")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("#p0 == authentication.principal.id")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "Avaliar hobby", description = "Registra a avaliação de um usuário para um hobby já experimentado")
    @ApiResponse(responseCode = "201", description = "Feedback registrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Usuário ou hobby não encontrado")
    @PostMapping
    public ResponseEntity<FeedbackResponseDTO> avaliar(
            @PathVariable Long userId,
            @RequestBody  @Valid FeedbackCreateRequestDTO dto
    ) {
        FeedbackResponseDTO response = feedbackService.avaliar(userId,dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Listar feedbacks por usuário", description = "Retorna o histórico de hobbies avaliados por um usuário")
    @ApiResponse(responseCode = "200", description = "Feedbacks encontrados")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @GetMapping
    public ResponseEntity<List<FeedbackResponseDTO>> listarPorUsuario(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                feedbackService.listarPorUsuario(userId)
        );
    }
}
