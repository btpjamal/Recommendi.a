package dev.jamal.projetotcc.DTO.RecommendationProfile;

import dev.jamal.projetotcc.Enum.AmbientePreferido;
import dev.jamal.projetotcc.Enum.NivelAtividadeFisica;
import dev.jamal.projetotcc.Enum.TipoSocializacao;
import jakarta.validation.constraints.*;

import java.util.List;

public class RecommendationProfileCreateRequestDTO {

    @NotNull
    @Positive
    private Double tempoDisponivelSemanal;

    @NotNull
    @DecimalMin("0.0")
    private Double orcamentoInicial;

    @NotNull
    private TipoSocializacao tipoSocializacao;

    @NotNull
    private NivelAtividadeFisica nivelAtividadeFisicaDesejada;

    @NotNull
    private AmbientePreferido ambientePreferido;

    @Size(max = 100)
    private List<@Positive Long> interestIds;

    @Size(max = 100)
    private List<@Positive Long> objectiveIds;

    @NotBlank(message = "A cidade é obrigatória.")
    @Size(max = 100)
    @Pattern(regexp = "[\\p{L}\\p{M} .’'-]+", message = "Informe apenas o nome da cidade.")
    private String cidade;

    @NotBlank(message = "O estado é obrigatório.")
    @Size(min = 2, max = 2, message = "Informe a sigla do estado.")
    @Pattern(regexp = "(?i)AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO", message = "Estado inválido.")
    private String estado;

    public Double getTempoDisponivelSemanal() {
        return tempoDisponivelSemanal;
    }

    public void setTempoDisponivelSemanal(
            Double tempoDisponivelSemanal
    ) {
        this.tempoDisponivelSemanal = tempoDisponivelSemanal;
    }

    public Double getOrcamentoInicial() {
        return orcamentoInicial;
    }

    public void setOrcamentoInicial(Double orcamentoInicial) {
        this.orcamentoInicial = orcamentoInicial;
    }

    public TipoSocializacao getTipoSocializacao() {
        return tipoSocializacao;
    }

    public void setTipoSocializacao(
            TipoSocializacao tipoSocializacao
    ) {
        this.tipoSocializacao = tipoSocializacao;
    }


    public NivelAtividadeFisica getNivelAtividadeFisicaDesejada() {
        return nivelAtividadeFisicaDesejada;
    }

    public void setNivelAtividadeFisicaDesejada(
            NivelAtividadeFisica nivelAtividadeFisicaDesejada
    ) {
        this.nivelAtividadeFisicaDesejada =
                nivelAtividadeFisicaDesejada;
    }

    public AmbientePreferido getAmbientePreferido() {
        return ambientePreferido;
    }

    public void setAmbientePreferido(
            AmbientePreferido ambientePreferido
    ) {
        this.ambientePreferido = ambientePreferido;
    }

    public List<Long> getInterestIds() {
        return interestIds;
    }

    public void setInterestIds(List<Long> interestIds) {
        this.interestIds = interestIds;
    }

    public List<Long> getObjectiveIds() {
        return objectiveIds;
    }

    public void setObjectiveIds(List<Long> objectiveIds) {
        this.objectiveIds = objectiveIds;
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