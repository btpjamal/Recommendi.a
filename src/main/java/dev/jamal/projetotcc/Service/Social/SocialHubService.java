package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.SocialAffinityDTO;
import dev.jamal.projetotcc.DTO.Social.SocialHobbyDTO;
import dev.jamal.projetotcc.DTO.Social.SocialProfileDTO;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.RecommendationFeedbackType;
import dev.jamal.projetotcc.Enum.UserHobbyStatus;
import dev.jamal.projetotcc.Enum.SocialVisibility;
import dev.jamal.projetotcc.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.jamal.projetotcc.Enum.SocialLocationScope;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Predicate;
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
    private final SocialContactRepository contactRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Os vínculos aceitos e sem bloqueio já foram selecionados pela consulta da caixa.
    Map<Long, SocialProfileDTO> buscarPerfisDeContatosAceitos(Long requesterId, List<Long> outrosIds) {
        if (outrosIds.isEmpty()) return Map.of();

        var ids = new ArrayList<>(outrosIds);
        ids.add(requesterId);
        var perfis = new HashMap<Long, SocialProfileSummary>();
        socialProfileRepository.findPublicSummaries(ids).stream()
                .filter(p -> SocialProfileService.maiorDeIdade(p.nascimento()))
                .forEach(p -> perfis.put(p.userId(), p));
        var solicitante = perfis.remove(requesterId);
        if (solicitante == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil social não disponível");
        if (perfis.isEmpty()) return Map.of();

        ids.retainAll(perfis.keySet());
        ids.add(requesterId);
        var interesses = new HashMap<Long, List<String>>();
        entityManager.createQuery("""
                select i.user.id, i.interest.nome from UserInterest i
                where i.user.id in :ids
                """, Object[].class).setParameter("ids", ids).getResultList().forEach(row ->
                interesses.computeIfAbsent((Long) row[0], id -> new ArrayList<>()).add((String) row[1]));
        var praticando = carregarHobbies(ids, """
                select h.user.id, h.hobby.id, h.hobby.nome from UserHobby h
                where h.user.id in :ids and h.statusAtual = dev.jamal.projetotcc.Enum.UserHobbyStatus.PRATICANDO
                order by h.id
                """);
        var interessados = carregarHobbies(ids, """
                select f.user.id, f.hobby.id, f.hobby.nome from UserRecommendationFeedback f
                where f.user.id in :ids and f.tipo = dev.jamal.projetotcc.Enum.RecommendationFeedbackType.INTERESSADO
                  and not exists (
                      select h.id from UserHobby h where h.user.id = f.user.id and h.hobby.id = f.hobby.id
                        and h.statusAtual <> dev.jamal.projetotcc.Enum.UserHobbyStatus.INTERESSADO)
                order by f.id
                """);
        var locais = new HashMap<Long, SocialLocationService.Localizacao>();
        var idsComRegiao = perfis.values().stream().filter(SocialProfileSummary::compartilharRegiao)
                .map(SocialProfileSummary::userId).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (!idsComRegiao.isEmpty()) {
            idsComRegiao.add(requesterId);
            entityManager.createQuery("""
                    select p.user.id, p.estado, p.cidade from RecommendationProfile p where p.user.id in :ids
                    """, Object[].class).setParameter("ids", idsComRegiao).getResultList().forEach(row ->
                    locais.put((Long) row[0], new SocialLocationService.Localizacao(
                            SocialLocationService.normalizarEstado((String) row[1]),
                            SocialLocationService.normalizarCidade((String) row[2]))));
        }
        var localPadrao = new SocialLocationService.Localizacao(null, null);
        var localAtual = locais.getOrDefault(requesterId, localPadrao);
        var perfilAtual = montarContatoAceito(solicitante, interesses, praticando, interessados);
        var resultado = new HashMap<Long, SocialProfileDTO>();
        perfis.forEach((id, perfil) -> {
            var visivel = montarContatoAceito(perfil, interesses, praticando, interessados);
            var proximidade = SocialLocationService.comparar(localAtual, locais.getOrDefault(id, localPadrao));
            resultado.put(id, comAfinidade(visivel, perfilAtual, proximidade));
        });
        return resultado;
    }

    private Map<Long, List<SocialHobbyDTO>> carregarHobbies(List<Long> ids, String jpql) {
        var hobbies = new HashMap<Long, List<SocialHobbyDTO>>();
        for (var row : entityManager.createQuery(jpql, Object[].class).setParameter("ids", ids).getResultList()) {
            hobbies.computeIfAbsent((Long) row[0], id -> new ArrayList<>())
                    .add(new SocialHobbyDTO((Long) row[1], (String) row[2]));
        }
        return hobbies;
    }

    private SocialProfileDTO montarContatoAceito(SocialProfileSummary perfil, Map<Long, List<String>> interesses,
            Map<Long, List<SocialHobbyDTO>> praticando, Map<Long, List<SocialHobbyDTO>> interessados) {
        Long id = perfil.userId();
        return montarVisivel(perfil, interesses.getOrDefault(id, List.of()).stream().sorted().toList(),
                praticando.getOrDefault(id, List.of()), interessados.getOrDefault(id, List.of()),
                regra -> regra == SocialVisibility.PARTICIPANTES || regra == SocialVisibility.CONTATOS);
    }

    public SocialProfileDTO buscarPerfilPublicoPara(Long requesterId, Long outroId) {
        if (blockRepository.bloqueados(requesterId, outroId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Perfil social indisponível.");
        }
        var outro = buscarResumo(outroId);
        var perfil = visivelPara(requesterId, outro);
        String proximidade = locationService.proximidade(locationService.buscar(requesterId),
                outro.userId(), outro.compartilharRegiao());
        var solicitante = requesterId.equals(outroId) ? outro : buscarResumo(requesterId);
        return comAfinidade(perfil, visivelPara(outroId, solicitante), proximidade);
    }

    private SocialProfileDTO comAfinidade(SocialProfileDTO perfil, SocialProfileDTO solicitante, String proximidade) {
        return new SocialProfileDTO(perfil.userId(), perfil.nome(), perfil.resumo(), perfil.interesses(),
                perfil.praticando(), perfil.interessados(),
                affinityService.calcular(solicitante, perfil), proximidade,
                perfil.biografia(), perfil.possuiFoto());
    }

    public boolean podeVer(dev.jamal.projetotcc.Enum.SocialVisibility regra, Long viewer, Long owner) {
        if (viewer.equals(owner)) return true;
        if (regra == dev.jamal.projetotcc.Enum.SocialVisibility.PARTICIPANTES) return true;
        return regra == dev.jamal.projetotcc.Enum.SocialVisibility.CONTATOS && contactRepository
                .findByLowUserIdAndHighUserId(Math.min(viewer, owner), Math.max(viewer, owner))
                .map(c -> c.getStatus() == dev.jamal.projetotcc.Enum.SocialContactStatus.ACEITA).orElse(false);
    }

    private SocialProfileSummary buscarResumo(Long userId) {
        return socialProfileRepository.findPublicSummary(userId)
                .filter(p -> SocialProfileService.maiorDeIdade(p.nascimento()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil social não disponível"));
    }

    private record Candidato(SocialProfileSummary perfil, String proximidade) {}

    private SocialProfileDTO visivelPara(Long viewer, SocialProfileSummary perfil) {
        Long userId = perfil.userId();
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


        return montarVisivel(perfil, interesses, praticando, interessados, regra -> podeVer(regra, viewer, userId));
    }

    private SocialProfileDTO montarVisivel(SocialProfileSummary perfil, List<String> interesses,
            List<SocialHobbyDTO> praticando, List<SocialHobbyDTO> interessados, Predicate<SocialVisibility> visivel) {
        if (!visivel.test(perfil.interessesVisibilidade())) interesses = List.of();
        if (!visivel.test(perfil.hobbiesVisibilidade())) {
            praticando = List.of();
            interessados = List.of();
        }

        return new SocialProfileDTO(perfil.userId(), perfil.nome(),
                resumeService.construirResumo(perfil.nome(), interesses, praticando, interessados),
                interesses, praticando, interessados, null, null,
                visivel.test(perfil.biografiaVisibilidade()) ? perfil.biografia() : null,
                perfil.possuiFoto() && visivel.test(perfil.fotoVisibilidade()));
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

        buscarResumo(requesterId);

        return socialProfileRepository.findEnabledSummaries().stream()
                .filter(perfil -> !perfil.userId().equals(requesterId))
                .filter(perfil -> SocialProfileService.maiorDeIdade(perfil.nascimento()))
                .filter(perfil -> !indisponiveis.contains(perfil.userId()))
                .map(perfil -> new Candidato(perfil,
                        locationService.proximidade(localAtual, perfil.userId(), perfil.compartilharRegiao())))
                .filter(candidato -> switch (escopo) {
                    case GERAL -> true;
                    case ESTADUAL -> candidato.proximidade() != null;
                    case MUNICIPAL -> "MESMA_CIDADE".equals(candidato.proximidade());
                })
                .map(candidato -> buscarPerfilPublicoPara(requesterId, candidato.perfil().userId()))

                .sorted(
                        Comparator.comparingInt(
                                (SocialProfileDTO perfil) ->
                                        perfil.afinidade().score()
                        ).reversed()
                )

                .toList();
    }
}


