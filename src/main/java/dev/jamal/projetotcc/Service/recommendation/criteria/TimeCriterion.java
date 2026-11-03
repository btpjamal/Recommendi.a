package dev.jamal.projetotcc.Service.recommendation.criteria;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TimeCriterion implements RecommendationCriterion {

    @Override
    public CriterionResult avaliar(
            Hobby h,
            RecommendationProfile p,
            List<UserInterest> i,
            List<UserHobbyFeedback> f
    ) {
        if (h.getTempoNecessario() == null
                || p.getTempoDisponivelSemanal() == null) {
            return CriterionResult.of(0, null);
        }

        double disponivel = p.getTempoDisponivelSemanal();
        double necessario = h.getTempoNecessario();

        String tempoDisponivel = formatarHoras(disponivel);
        String tempoNecessario = formatarHoras(necessario);

        if (disponivel >= necessario) {
            return CriterionResult.of(
                    15,
                    "Este hobby exige cerca de " + tempoNecessario
                            + " por semana, dentro das "
                            + tempoDisponivel
                            + " que você possui disponíveis."
            );
        }

        double proporcao = disponivel / necessario;

        if (proporcao >= 0.75) {
            return CriterionResult.warning(
                    7,
                    "Este hobby exige cerca de " + tempoNecessario
                            + " por semana, enquanto você possui "
                            + tempoDisponivel
                            + " disponíveis. Talvez seja necessário ajustar sua rotina."
            );
        }

        if (proporcao >= 0.5) {
            return CriterionResult.warning(
                    2,
                    "Com " + tempoDisponivel
                            + " disponíveis por semana, pode ser difícil encaixar "
                            + "uma atividade que exige cerca de "
                            + tempoNecessario + "."
            );
        }

        return CriterionResult.warning(
                -5,
                "Este hobby exige cerca de " + tempoNecessario
                        + " por semana, significativamente mais que as "
                        + tempoDisponivel
                        + " que você possui disponíveis."
        );
    }

    private String formatarHoras(double horas) {
        return String.format(
                java.util.Locale.forLanguageTag("pt-BR"),
                "%.1f %s",
                horas,
                horas == 1.0 ? "hora" : "horas"
        );
    }
}
