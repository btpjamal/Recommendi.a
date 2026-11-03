package dev.jamal.projetotcc.Service.recommendation.criteria;

import dev.jamal.projetotcc.Enum.TipoSocializacao;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.*;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PreferenceCriterion implements RecommendationCriterion {

    @Override
    public CriterionResult avaliar(
            Hobby h,
            RecommendationProfile p,
            List<UserInterest> i,
            List<UserHobbyFeedback> f
    ) {

        double score = 0;
        List<String> motivos = new ArrayList<>();
        List<String> alertas = new ArrayList<>();


        if (p.getTipoSocializacao() == TipoSocializacao.INDIFERENTE
                || p.getTipoSocializacao() == h.getTipoSocializacao()) {

            score += 7;

            String justificativa;

            if (p.getTipoSocializacao() == TipoSocializacao.INDIFERENTE) {

                justificativa =
                        "Esta atividade oferece uma forma de socialização "
                                + "que pode complementar suas preferências.";

            } else {

                justificativa = switch (h.getTipoSocializacao()) {

                    case INDIVIDUAL -> "Este hobby favorece a prática individual, "
                            + "combinando com sua preferência por atividades "
                            + "mais independentes.";

                    case SOCIAL -> "Este hobby favorece a interação com outras pessoas, "
                            + "combinando com sua preferência por atividades sociais.";

                    default -> "Este hobby combina com sua preferência de socialização.";
                };
            }

            motivos.add(justificativa);
        }



        if (p.getAmbientePreferido() != null
                && h.getAmbiente() != null) {

            AmbientePreferido preferencia = p.getAmbientePreferido();
            AmbientePreferido ambienteHobby = h.getAmbiente();

            if (preferencia == AmbientePreferido.INDIFERENTE
                    || ambienteHobby == AmbientePreferido.INDIFERENTE
                    || preferencia == ambienteHobby) {

                score += 5;

                if (preferencia == AmbientePreferido.INDIFERENTE) {
                    motivos.add(
                            "Você não possui uma preferência específica de ambiente, "
                                    + "o que torna esta atividade uma opção compatível."
                    );

                } else if (ambienteHobby == AmbientePreferido.INDIFERENTE) {
                    motivos.add(
                            "Este hobby pode ser praticado em diferentes ambientes, "
                                    + "permitindo que você escolha aquele que prefere."
                    );

                } else {
                    motivos.add(
                            "Este hobby é praticado em um ambiente "
                                    + "que corresponde à sua preferência por "
                                    + formatarAmbiente(ambienteHobby) + "."
                    );
                }

            } else {
                alertas.add(
                        "Este hobby costuma ser praticado em "
                                + formatarAmbiente(ambienteHobby)
                                + ", enquanto você prefere "
                                + formatarAmbiente(preferencia) + "."
                );
            }
        }




        if (p.getNivelAtividadeFisicaDesejada() != null
                && h.getNivelAtividadeFisica() != null) {

            NivelAtividadeFisica preferencia =
                    p.getNivelAtividadeFisicaDesejada();

            NivelAtividadeFisica nivelHobby =
                    h.getNivelAtividadeFisica();

            if (preferencia == NivelAtividadeFisica.INDIFERENTE
                    || preferencia == nivelHobby) {

                score += 4;

                if (preferencia == NivelAtividadeFisica.INDIFERENTE) {

                    motivos.add(
                            "Como você não possui uma preferência específica "
                                    + "de intensidade física, esta atividade pode "
                                    + "ser uma opção interessante."
                    );

                } else {

                    motivos.add(
                            "Este hobby possui um nível de atividade física "
                                    + "compatível com a intensidade que você procura: "
                                    + formatarNivelAtividade(nivelHobby) + "."
                    );
                }
            }
        }


        return new CriterionResult(
                score,
                motivos,
                alertas
        );

    }

    private String formatarAmbiente(AmbientePreferido ambiente) {
        return switch (ambiente) {
            case INDIFERENTE -> "qualquer ambiente";
            default -> ambiente.name()
                    .toLowerCase(java.util.Locale.forLanguageTag("pt-BR"))
                    .replace("_", " ");
        };
    }

    private String formatarNivelAtividade(NivelAtividadeFisica nivel) {
        return nivel.name()
                .toLowerCase(java.util.Locale.forLanguageTag("pt-BR"))
                .replace("_", " ");
    }

}

