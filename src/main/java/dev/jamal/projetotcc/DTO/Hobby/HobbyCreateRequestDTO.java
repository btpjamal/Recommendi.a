package dev.jamal.projetotcc.DTO.Hobby;
import dev.jamal.projetotcc.Enum.*; import jakarta.validation.constraints.*; import lombok.Getter; import lombok.Setter;
@Getter @Setter public class HobbyCreateRequestDTO {
 @NotBlank private String nome; private String descricao; private String imagemUrl;
 @NotNull @PositiveOrZero private Double custoEstimado; @PositiveOrZero private Double custoRecorrenteEstimado;
 @NotNull @Min(1) @Max(5) private Integer nivelDificuldade; @NotNull @Positive private Double tempoNecessario;
 @NotNull private TipoSocializacao tipoSocializacao; @NotNull private NivelAtividadeFisica nivelAtividadeFisica;
 @NotNull private AmbientePreferido ambiente;
 private boolean requerEquipamento;
 @NotNull private Long categoryId;
}
