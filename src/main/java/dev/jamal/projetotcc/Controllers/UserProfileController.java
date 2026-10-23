package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Profile.UserProfileCreateRequestDTO;
import dev.jamal.projetotcc.DTO.Profile.UserProfileResponseDTO;
import dev.jamal.projetotcc.Service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.jamal.projetotcc.Entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(
        name = "Perfis",
        description = "Gerenciamento do perfil comportamental e financeiro do usuário"
)
@RestController
@RequestMapping("/api/v1/users/{userId}/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;


    @Operation(
            summary = "Buscar perfil do usuário",
            description = "Retorna tempo disponível, orçamento e perfil social do usuário."
    )
    @ApiResponse(responseCode = "200", description = "Perfil encontrado")
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    @Transactional
    @GetMapping
    public ResponseEntity<UserProfileResponseDTO> buscarPerfil(
            @PathVariable Long userId,
            @AuthenticationPrincipal User usuarioAutenticado
    ) {
        if (!usuarioAutenticado.getId().equals(userId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        return ResponseEntity.ok(
                userProfileService.buscarPorUsuario(userId)
        );
    }

    @Operation(
            summary = "Criar ou atualizar perfil",
            description = "Cria o perfil do usuário caso não exista ou atualiza os dados existentes."
    )
    @ApiResponse(responseCode = "200", description = "Perfil salvo com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @PutMapping
    public ResponseEntity<UserProfileResponseDTO> criarOuAtualizarPerfil(
            @PathVariable Long userId,
            @RequestBody @Valid UserProfileCreateRequestDTO dto,
            @AuthenticationPrincipal User usuarioAutenticado
    ) {
        if (!usuarioAutenticado.getId().equals(userId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        return ResponseEntity.ok(
                userProfileService.criarOuAtualizar(userId, dto)
        );
    }
}
