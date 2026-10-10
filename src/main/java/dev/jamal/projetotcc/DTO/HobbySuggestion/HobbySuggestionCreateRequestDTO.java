
package dev.jamal.projetotcc.DTO.HobbySuggestion;

import dev.jamal.projetotcc.Enum.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HobbySuggestionCreateRequestDTO {

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150)
    private String nome;

    @NotBlank(message = "A descrição é obrigatória")
    private String descricao;

    @PositiveOrZero
    private Double custoEstimado;

    @PositiveOrZero
    private Double custoRecorrenteEstimado;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer nivelDificuldade;

    @NotNull
    @Positive
    private Double tempoNecessario;

    @NotNull
    private TipoSocializacao tipoSocializacao;

    @NotNull
    private NivelAtividadeFisica nivelAtividadeFisica;

    @NotNull
    private AmbientePreferido ambiente;

    @NotNull
    private FormatoPreferido formato;

    private boolean requerEquipamento;

    @NotNull
    private Long categoryId;
}
