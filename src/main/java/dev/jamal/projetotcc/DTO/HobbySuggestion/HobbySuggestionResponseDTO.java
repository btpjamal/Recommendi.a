
package dev.jamal.projetotcc.DTO.HobbySuggestion;

import dev.jamal.projetotcc.Enum.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class HobbySuggestionResponseDTO {

    private Long id;
    private String nome;
    private String descricao;

    private Double custoEstimado;
    private Double custoRecorrenteEstimado;
    private Integer nivelDificuldade;
    private Double tempoNecessario;

    private TipoSocializacao tipoSocializacao;
    private NivelAtividadeFisica nivelAtividadeFisica;
    private AmbientePreferido ambiente;
    private boolean requerEquipamento;

    private Long categoryId;
    private String categoryNome;

    private Long createdById;
    private String createdByNome;

    private HobbySuggestionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    private String reviewNotes;
    private Long approvedHobbyId;
}
