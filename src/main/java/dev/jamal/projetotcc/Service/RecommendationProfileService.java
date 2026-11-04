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

import dev.jamal.projetotcc.DTO.RecommendationProfile.UserInterestAssociationDTO;
import dev.jamal.projetotcc.DTO.RecommendationProfile.UserObjectiveAssociationDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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


        if (dto.getInterestIds() != null
                && dto.getInterestAssociations() != null) {

            throw new BusinessException(
                    "Informe interestIds ou interestAssociations, " +
                            "mas não ambos."
            );
        }

        if (dto.getObjectiveIds() != null
                && dto.getObjectiveAssociations() != null) {

            throw new BusinessException(
                    "Informe objectiveIds ou objectiveAssociations, " +
                            "mas não ambos."
            );
        }


        if (dto.getInterestAssociations() != null) {
            sincronizarInteresses(user, dto.getInterestAssociations());
        } else {
            salvarInteresses(user, dto.getInterestIds());
        }

        if (dto.getObjectiveAssociations() != null) {
            sincronizarObjetivos(user, dto.getObjectiveAssociations());
        } else {
            salvarObjetivos(user, dto.getObjectiveIds());
        }

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


        RecommendationProfileResponseDTO response =
                recommendationProfileMapper.toResponseDTO(
                        salvo, interestIds, objectiveIds
                );

        response.setInterestAssociations(
                buscarInteressesComPesos(userId)
        );

        response.setObjectiveAssociations(
                buscarObjetivosComPesos(userId)
        );

        return response;

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


        RecommendationProfileResponseDTO response =
                recommendationProfileMapper.toResponseDTO(
                        profile, interestIds, objectiveIds
                );

        response.setInterestAssociations(
                buscarInteressesComPesos(userId)
        );

        response.setObjectiveAssociations(
                buscarObjetivosComPesos(userId)
        );

        return response;

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


    private void sincronizarInteresses(
            User user,
            List<UserInterestAssociationDTO> associacoes
    ) {
        if (associacoes == null) {
            return;
        }

        Set<Long> idsRecebidos = new HashSet<>();

        // Validar antes de alterar os registros
        for (UserInterestAssociationDTO associacao : associacoes) {

            if (associacao == null
                    || associacao.interestId() == null
                    || associacao.peso() == null
                    || associacao.peso() < 1
                    || associacao.peso() > 3) {

                throw new BusinessException(
                        "Associação de interesse inválida."
                );
            }

            if (!idsRecebidos.add(associacao.interestId())) {
                throw new BusinessException(
                        "Interesse duplicado: " + associacao.interestId()
                );
            }
        }

        List<UserInterest> existentes =
                userInterestRepository.findByUserIdWithInterest(user.getId());

        // Remover interesses que não foram enviados
        for (UserInterest existente : existentes) {
            if (!idsRecebidos.contains(existente.getInterest().getId())) {
                userInterestRepository.delete(existente);
            }
        }

        // Atualizar associações existentes ou criar novas
        for (UserInterestAssociationDTO associacao : associacoes) {

            UserInterest userInterest = existentes.stream()
                    .filter(ui -> ui.getInterest().getId()
                            .equals(associacao.interestId()))
                    .findFirst()
                    .orElse(null);

            if (userInterest != null) {
                userInterest.setPeso(associacao.peso());
            } else {
                Interest interest = interestRepository
                        .findById(associacao.interestId())
                        .orElseThrow(() -> new BusinessException(
                                "Interesse não encontrado: "
                                        + associacao.interestId()
                        ));

                UserInterestId id = new UserInterestId();
                id.setUserId(user.getId());
                id.setInterestId(interest.getId());

                userInterest = new UserInterest();
                userInterest.setId(id);
                userInterest.setUser(user);
                userInterest.setInterest(interest);
                userInterest.setPeso(associacao.peso());
            }

            userInterestRepository.save(userInterest);
        }
    }


    private void sincronizarObjetivos(
            User user,
            List<UserObjectiveAssociationDTO> associacoes
    ) {
        if (associacoes == null) {
            return;
        }

        Set<Long> idsRecebidos = new HashSet<>();

        // Validar as associações antes de modificar os registros
        for (UserObjectiveAssociationDTO associacao : associacoes) {

            if (associacao == null
                    || associacao.objectiveId() == null
                    || associacao.peso() == null
                    || associacao.peso() < 1
                    || associacao.peso() > 3) {

                throw new BusinessException(
                        "Associação de objetivo inválida."
                );
            }

            if (!idsRecebidos.add(associacao.objectiveId())) {
                throw new BusinessException(
                        "Objetivo duplicado: " + associacao.objectiveId()
                );
            }
        }

        List<UserObjective> existentes =
                userObjectiveRepository.findByUserId(user.getId());

        // Remover objetivos que não foram enviados
        for (UserObjective existente : existentes) {
            if (!idsRecebidos.contains(existente.getObjective().getId())) {
                userObjectiveRepository.delete(existente);
            }
        }

        // Atualizar associações existentes ou criar novas
        for (UserObjectiveAssociationDTO associacao : associacoes) {

            UserObjective userObjective = existentes.stream()
                    .filter(uo -> uo.getObjective().getId()
                            .equals(associacao.objectiveId()))
                    .findFirst()
                    .orElse(null);

            if (userObjective != null) {
                userObjective.setPeso(associacao.peso());
            } else {
                Objective objective = objectiveRepository
                        .findById(associacao.objectiveId())
                        .orElseThrow(() -> new BusinessException(
                                "Objetivo não encontrado: "
                                        + associacao.objectiveId()
                        ));

                UserObjectiveId id = new UserObjectiveId();
                id.setUserId(user.getId());
                id.setObjectiveId(objective.getId());

                userObjective = new UserObjective();
                userObjective.setId(id);
                userObjective.setUser(user);
                userObjective.setObjective(objective);
                userObjective.setPeso(associacao.peso());
            }

            userObjectiveRepository.save(userObjective);
        }
    }



    private List<UserInterestAssociationDTO> buscarInteressesComPesos(
            Long userId
    ) {
        return userInterestRepository
                .findByUserIdWithInterest(userId)
                .stream()
                .map(ui -> new UserInterestAssociationDTO(
                        ui.getInterest().getId(),
                        ui.getPeso()
                ))
                .toList();
    }

    private List<UserObjectiveAssociationDTO> buscarObjetivosComPesos(
            Long userId
    ) {
        return userObjectiveRepository
                .findByUserId(userId)
                .stream()
                .map(uo -> new UserObjectiveAssociationDTO(
                        uo.getObjective().getId(),
                        uo.getPeso()
                ))
                .toList();
    }


}