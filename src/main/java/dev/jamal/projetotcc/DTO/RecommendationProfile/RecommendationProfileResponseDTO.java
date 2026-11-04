package dev.jamal.projetotcc.DTO.RecommendationProfile;

import dev.jamal.projetotcc.Enum.*;

import java.util.List;

public class RecommendationProfileResponseDTO {

    private Long id;
    private Long userId;
    private Double tempoDisponivelSemanal;
    private Double orcamentoInicial;
    private TipoSocializacao tipoSocializacao;
    private NivelAtividadeFisica nivelAtividadeFisicaDesejada;
    private AmbientePreferido ambientePreferido;
    private Boolean questionarioConcluido;
    private List<Long> interestIds;
    private List<UserInterestAssociationDTO> interestAssociations;
    private List<Long> objectiveIds;
    private String cidade;
    private String estado;

    public RecommendationProfileResponseDTO(
            Long id,
            Long userId,
            Double tempoDisponivelSemanal,
            Double orcamentoInicial,
            TipoSocializacao tipoSocializacao,
            NivelAtividadeFisica nivelAtividadeFisicaDesejada,
            AmbientePreferido ambientePreferido,
            Boolean questionarioConcluido,
            List interestIds,
            List objectiveIds,
            String cidade,
            String estado
    ) {
        this.id = id;
        this.userId = userId;
        this.tempoDisponivelSemanal = tempoDisponivelSemanal;
        this.orcamentoInicial = orcamentoInicial;
        this.tipoSocializacao = tipoSocializacao;
        this.nivelAtividadeFisicaDesejada = nivelAtividadeFisicaDesejada;
        this.ambientePreferido = ambientePreferido;
        this.questionarioConcluido = questionarioConcluido;
        this.interestIds = interestIds;
        this.objectiveIds = objectiveIds;
        this.cidade = cidade;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Double getTempoDisponivelSemanal() {
        return tempoDisponivelSemanal;
    }

    public Double getOrcamentoInicial() {
        return orcamentoInicial;
    }

    public TipoSocializacao getTipoSocializacao() {
        return tipoSocializacao;
    }

    public NivelAtividadeFisica getNivelAtividadeFisicaDesejada() {
        return nivelAtividadeFisicaDesejada;
    }

    public AmbientePreferido getAmbientePreferido() {
        return ambientePreferido;
    }

    public Boolean getQuestionarioConcluido() {
        return questionarioConcluido;
    }

    public List<Long> getInterestIds() {
        return interestIds;
    }

    public void setInterestIds(List<Long> interestIds) {
        this.interestIds = interestIds;
    }


    public List<UserInterestAssociationDTO> getInterestAssociations() {
        return interestAssociations;
    }

    public void setInterestAssociations(
            List<UserInterestAssociationDTO> interestAssociations
    ) {
        this.interestAssociations = interestAssociations;
    }


    public List<Long> getObjectiveIds() {
        return objectiveIds;
    }

    public void setObjectiveIds(List<Long> objectiveIds) {
        this.objectiveIds = objectiveIds;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}