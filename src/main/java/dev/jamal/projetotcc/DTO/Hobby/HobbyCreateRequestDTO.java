
package dev.jamal.projetotcc.DTO.Hobby;

import dev.jamal.projetotcc.Enum.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HobbyCreateRequestDTO {

 @NotBlank(message = "O nome é obrigatório.")
 @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
 private String nome;

 @NotBlank(message = "A descrição é obrigatória.")
 private String descricao;

 @Size(max = 2000, message = "A URL da imagem é muito longa.")
 private String imagemUrl;

 @NotNull(message = "O custo inicial é obrigatório.")
 @PositiveOrZero(message = "O custo inicial não pode ser negativo.")
 private Double custoEstimado;

 @PositiveOrZero(message = "O custo recorrente não pode ser negativo.")
 private Double custoRecorrenteEstimado;

 @NotNull(message = "A dificuldade é obrigatória.")
 @Min(value = 1, message = "A dificuldade mínima é 1.")
 @Max(value = 5, message = "A dificuldade máxima é 5.")
 private Integer nivelDificuldade;

 @NotNull(message = "O tempo necessário é obrigatório.")
 @Positive(message = "O tempo necessário deve ser maior que zero.")
 private Double tempoNecessario;

 @NotNull(message = "O tipo de socialização é obrigatório.")
 private TipoSocializacao tipoSocializacao;

 @NotNull(message = "O nível de atividade física é obrigatório.")
 private NivelAtividadeFisica nivelAtividadeFisica;

 @NotNull(message = "O ambiente é obrigatório.")
 private AmbientePreferido ambiente;

 private boolean requerEquipamento;

 @NotNull(message = "A categoria é obrigatória.")
 private Long categoryId;
}
