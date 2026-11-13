package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.CommunityDTO;
import dev.jamal.projetotcc.DTO.Social.CommunityDetailsDTO;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.CommunityType;
import dev.jamal.projetotcc.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final HobbyRepository hobbyRepository;
    private final UserRepository userRepository;
    private final RecommendationProfileRepository profileRepository;
    private final SocialLocationService locationService;
    private final CommunityChatLockRepository chatLocks;

    private void bloquearParticipacao(Long communityId, Long userId) {
        if (chatLocks.lockUser(userId).isEmpty()) throw new IllegalStateException("Usuário não encontrado.");
        if (chatLocks.lockCommunity(communityId).isEmpty()) throw new IllegalStateException("Comunidade não encontrada.");
    }

    @Transactional(readOnly = true)
    public List<CommunityDTO> listarMunicipais(Long userId) {
        var local = locationService.buscar(userId);
        locationService.validarEscopo(local, dev.jamal.projetotcc.Enum.SocialLocationScope.MUNICIPAL);
        return catalogo(userId, CommunityType.MUNICIPAL, local.estado(), local.cidade());
    }

    private List<CommunityDTO> catalogo(Long userId, CommunityType tipo, String estado, String cidade) {
        return communityRepository.catalogo(userId, tipo.name(), estado, cidade).stream()
                .map(row -> new CommunityDTO(row.getId(), row.getHobbyId(), row.getHobbyNome(),
                        tipo.name(), estado, row.getMembros(), row.getParticipando(), cidade))
                .toList();
    }

    @Transactional
    public CommunityDTO entrarPeloCatalogo(Long hobbyId, CommunityType tipo, Long userId) {
        if (chatLocks.lockUser(userId).isEmpty()) throw new IllegalStateException("Usuário não encontrado.");
        var local = locationService.buscar(userId);
        String estado = tipo == CommunityType.GERAL ? null : normalizarEstado(local.estado());
        String cidade = tipo == CommunityType.MUNICIPAL ? local.cidade() : null;
        if (tipo == CommunityType.MUNICIPAL) {
            locationService.validarEscopo(local, dev.jamal.projetotcc.Enum.SocialLocationScope.MUNICIPAL);
        }
        var existing = communityRepository.findByHobby_IdAndTipoAndEstadoAndCidade(hobbyId, tipo, estado, cidade);
        if (existing.isPresent()) return entrar(existing.get().getId(), userId);
        if (!hobbyRepository.findById(hobbyId).map(Hobby::isAtivo).orElse(false)) {
            throw new IllegalStateException("Hobby indisponível para novas comunidades.");
        }
        communityRepository.insertIfAbsent(hobbyId, tipo.name(), estado, cidade);
        var community = communityRepository.findByHobby_IdAndTipoAndEstadoAndCidade(hobbyId, tipo, estado, cidade)
                .orElseThrow(() -> new IllegalStateException("Comunidade indisponível."));
        return entrar(community.getId(), userId);
    }

    @Transactional
    public void garantirComunidadesGerais() {

        List<Hobby> hobbies =
                hobbyRepository.findAll();

        for (Hobby hobby : hobbies) {

            boolean existe =
                    communityRepository
                            .existsByHobby_IdAndTipo(
                                    hobby.getId(),
                                    CommunityType.GERAL
                            );

            if (!existe) {

                Community community =
                        new Community();

                community.setHobby(hobby);
                community.setTipo(
                        CommunityType.GERAL
                );

                // Comunidade geral não possui localização.
                community.setEstado(null);
                community.setCidade(null);

                communityRepository.insertIfAbsent(community.getHobby().getId(),
                        community.getTipo().name(), community.getEstado(), community.getCidade());
            }
        }
    }


    @Transactional(readOnly = true)
    public List<CommunityDTO> listarGerais(Long userId) {
        return catalogo(userId, CommunityType.GERAL, null, null);
    }

    private CommunityDTO converterParaDTO(
            Community community,
            Long userId
    ) {

        long membros =
                memberRepository
                        .countByCommunity_Id(
                                community.getId()
                        );

        boolean participando =
                memberRepository
                        .existsByCommunity_IdAndUser_Id(
                                community.getId(),
                                userId
                        );

        return new CommunityDTO(
                community.getId(),
                community.getHobby().getId(),
                community.getHobby().getNome(),
                community.getTipo().name(),
                community.getEstado(),
                membros,
                participando,
                community.getCidade()
        );
    }

    @Transactional
    public CommunityDTO entrar(
            Long communityId,
            Long userId
    ) {

        bloquearParticipacao(communityId, userId);

        Community community =
                communityRepository
                        .findById(communityId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Comunidade não encontrada."
                                )
                        );

        if (community.getTipo() == CommunityType.MUNICIPAL) {
            var local = locationService.buscar(userId);
            if (!local.temCidade() || !local.estado().equals(community.getEstado())
                    || !local.cidade().equals(community.getCidade())) {
                throw new IllegalStateException("Esta comunidade é destinada à cidade cadastrada no seu perfil.");
            }
        }

        memberRepository.insertIfAbsent(communityId, userId, java.time.LocalDateTime.now());

        return converterParaDTO(
                community,
                userId
        );
    }

    @Transactional
    public CommunityDTO sair(
            Long communityId,
            Long userId
    ) {

        bloquearParticipacao(communityId, userId);

        Community community =
                communityRepository
                        .findById(communityId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Comunidade não encontrada."
                                )
                        );

        memberRepository
                .findByCommunity_IdAndUser_Id(
                        communityId,
                        userId
                )
                .ifPresent(
                        memberRepository::delete
                );

        memberRepository.flush();

        return converterParaDTO(
                community,
                userId
        );
    }

    @Transactional
    public void garantirComunidadesRegionais(
            Long userId
    ) {

        RecommendationProfile profile =
                profileRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Perfil de recomendação não encontrado."
                                )
                        );

        String estado = normalizarEstado(
                profile.getEstado()
        );

        List<Hobby> hobbies =
                hobbyRepository.findAll();


        for (Hobby hobby : hobbies) {

            boolean existe =
                    communityRepository
                            .existsByHobby_IdAndTipoAndEstado(
                                    hobby.getId(),
                                    CommunityType.REGIONAL,
                                    estado
                            );


            if (!existe) {

                Community community =
                        new Community();

                community.setHobby(hobby);
                community.setTipo(
                        CommunityType.REGIONAL
                );

                community.setEstado(estado);

                // Nesta primeira versão regionalizamos
                // somente por estado.
                community.setCidade(null);

                communityRepository.insertIfAbsent(community.getHobby().getId(),
                        community.getTipo().name(), community.getEstado(), community.getCidade());
            }
        }
    }

    private String normalizarEstado(
            String estado
    ) {

        if (estado == null || estado.isBlank()) {
            throw new IllegalStateException(
                    "Estado não informado no perfil."
            );
        }

        String estadoNormalizado =
                estado.trim().toUpperCase();

        if (estadoNormalizado.length() != 2) {
            throw new IllegalStateException(
                    "Estado inválido no perfil."
            );
        }

        return estadoNormalizado;
    }

    @Transactional(readOnly = true)
    public List<CommunityDTO> listarRegionais(Long userId) {
        return catalogo(userId, CommunityType.REGIONAL, normalizarEstado(locationService.buscar(userId).estado()), null);
    }

    @Transactional(readOnly = true)
    public CommunityDetailsDTO buscarDetalhes(
            Long communityId,
            Long userId
    ) {

        Community community =
                communityRepository
                        .findById(communityId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Comunidade não encontrada."
                                )
                        );


        boolean participando =
                memberRepository
                        .existsByCommunity_IdAndUser_Id(
                                communityId,
                                userId
                        );


        if (!participando) {
            throw new IllegalStateException(
                    "Você precisa participar da comunidade para acessá-la."
            );
        }


        long membros =
                memberRepository
                        .countByCommunity_Id(
                                communityId
                        );


        return new CommunityDetailsDTO(
                community.getId(),
                community.getHobby().getId(),
                community.getHobby().getNome(),
                community.getTipo().name(),
                community.getEstado(),
                membros,
                community.getCidade()
        );
    }
}
