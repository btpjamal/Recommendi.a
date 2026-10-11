package dev.jamal.projetotcc.DTO.Auth;

import dev.jamal.projetotcc.Enum.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private Long userId;
    private String nome;
    private String email;
    private Role role;
}
