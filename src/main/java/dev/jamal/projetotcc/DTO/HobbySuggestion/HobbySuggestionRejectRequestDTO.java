
package dev.jamal.projetotcc.DTO.HobbySuggestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HobbySuggestionRejectRequestDTO {

    @NotBlank(message = "A justificativa é obrigatória.")
    @Size(max = 2000, message = "A justificativa deve ter no máximo 2000 caracteres.")
    private String justificativa;
}
