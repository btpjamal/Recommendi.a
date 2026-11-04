package dev.jamal.projetotcc.Service.recommendation.criteria;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Repository.HobbyInterestRepository;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterestCriterion implements RecommendationCriterion {

    private final HobbyInterestRepository hobbyInterestRepository;

    public InterestCriterion(HobbyInterestRepository hobbyInterestRepository) {
        this.hobbyInterestRepository = hobbyInterestRepository;
    }

    @Override
    public CriterionResult avaliar(
            Hobby hobby,
            RecommendationProfile perfil,
            List<UserInterest> interesses,
            List<UserHobbyFeedback> feedbacks
    ) {
        if (interesses == null || interesses.isEmpty()) {
            return CriterionResult.of(0, null);
        }

        List<HobbyInterest> interessesHobby=
                hobbyInterestRepository.findByHobbyId(hobby.getId());

        if (interessesHobby.isEmpty()) {
            return CriterionResult.of(0,null);
        }


        double maiorPrioridadeUsuario = interesses.stream()
                .mapToInt(ui -> ui.getPeso() != null ? ui.getPeso() : 1)
                .max()
                .orElse(0);

        double somaPrioridadesUsuario = interesses.stream()
                .mapToInt(ui -> ui.getPeso() != null ? ui.getPeso() : 1)
                .sum();

        if (maiorPrioridadeUsuario == 0 || somaPrioridadesUsuario == 0) {
            return CriterionResult.of(0, null);
        }

        double maiorCompatibilidade = 0;
        double somaCompatibilidades = 0;

        for (UserInterest userInterest : interesses) {

            int pesoUsuario = userInterest.getPeso() != null
                    ? userInterest.getPeso()
                    : 1;

            int pesoHobby = interessesHobby.stream()
                    .filter(hi ->
                            hi.getInterest().getId()
                                    .equals(userInterest.getInterest().getId())
                    )
                    .mapToInt(hi -> hi.getPeso() != null ? hi.getPeso() : 1)
                    .max()
                    .orElse(0);

            double compatibilidade = pesoUsuario * pesoHobby;

            maiorCompatibilidade = Math.max(
                    maiorCompatibilidade,
                    compatibilidade
            );

            somaCompatibilidades += compatibilidade;
        }

        if (maiorCompatibilidade == 0) {
            return CriterionResult.of(0, null);
        }

        double afinidadePrincipal =
                maiorCompatibilidade / (3.0 * maiorPrioridadeUsuario);

        double coberturaPonderada =
                somaCompatibilidades / (3.0 * somaPrioridadesUsuario);

        double pontos = 25.0 * (
                0.8 * afinidadePrincipal +
                        0.2 * coberturaPonderada
        );


        List<String> interessesCorrespondentes = interesses.stream()
                .filter(ui ->
                        interessesHobby.stream()
                                .anyMatch(hi ->
                                        hi.getInterest().getId()
                                                .equals(ui.getInterest().getId())
                                )
                )
                .map(ui -> ui.getInterest().getNome())
                .distinct()
                .toList();

        String motivo = interessesCorrespondentes.isEmpty()
                ? null
                : "Combina com seus interesses: "
                + String.join(", ", interessesCorrespondentes);

        return CriterionResult.of(
                pontos,
                motivo
        );
    }
}
