package dev.jamal.projetotcc.Service.recommendation.model;

import dev.jamal.projetotcc.Entities.RecommendationProfile;
import dev.jamal.projetotcc.Entities.UserHobby;
import dev.jamal.projetotcc.Entities.UserInterest;
import dev.jamal.projetotcc.Entities.UserObjective;
import dev.jamal.projetotcc.Entities.UserRecommendationFeedback;

import java.util.List;
import java.util.Map;

public record RecommendationContext(
        RecommendationProfile perfil,
        List<UserInterest> interesses,
        List<UserObjective> objetivos,
        List<HobbyRating> avaliacoes,
        List<UserRecommendationFeedback> decisoes,
        List<UserHobby> hobbiesDoUsuario,
        Map<Long, String> nomesDosHobbiesDoUsuario,
        Map<Long, String> nomesDosObjetivos,
        Map<Long, List<HobbyAssociation>> interessesPorHobby,
        Map<Long, List<HobbyAssociation>> objetivosPorHobby
) {
    public List<HobbyAssociation> interessesDoHobby(Long hobbyId) {
        return interessesPorHobby.getOrDefault(hobbyId, List.of());
    }

    public List<HobbyAssociation> objetivosDoHobby(Long hobbyId) {
        return objetivosPorHobby.getOrDefault(hobbyId, List.of());
    }
}
