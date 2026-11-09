package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.SocialAffinityDTO;
import dev.jamal.projetotcc.DTO.Social.SocialHobbyDTO;
import dev.jamal.projetotcc.DTO.Social.SocialProfileDTO;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.RecommendationFeedbackType;
import dev.jamal.projetotcc.Enum.UserHobbyStatus;
import dev.jamal.projetotcc.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.jamal.projetotcc.Enum.SocialLocationScope;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialHubService {

    private final UserRepository userRepository;
    private final SocialProfileRepository socialProfileRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final UserRecommendationFeedbackRepository feedbackRepository;
    private final SocialProfileResumeService resumeService;
    private final SocialAffinityService affinityService;
    private final SocialLocationService locationService;
    private final SocialUserBlockRepository blockRepository;

    public SocialProfileDTO buscarPerfilPublicoPara(Long requesterId, Long outroId) {
        if (blockRepository.bloqueados(requesterId, outroId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Perfil social indisponível.");
        }
        var perfil = buscarPerfilPublico(outroId);
        String proximidade = socialProfileRepository.findByUser_Id(outroId)
                .map(p -> locationService.proximidade(locationService.buscar(requesterId), p)).orElse(null);
        return new SocialProfileDTO(perfil.userId(), perfil.nome(), perfil.resumo(), perfil.interesses(),
                perfil.praticando(), perfil.interessados(),
                affinityService.calcular(buscarPerfilPublico(requesterId), perfil), proximidade);
    }

    private record Candidato(SocialProfile perfil, String proximidade) {}

    public SocialProfileDTO buscarPerfilPublico(Long userId) {

        SocialProfile socialProfile =
                socialProfileRepository
                        .findByUser_Id(userId)
                        .filter(SocialProfile::isEnabled)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Perfil social não disponível"
                                )
                        );

        User user = socialProfile.getUser();

        List<String> interesses =
                userInterestRepository
                        .findByUserIdWithInterest(userId)
                        .stream()
                        .map(userInterest ->
                                userInterest
                                        .getInterest()
                                        .getNome()
                        )
                        .sorted()
                        .toList();


        List<UserHobby> userHobbies =
                userHobbyRepository
                        .findByUser_Id(userId);


        List<SocialHobbyDTO> praticando =
                userHobbies.stream()
                        .filter(userHobby ->
                                userHobby.getStatusAtual()
                                        == UserHobbyStatus.PRATICANDO
                        )
                        .map(userHobby ->
                                new SocialHobbyDTO(
                                        userHobby
                                                .getHobby()
                                                .getId(),

                                        userHobby
                                                .getHobby()
                                                .getNome()
                                )
                        )
                        .toList();


        List<SocialHobbyDTO> interessados =
                feedbackRepository
                        .findByUser_IdAndTipo(
                                userId,
                                RecommendationFeedbackType.INTERESSADO
                        )
                        .stream()

                        .filter(feedback -> {

                            Long hobbyId =
                                    feedback
                                            .getHobby()
                                            .getId();

                            return userHobbies.stream()
                                    .filter(userHobby ->
                                            userHobby
                                                    .getHobby()
                                                    .getId()
                                                    .equals(hobbyId)
                                    )
                                    .findFirst()
                                    .map(userHobby ->
                                            userHobby.getStatusAtual()
                                                    == UserHobbyStatus.INTERESSADO
                                    )
                                    .orElse(true);
                        })

                        .map(feedback ->
                                new SocialHobbyDTO(
                                        feedback
                                                .getHobby()
                                                .getId(),

                                        feedback
                                                .getHobby()
                                                .getNome()
                                )
                        )

                        .toList();


        String resumo =
                resumeService.construirResumo(
                        user.getNome(),
                        interesses,
                        praticando,
                        interessados
                );


        return new SocialProfileDTO(
                user.getId(),
                user.getNome(),
                resumo,
                interesses,
                praticando,
                interessados,
                null
        );
    }

    public List<SocialProfileDTO> buscarUsuarios(
            Long requesterId
    ) {
        return buscarUsuarios(requesterId, SocialLocationScope.GERAL);
    }

    public List<SocialProfileDTO> buscarUsuarios(Long requesterId, SocialLocationScope escopo) {

        var indisponiveis = blockRepository.buscarIdsIndisponiveis(requesterId);

        var localAtual = locationService.buscar(requesterId);
        locationService.validarEscopo(localAtual, escopo);

        SocialProfileDTO usuarioAtual =
                buscarPerfilPublico(requesterId);


        return socialProfileRepository
                .findByEnabledTrue()
                .stream()

                .filter(socialProfile ->
                        !socialProfile
                                .getUser()
                                .getId()
                                .equals(requesterId)
                )

                .filter(perfil -> !indisponiveis.contains(perfil.getUser().getId()))
                .map(perfil -> new Candidato(perfil, locationService.proximidade(localAtual, perfil)))
                .filter(candidato -> switch (escopo) {
                    case GERAL -> true;
                    case ESTADUAL -> candidato.proximidade() != null;
                    case MUNICIPAL -> "MESMA_CIDADE".equals(candidato.proximidade());
                })
                .map(candidato -> {
                    SocialProfile socialProfile = candidato.perfil();

                    SocialProfileDTO outroUsuario =
                            buscarPerfilPublico(
                                    socialProfile
                                            .getUser()
                                            .getId()
                            );


                    SocialAffinityDTO afinidade =
                            affinityService.calcular(
                                    usuarioAtual,
                                    outroUsuario
                            );


                    return new SocialProfileDTO(
                            outroUsuario.userId(),
                            outroUsuario.nome(),
                            outroUsuario.resumo(),
                            outroUsuario.interesses(),
                            outroUsuario.praticando(),
                            outroUsuario.interessados(),
                            afinidade,
                            candidato.proximidade()
                    );
                })

                .sorted(
                        Comparator.comparingInt(
                                (SocialProfileDTO perfil) ->
                                        perfil.afinidade().score()
                        ).reversed()
                )

                .toList();
    }
}
