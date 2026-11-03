package dev.jamal.projetotcc.DTO.User;
import jakarta.validation.constraints.*; import lombok.Getter; import lombok.Setter; import java.time.LocalDate;
@Getter @Setter public class UserCreateRequestDTO {
 @Size(max=255) @NotBlank(message="O nome é obrigatório") private String nome;
 @Size(max=255) @NotBlank(message="O email é obrigatório") @Email(message="Email inválido") private String email;
 @Size(min=12, max=72, message="A senha deve possuir entre 12 e 72 caracteres") @NotBlank(message="A senha é obrigatória")  private String senha;
 @NotNull(message="A data de nascimento é obrigatória") @Past(message="A data de nascimento deve estar no passado") private LocalDate dataNascimento;
}
