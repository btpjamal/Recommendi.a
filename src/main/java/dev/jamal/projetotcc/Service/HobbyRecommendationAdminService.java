package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.Hobby.HobbyInterestAssociationDTO;
import dev.jamal.projetotcc.DTO.Hobby.HobbyObjectiveAssociationDTO;
import dev.jamal.projetotcc.DTO.Hobby.HobbyRecommendationAssociationsDTO;
import dev.jamal.projetotcc.Exception.ResourceNotFoundException;
import dev.jamal.projetotcc.Repository.HobbyInterestRepository;
import dev.jamal.projetotcc.Repository.HobbyObjectiveRepository;
import dev.jamal.projetotcc.Repository.HobbyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HobbyRecommendationAdminService {

    private final HobbyRepository hobbyRepository;
    private final HobbyInterestRepository interestRepository;
    private final HobbyObjectiveRepository objectiveRepository;

    @Transactional(readOnly = true)
    public HobbyRecommendationAssociationsDTO buscar(Long hobbyId) {
        if (!hobbyRepository.existsById(hobbyId)) {
            throw new ResourceNotFoundException("Hobby não encontrado.");
        }

        var interesses = interestRepository.findByHobbyId(hobbyId).stream()
                .map(item -> new HobbyInterestAssociationDTO(
                        item.getId().getInterestId(), item.getPeso()))
                .toList();
        var objetivos = objectiveRepository.findByHobbyId(hobbyId).stream()
                .map(item -> new HobbyObjectiveAssociationDTO(
                        item.getId().getObjectiveId(), item.getPeso()))
                .toList();

        return new HobbyRecommendationAssociationsDTO(interesses, objetivos);
    }
}