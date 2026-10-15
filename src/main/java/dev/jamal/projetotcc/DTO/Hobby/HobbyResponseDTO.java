package dev.jamal.projetotcc.DTO.Hobby;

import dev.jamal.projetotcc.DTO.Category.HobbyCategoryResponseDTO;
import dev.jamal.projetotcc.Enum.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HobbyResponseDTO {
    private Long id;
    private String nome;
    private String descricao;
    private String imagemUrl;
    private Double custoEstimado;
    private Double custoRecorrenteEstimado;
    private Integer nivelDificuldade;
    private Double tempoNecessario;
    private TipoSocializacao tipoSocializacao;
    private NivelAtividadeFisica nivelAtividadeFisica;
    private AmbientePreferido ambiente;
    private boolean requerEquipamento;
    private HobbyCategoryResponseDTO category;
    private boolean ativo;
}
