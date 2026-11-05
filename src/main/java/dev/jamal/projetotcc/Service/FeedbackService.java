package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.Feedback.FeedbackCreateRequestDTO;
import dev.jamal.projetotcc.DTO.Feedback.FeedbackResponseDTO;
import dev.jamal.projetotcc.Entities.Hobby;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Entities.UserHobby;
import dev.jamal.projetotcc.Entities.UserHobbyFeedback;
import dev.jamal.projetotcc.Enum.UserHobbyStatus;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Exception.ResourceNotFoundException;
import dev.jamal.projetotcc.Mapper.FeedbackMapper;
import dev.jamal.projetotcc.Repository.HobbyRepository;
import dev.jamal.projetotcc.Repository.UserHobbyFeedbackRepository;
import dev.jamal.projetotcc.Repository.UserHobbyRepository;
import dev.jamal.projetotcc.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    private final UserHobbyFeedbackRepository userHobbyFeedbackRepository;
    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final FeedbackMapper feedbackMapper;
    private final UserHobbyRepository userHobbyRepository;

    public FeedbackResponseDTO avaliar(Long userId, FeedbackCreateRequestDTO dto){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Hobby hobby= hobbyRepository.findById(dto.getHobbyId())
                .orElseThrow(() -> new ResourceNotFoundException("Hobby não encontrado"));

        UserHobby userHobby = userHobbyRepository
                .findByUser_IdAndHobby_Id(userId, dto.getHobbyId())
                .orElseThrow(() -> new BusinessException(
                        "Você precisa começar a praticar este hobby antes de avaliá-lo."
                ));

        UserHobbyStatus status = userHobby.getStatusAtual();

        if (status != UserHobbyStatus.PRATICANDO
                && status != UserHobbyStatus.PAUSADO) {

            throw new BusinessException(
                    "Apenas hobbies praticados ou pausados podem ser avaliados."
            );
        }

        UserHobbyFeedback feedback = userHobbyFeedbackRepository.
                findByUserIdAndHobbyId(userId, dto.getHobbyId())
                .orElseGet(() -> feedbackMapper.toEntity(dto,user, hobby));

        feedback.setRating(dto.getRating());

        UserHobbyFeedback salvo = userHobbyFeedbackRepository.save(feedback);

        return feedbackMapper.toResponseDTO(salvo);


    }

    @Transactional
    public List<FeedbackResponseDTO> listarPorUsuario(Long userId){
        return userHobbyFeedbackRepository.buscarComHobbyEUsuario(userId)
                .stream()
                .map(feedbackMapper::toResponseDTO)
                .toList();
    }
}
