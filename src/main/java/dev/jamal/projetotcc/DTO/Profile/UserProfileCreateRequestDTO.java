package dev.jamal.projetotcc.DTO.Profile;
import dev.jamal.projetotcc.Enum.TipoSocializacao;
import dev.jamal.projetotcc.Enum.*;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
@Getter @Setter
public class UserProfileCreateRequestDTO {
 @NotNull @Positive private Double tempoDisponivelSemanal;
 @NotNull @PositiveOrZero private Double orcamentoInicial;
 @NotNull private TipoSocializacao tipoSocializacao;
 @NotNull private NivelAtividadeFisica nivelAtividadeFisicaDesejada;
 @NotNull private AmbientePreferido ambientePreferido;
 @NotNull private NivelExperiencia nivelExperiencia;
}
