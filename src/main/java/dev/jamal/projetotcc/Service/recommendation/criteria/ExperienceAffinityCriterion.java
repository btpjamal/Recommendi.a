package dev.jamal.projetotcc.Service.recommendation.criteria;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.NivelExperiencia;
import dev.jamal.projetotcc.Enum.UserHobbyStatus;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import dev.jamal.projetotcc.Service.recommendation.model.HobbyAssociation;
import dev.jamal.projetotcc.Service.recommendation.model.RecommendationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ExperienceAffinityCriterion implements RecommendationCriterion{

    @Override
    public CriterionResult avaliar(Hobby hobby, RecommendationContext contexto) {

        List<UserHobby> hobbiesDoUsuario =
                contexto.hobbiesDoUsuario();

        if (hobbiesDoUsuario.isEmpty()) {
            return CriterionResult.of(0, null);
        }

        Set<Long> interessesDoCandidato =
                contexto.interessesDoHobby(hobby.getId())
                        .stream()
                        .map(HobbyAssociation::associationId)
                        .collect(Collectors.toSet());

        if (interessesDoCandidato.isEmpty()){
            return CriterionResult.of(0, null);
        }

        double pontos = 0;

        UserHobby hobbyMaisRelevante = null;
        double maiorContribuicao = 0;

        for (UserHobby userHobby : hobbiesDoUsuario) {

            // não compara o hobby com ele mesmo
            if (userHobby.getHobby().getId().equals(hobby.getId())){
                continue;
            }

            List<HobbyAssociation> interessesDoHobbyConhecido =
                    contexto.interessesDoHobby(userHobby.getHobby().getId());

            long quantidadeEmComum =
                    interessesDoHobbyConhecido
                            .stream()
                            .filter(hi ->
                                    interessesDoCandidato.contains(
                                            hi.associationId()
                                    )
                            )
                            .count();

            if (quantidadeEmComum == 0) {
                continue;
            }

            double pesoExperiencia =
                    obterPesoExperiencia(
                            userHobby.getNivelAtual()
                    );

            double pesoStatus =
                    obterPesoStatus(
                            userHobby.getStatusAtual()
                    );

            double contribuicao =
                    quantidadeEmComum * pesoExperiencia * pesoStatus;

            if (contribuicao <= 0) {
                continue;
            }

            pontos += contribuicao;

            if (contribuicao > maiorContribuicao) {
                maiorContribuicao = contribuicao;
                hobbyMaisRelevante = userHobby;
            }

        }

        pontos = Math.min(pontos, 8);

        if (pontos <= 0) {
            return CriterionResult.of(0, null);
        }



        String nomeHobby = contexto.nomesDosHobbiesDoUsuario().get(hobbyMaisRelevante.getHobby().getId());

        String justificativa = switch (hobbyMaisRelevante.getStatusAtual()) {

            case INTERESSADO ->
                    "Seu interesse por " + nomeHobby
                            + " sugere afinidade com esta atividade, pois compartilham interesses semelhantes.";

            case PAUSADO ->
                    "Seu histórico com " + nomeHobby
                            + " indica interesses em comum com esta atividade.";

            case PRATICANDO ->
                    "Como você pratica " + nomeHobby
                            + ", esta atividade também pode combinar com seus interesses.";
        };

        return CriterionResult.of(pontos, justificativa);


    }

    private double obterPesoExperiencia(
            NivelExperiencia nivel
    ) {
        if (nivel == null) {
            return 0;
        }

        return switch (nivel) {
            case INICIANTE -> 1.0;
            case INTERMEDIARIO -> 1.5;
            case AVANCADO -> 2.0;
        };
    }

    private double obterPesoStatus(
            UserHobbyStatus status
    ) {
        if (status == null) {
            return 0;
        }

        return switch (status) {
            case INTERESSADO -> 0.5;
            case PAUSADO -> 0.75;
            case PRATICANDO -> 1.0;
        };
    }
}
