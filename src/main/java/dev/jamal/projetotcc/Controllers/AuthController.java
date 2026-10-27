package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Auth.AuthRequestDTO;
import dev.jamal.projetotcc.DTO.Auth.AuthResponseDTO;
import dev.jamal.projetotcc.DTO.User.UserCreateRequestDTO;
import dev.jamal.projetotcc.DTO.User.UserResponseDTO;
import dev.jamal.projetotcc.Service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Cadastro e login de usuários")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final dev.jamal.projetotcc.Repository.UserRepository userRepository;

    @PostMapping("/logout")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> logout(
            @org.springframework.security.core.annotation.AuthenticationPrincipal dev.jamal.projetotcc.Entities.User user) {
        userRepository.revokeTokens(user.getId());
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(
            @RequestBody @Valid UserCreateRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @RequestBody @Valid AuthRequestDTO dto
            ) {
            return ResponseEntity.ok(authService.login(dto));
    }
}
