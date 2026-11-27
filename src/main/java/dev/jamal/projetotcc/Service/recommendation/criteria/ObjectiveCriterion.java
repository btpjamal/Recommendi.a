package dev.jamal.projetotcc.Service.recommendation.criteria;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import dev.jamal.projetotcc.Service.recommendation.model.HobbyAssociation;
import dev.jamal.projetotcc.Service.recommendation.model.RecommendationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObjectiveCriterion implements RecommendationCriterion{

    @Override
    public CriterionResult avaliar(Hobby hobby, RecommendationContext contexto) {
        List<UserObjective> objetivosUsuario =
                contexto.objetivos();

        List<HobbyAssociation> objetivosHobby =
                contexto.objetivosDoHobby(hobby.getId());

        if (objetivosUsuario.isEmpty() || objetivosHobby.isEmpty()) {
            return CriterionResult.of(0, null);
        }


        int maiorPesoUsuario = objetivosUsuario.stream()
                .mapToInt(uo -> uo.getPeso() != null ? uo.getPeso() : 1)
                .max()
                .orElse(1);

        int somaPesosUsuario = objetivosUsuario.stream()
                .mapToInt(uo -> uo.getPeso() != null ? uo.getPeso() : 1)
                .sum();

        int maiorCompatibilidade = 0;
        int somaCompatibilidades = 0;

        for (UserObjective objetivoUsuario : objetivosUsuario) {
            int pesoUsuario = objetivoUsuario.getPeso() != null
                    ? objetivoUsuario.getPeso()
                    : 1;

            int pesoHobby = objetivosHobby.stream()
                    .filter(ho -> ho.associationId()
                            .equals(objetivoUsuario.getId().getObjectiveId()))
                    .mapToInt(ho -> ho.peso() != null ? ho.peso() : 1)
                    .max()
                    .orElse(0);

            int compatibilidade = pesoUsuario * pesoHobby;

            maiorCompatibilidade = Math.max(
                    maiorCompatibilidade,
                    compatibilidade
            );

            somaCompatibilidades += compatibilidade;
        }

        if (somaCompatibilidades == 0) {
            return CriterionResult.of(0, null);
        }

        double melhorCorrespondencia =
                (double) maiorCompatibilidade / (3 * maiorPesoUsuario);

        double coberturaGeral =
                (double) somaCompatibilidades / (3 * somaPesosUsuario);

        double pontos = 25 * (
                0.8 * melhorCorrespondencia
                        + 0.2 * coberturaGeral
        );


        List<String> objetivosCorrespondentes = objetivosUsuario.stream()
                .filter(uo ->
                        objetivosHobby.stream()
                                .anyMatch(ho ->
                                        ho.associationId()
                                                .equals(uo.getId().getObjectiveId())
                                )
                )
                .map(uo -> contexto.nomesDosObjetivos().get(uo.getId().getObjectiveId()))
                .distinct()
                .toList();

        String motivo = objetivosCorrespondentes.isEmpty()
                ? null
                : "Ajuda nos seus objetivos: "
                + String.join(", ", objetivosCorrespondentes);

        return CriterionResult.of(
                pontos,
                motivo
        );
    }
}
