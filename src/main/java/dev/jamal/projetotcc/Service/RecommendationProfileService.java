package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.RecommendationProfile.RecommendationProfileCreateRequestDTO;
import dev.jamal.projetotcc.DTO.RecommendationProfile.RecommendationProfileResponseDTO;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Mapper.RecommendationProfileMapper;
import dev.jamal.projetotcc.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
@Service
@RequiredArgsConstructor
public class RecommendationProfileService {

    private final RecommendationProfileRepository
            recommendationProfileRepository;

    private final UserRepository userRepository;

    private final RecommendationProfileMapper
            recommendationProfileMapper;

    private final InterestRepository interestRepository;

    private final UserInterestRepository userInterestRepository;

    private final ObjectiveRepository objectiveRepository;

    private final UserObjectiveRepository userObjectiveRepository;

    @Transactional
    public RecommendationProfileResponseDTO salvarQuestionario(
            Long userId,
            RecommendationProfileCreateRequestDTO dto
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException("Usuário não encontrado.")
                );

        RecommendationProfile profile =
                recommendationProfileRepository
                        .findByUserId(userId)
                        .orElseGet(() -> {
                            RecommendationProfile novo =
                                    recommendationProfileMapper
                                            .toEntity(dto);

                            novo.setUser(user);

                            return novo;
                        });

        if (profile.getId() != null) {
            recommendationProfileMapper.updateEntity(profile, dto);
        }

        profile.setQuestionarioConcluido(true);

        RecommendationProfile salvo =
                recommendationProfileRepository.save(profile);

        salvarInteresses(user, dto.getInterestIds());
        salvarObjetivos(user, dto.getObjectiveIds());

        List<Long> interestIds =
                userInterestRepository
                        .findByUserIdWithInterest(userId)
                        .stream()
                        .map(userInterest ->
                                userInterest.getInterest().getId()
                        )
                        .toList();

        List<Long> objectiveIds =
                userObjectiveRepository
                        .findByUserId(userId)
                        .stream()
                        .map(userObjective ->
                                userObjective.getObjective().getId()
                        )
                        .toList();

        return recommendationProfileMapper.toResponseDTO(salvo, interestIds, objectiveIds);
    }

    private void salvarObjetivos(
            User user,
            List<Long> objectiveIds
    ) {
        if (objectiveIds == null) {
            return;
        }

        userObjectiveRepository.deleteByUserId(user.getId());

        for (Long objectiveId : objectiveIds){
            Objective objective = objectiveRepository
                    .findById(objectiveId)
                    .orElseThrow(() ->
                            new BusinessException(
                                    "Objetivo não encontrado: "+ objectiveId
                            )
                    );

            UserObjectiveId id = new UserObjectiveId();
            id.setUserId(user.getId());
            id.setObjectiveId(objective.getId());

            UserObjective userObjective = new UserObjective();

            userObjective.setId(id);
            userObjective.setUser(user);
            userObjective.setObjective(objective);
            userObjective.setPeso(1);

            userObjectiveRepository.save(userObjective);
        }
    }

    @Transactional(readOnly = true)
    public RecommendationProfileResponseDTO buscarPorUsuario(
            Long userId
    ) {
        RecommendationProfile profile =
                recommendationProfileRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Perfil de recomendação não encontrado."
                                )
                        );

        List<Long> interestIds =
                userInterestRepository
                        .findByUserIdWithInterest(userId)
                        .stream()
                        .map(ui -> ui.getInterest().getId())
                        .toList();

        List<Long> objectiveIds =
                userObjectiveRepository
                        .findByUserId(userId)
                        .stream()
                        .map(uo -> uo.getObjective().getId())
                        .toList();

        return recommendationProfileMapper.toResponseDTO(profile, interestIds, objectiveIds);
    }

    @Transactional(readOnly = true)
    public boolean questionarioConcluido(Long userId) {
        return recommendationProfileRepository
                .findByUserId(userId)
                .map(RecommendationProfile::getQuestionarioConcluido)
                .orElse(false);
    }

    private void salvarInteresses(
            User user,
            List<Long> interestIds
    ){

        if (interestIds == null){
            return;
        }

        // Remove interesses antigos do usuário
        userInterestRepository.deleteByUserId(user.getId());

        for (Long interestId : interestIds){
            Interest interest = interestRepository.findById(interestId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Interesse não encontrado" + interestId
                            )
                    );

            UserInterestId id = new UserInterestId();
            id.setUserId(user.getId());
            id.setInterestId(interest.getId());

            UserInterest userInterest = new UserInterest();
            userInterest.setId(id);
            userInterest.setUser(user);
            userInterest.setInterest(interest);

            // por enquanto todos valem igual
            userInterest.setPeso(1);

            userInterestRepository.save(userInterest);
        }
    }
}