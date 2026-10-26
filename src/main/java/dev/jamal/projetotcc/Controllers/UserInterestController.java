package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Interest.UserInterestCreateRequestDTO;
import dev.jamal.projetotcc.DTO.Interest.UserInterestResponseDTO;
import dev.jamal.projetotcc.Service.UserInterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.jamal.projetotcc.Entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@Tag(name = "Interesses", description = "Gerenciamento de interesses pessoais do usuário")
@RestController
@RequestMapping("/api/v1/users/{userId}/interests")
@RequiredArgsConstructor
public class UserInterestController {

    private final UserInterestService userInterestService;

    @Operation(summary = "Listar interesses do usuário", description = "Retorna todos os interesses cadastrados para um usuário e seus respectivos pesos.")
    @ApiResponse(responseCode = "200", description = "Interesses encontrados")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @GetMapping
    public ResponseEntity<List<UserInterestResponseDTO>> listar(
            @PathVariable Long userId,
            @AuthenticationPrincipal User usuarioAutenticado
    ) {
        if (!usuarioAutenticado.getId().equals(userId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        return ResponseEntity.ok(
                userInterestService.listarPorUsuario(userId)
        );
    }

    @Operation(summary = "Adicionar ou atualizar interesses", description = "Adiciona um novo interesse ao usuário ou atualiza o peso de um interesse existente.")
    @ApiResponse(responseCode = "200", description = "Interesse salvo com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Usuário ou interesse não encontrado")
    @PostMapping
    public ResponseEntity<UserInterestResponseDTO> adicionarOuAtualizar(
            @PathVariable Long userId,
            @RequestBody UserInterestCreateRequestDTO dto,
            @AuthenticationPrincipal User usuarioAutenticado
    ) {
        if (!usuarioAutenticado.getId().equals(userId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        return ResponseEntity.ok(
                userInterestService.adicionarOuAtualizar(userId, dto)
        );
    }
}
