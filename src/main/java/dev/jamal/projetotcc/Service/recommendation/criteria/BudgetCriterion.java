package dev.jamal.projetotcc.Service.recommendation.criteria;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BudgetCriterion implements RecommendationCriterion {


    @Override
    public CriterionResult avaliar(
            Hobby h,
            RecommendationProfile p,
            List<UserInterest> i,
            List<UserHobbyFeedback> f
    ) {
        if (h.getCustoEstimado() == null
                || p.getOrcamentoInicial() == null) {
            return CriterionResult.of(0, null);
        }

        double orcamento = p.getOrcamentoInicial();
        double custo = h.getCustoEstimado();

        String valorOrcamento = formatarMoeda(orcamento);
        String valorCusto = formatarMoeda(custo);

        if (custo <= orcamento) {
            return CriterionResult.of(
                    15,
                    "O custo inicial estimado deste hobby é de "
                            + valorCusto
                            + ", dentro do seu orçamento de "
                            + valorOrcamento + "."
            );
        }

        if (orcamento <= 0) {
            return CriterionResult.warning(
                    -5,
                    "Este hobby possui um custo inicial estimado de "
                            + valorCusto
                            + ", mas você informou um orçamento de "
                            + valorOrcamento + "."
            );
        }

        double excesso = (custo - orcamento) / orcamento;

        if (excesso <= 0.25) {
            return CriterionResult.warning(
                    7,
                    "O custo inicial estimado deste hobby é de "
                            + valorCusto
                            + ", um pouco acima do seu orçamento de "
                            + valorOrcamento + "."
            );
        }

        if (excesso <= 0.75) {
            return CriterionResult.warning(
                    0,
                    "Este hobby exige um investimento inicial estimado de "
                            + valorCusto
                            + ", superior ao seu orçamento de "
                            + valorOrcamento
                            + ". Talvez seja necessário planejar esse gasto."
            );
        }

        return CriterionResult.warning(
                -5,
                "O custo inicial estimado deste hobby é de "
                        + valorCusto
                        + ", significativamente acima do seu orçamento de "
                        + valorOrcamento + "."
        );
    }

    private String formatarMoeda(double valor) {
        return java.text.NumberFormat
                .getCurrencyInstance(
                        java.util.Locale.forLanguageTag("pt-BR")
                )
                .format(valor);
    }

}
