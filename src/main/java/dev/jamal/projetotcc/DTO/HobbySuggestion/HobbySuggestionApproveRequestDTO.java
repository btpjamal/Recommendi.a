
package dev.jamal.projetotcc.DTO.HobbySuggestion;

import dev.jamal.projetotcc.Enum.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HobbySuggestionApproveRequestDTO {

    @NotBlank
    @Size(max = 150)
    private String nome;

    @NotBlank
    private String descricao;

    private String imagemUrl;

    @NotNull
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

    private boolean requerEquipamento;

    @NotNull
    private Long categoryId;

    @Size(max = 2000)
    private String observacoes;
}
