package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.UserHobby.UserHobbyResponseDTO;
import dev.jamal.projetotcc.Entities.UserHobby;
import dev.jamal.projetotcc.Enum.NivelExperiencia;
import dev.jamal.projetotcc.Enum.UserHobbyStatus;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Repository.UserHobbyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
@Service
@RequiredArgsConstructor
public class UserHobbyService {

    private final UserHobbyRepository userHobbyRepository;

    @Transactional
    public UserHobbyResponseDTO atualizarNivel(
            Long userId,
            Long hobbyId,
            NivelExperiencia nivelAtual
    ) {

        UserHobby userHobby =
                userHobbyRepository
                        .findByUser_IdAndHobby_Id(userId, hobbyId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Hobby não encontrado para este usuário"
                                )
                        );

        userHobby.setNivelAtual(nivelAtual);

        UserHobby salvo =
                userHobbyRepository.save(userHobby);

        return new UserHobbyResponseDTO(
                salvo.getHobby().getId(),
                salvo.getHobby().getNome(),
                salvo.getNivelAtual(),
                salvo.getStatusAtual()
        );
    }

    @Transactional
    public UserHobbyResponseDTO atualizarStatus(
            Long userId,
            Long hobbyId,
            UserHobbyStatus statusAtual
    ) {

        UserHobby userHobby =
                userHobbyRepository
                        .findByUser_IdAndHobby_Id(
                                userId,
                                hobbyId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Hobby não encontrado para este usuário."
                                )
                        );

        userHobby.setStatusAtual(statusAtual);

        UserHobby salvo =
                userHobbyRepository.save(userHobby);

        return new UserHobbyResponseDTO(
                salvo.getHobby().getId(),
                salvo.getHobby().getNome(),
                salvo.getNivelAtual(),
                salvo.getStatusAtual()
        );
    }
}
