package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.RecommendationProfile.RecommendationProfileCreateRequestDTO;
import dev.jamal.projetotcc.Entities.RecommendationProfile;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class QuestionarioConclusao {

    private final Validator validator;

    void atualizar(RecommendationProfile perfil) {
        var estado = new RecommendationProfileCreateRequestDTO();
        estado.setTempoDisponivelSemanal(perfil.getTempoDisponivelSemanal());
        estado.setOrcamentoInicial(perfil.getOrcamentoInicial());
        estado.setTipoSocializacao(perfil.getTipoSocializacao());
        estado.setNivelAtividadeFisicaDesejada(perfil.getNivelAtividadeFisicaDesejada());
        estado.setAmbientePreferido(perfil.getAmbientePreferido());
        estado.setCidade(perfil.getCidade());
        estado.setEstado(perfil.getEstado());
        perfil.setQuestionarioConcluido(validator.validate(estado).isEmpty());
    }
}
