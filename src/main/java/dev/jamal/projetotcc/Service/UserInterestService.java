package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.Interest.UserInterestCreateRequestDTO;
import dev.jamal.projetotcc.DTO.Interest.UserInterestResponseDTO;
import dev.jamal.projetotcc.Entities.Interest;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Entities.UserInterest;
import dev.jamal.projetotcc.Exception.ResourceNotFoundException;
import dev.jamal.projetotcc.Mapper.UserInterestMapper;
import dev.jamal.projetotcc.Repository.InterestRepository;
import dev.jamal.projetotcc.Repository.UserInterestRepository;
import dev.jamal.projetotcc.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
@Service
@RequiredArgsConstructor
@Transactional
public class UserInterestService {

    private final UserInterestRepository userInterestRepository;
    private final UserRepository userRepository;
    private final InterestRepository interestRepository;
    private final UserInterestMapper userInterestMapper;

    @Transactional
    public List<UserInterestResponseDTO> listarPorUsuario(Long userId) {
        return userInterestRepository.findByUserIdWithInterest(userId)
                .stream()
                .map(userInterestMapper::toResponseDTO)
                .toList();
    }

    public UserInterestResponseDTO adicionarOuAtualizar(
            Long userId,
            UserInterestCreateRequestDTO dto
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        Interest interest = interestRepository.findById(dto.getInterestId())
                .orElseThrow(() -> new ResourceNotFoundException("Interesse não encontrado"));


        UserInterest userInterest = userInterestMapper.toEntity(dto, user, interest);

        UserInterest salvo = userInterestRepository.save(userInterest);

        return userInterestMapper.toResponseDTO(salvo);
    }
}
