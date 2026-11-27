package dev.jamal.projetotcc.DTO.Auth;

import dev.jamal.projetotcc.Service.CanonicalEmail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequestDTO {

    @NotBlank(message = "email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "senha é obrigatória")
    private String senha;
    public void setEmail(String email) {
        this.email = CanonicalEmail.normalize(email);
    }
}
