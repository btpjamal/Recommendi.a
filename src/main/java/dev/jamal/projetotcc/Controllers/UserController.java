package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.User.UserCreateRequestDTO;
import dev.jamal.projetotcc.DTO.User.UserResponseDTO;
import dev.jamal.projetotcc.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.jamal.projetotcc.Entities.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Usuários", description = "Gerenciamento de usuários")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // criar usuário
    // POST /api/v1/users
    @Operation(summary = "Cadastrar usuário", description = "Cria um novo usuário no sistema.")
    @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já cadastrado")
    @PostMapping
    public ResponseEntity<UserResponseDTO> cadastrar(
            @RequestBody @Valid UserCreateRequestDTO dto
    ) {
        UserResponseDTO response = userService.cadastrar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // buscar por ID
    // GET /api/v1/user/1
    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados de um usuário específico.")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @PreAuthorize("hasRole('ADMIN') or #p0 == authentication.principal.id")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> buscarPorId(
        @PathVariable Long id
    ) {
        UserResponseDTO response = userService.buscarPorId(id);

        return ResponseEntity.ok(response);
    }

    // atualizar usuário
    // PUT /api/v1/users/1
    @Operation(summary = "Atualizar usuário", description = "Atualiza nome, email e senha de um usuário.")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou email em uso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @PreAuthorize("hasRole('ADMIN') or #p0 == authentication.principal.id")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> atualizar(
        @PathVariable Long id,
        @RequestBody @Valid UserCreateRequestDTO dto
    ) {
        UserResponseDTO response = userService.atualizar(id, dto);
        return ResponseEntity.ok(response);
    }

    // deletar usuário
    // DELETE /api/v1/users/1
    @Operation(summary = "Deletar usuário", description = "Remove um usuário pelo ID.")
    @ApiResponse(responseCode = "204", description = "Usuário deletado com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @PreAuthorize("hasRole('ADMIN') or #p0 == authentication.principal.id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
        @PathVariable Long id
    ) {
        userService.deletar(id);

        return ResponseEntity.noContent().build();
    }
}
