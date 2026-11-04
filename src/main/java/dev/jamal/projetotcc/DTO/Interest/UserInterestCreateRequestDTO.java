package dev.jamal.projetotcc.DTO.Interest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInterestCreateRequestDTO {

    @NotNull(message = "O interesse é obrigatório")
    private Long interestId;

    @NotNull(message = "O peso é obrigatório")
    @Min(value = 1, message = "O peso mínimo é 1")
    @Max(value = 3, message = "O peso máximo é 3")
    private Integer peso;

}
