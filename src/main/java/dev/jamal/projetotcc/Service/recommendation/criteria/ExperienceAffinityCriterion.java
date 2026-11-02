package dev.jamal.projetotcc.Service.recommendation.criteria;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.NivelExperiencia;
import dev.jamal.projetotcc.Enum.UserHobbyStatus;
import dev.jamal.projetotcc.Repository.HobbyInterestRepository;
import dev.jamal.projetotcc.Repository.UserHobbyRepository;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExperienceAffinityCriterion implements RecommendationCriterion{

    private final UserHobbyRepository userHobbyRepository;
    private final HobbyInterestRepository hobbyInterestRepository;

    @Override
    public CriterionResult avaliar(
            Hobby hobby,
            RecommendationProfile perfil,
            List<UserInterest> interesses,
            List<UserHobbyFeedback> feedbacks
    ) {

        Long userId = perfil.getUser().getId();

        List<UserHobby> hobbiesDoUsuario =
                userHobbyRepository.findByUser_Id(userId);

        if (hobbiesDoUsuario.isEmpty()) {
            return CriterionResult.of(0, null);
        }

        Set<Long> interessesDoCandidato =
                hobbyInterestRepository
                        .findByHobbyId(hobby.getId())
                        .stream()
                        .map(hi -> hi.getInterest().getId())
                        .collect(Collectors.toSet());

        if (interessesDoCandidato.isEmpty()){
            return CriterionResult.of(0, null);
        }

        double pontos = 0;

        boolean possuiInteressado = false;
        boolean possuiPausado = false;
        boolean possuiPraticando = false;
        UserHobby hobbyMaisRelevante = null;
        double maiorContribuicao = 0;

        for (UserHobby userHobby : hobbiesDoUsuario) {

            // não compara o hobby com ele mesmo
            if (userHobby.getHobby().getId().equals(hobby.getId())){
                continue;
            }

            List<HobbyInterest> interessesDoHobbyConhecido =
                    hobbyInterestRepository
                            .findByHobbyId(
                                    userHobby.getHobby().getId()
                            );

            long quantidadeEmComum =
                    interessesDoHobbyConhecido
                            .stream()
                            .filter(hi ->
                                    interessesDoCandidato.contains(
                                            hi.getInterest().getId()
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

            switch (userHobby.getStatusAtual()) {
                case INTERESSADO -> possuiInteressado = true;
                case PAUSADO -> possuiPausado = true;
                case PRATICANDO -> possuiPraticando = true;
            }
        }

        pontos = Math.min(pontos, 8);

        if (pontos <= 0) {
            return CriterionResult.of(0, null);
        }



        String nomeHobby = hobbyMaisRelevante.getHobby().getNome();

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
