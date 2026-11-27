package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.Recommendation.HobbyRecommendationDTO;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Exception.ResourceNotFoundException;
import dev.jamal.projetotcc.Mapper.RecommendationMapper;
import dev.jamal.projetotcc.Repository.*;
import dev.jamal.projetotcc.Service.recommendation.criteria.RecommendationCriterion;
import dev.jamal.projetotcc.Service.recommendation.model.CriterionResult;
import dev.jamal.projetotcc.Service.recommendation.model.HobbyAssociation;
import dev.jamal.projetotcc.Service.recommendation.model.HobbyRating;
import dev.jamal.projetotcc.Service.recommendation.model.RecommendationContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dev.jamal.projetotcc.DTO.Recommendation.CriterionScoreDTO;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RecommendationService {
    private final HobbyRepository hobbyRepository;
    private final RecommendationProfileRepository recommendationProfileRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserHobbyFeedbackRepository feedbackRepository;
    private final RecommendationMapper mapper;
    private final List<RecommendationCriterion> criterios;
    private final UserRecommendationFeedbackRepository userRecommendationFeedbackRepository;
    private final UserObjectiveRepository userObjectiveRepository;
    private final ObjectiveRepository objectiveRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final HobbyInterestRepository hobbyInterestRepository;
    private final HobbyObjectiveRepository hobbyObjectiveRepository;

    public List<HobbyRecommendationDTO> recomendar(Long userId) {
        RecommendationProfile p = recommendationProfileRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Complete o questionário inicial antes de receber recomendações."));
        List<HobbyRating> feedbacks = feedbackRepository.buscarAvaliacoesParaRecomendacao(userId);

        Set<Long> avaliados = feedbacks
                .stream()
                .map(HobbyRating::hobbyId)
                .collect(Collectors.toSet());

        List<UserRecommendationFeedback> feedbacksRecomendacao =
                userRecommendationFeedbackRepository
                        .findByUser_Id(userId);

        Set<Long> hobbiesDecididos = feedbacksRecomendacao.stream()
                .map(feedback -> feedback.getHobby().getId())
                .collect(Collectors.toSet());

        List<Hobby> candidatos = hobbyRepository.findAllEligibleForRecommendation()
                .stream()
                .filter(h -> !avaliados.contains(h.getId()))
                .filter(h -> !hobbiesDecididos.contains(h.getId()))
                .toList();

        if (candidatos.isEmpty()) {
            return List.of();
        }

        RecommendationContext contexto = carregarContexto(userId, p, candidatos, feedbacks, feedbacksRecomendacao);
        return candidatos.stream()
                .map(h -> pontuarHobby(h, contexto))
                .sorted(Comparator.comparingDouble(HobbyPontuado::score).reversed())
                .limit(20)
                .map(this::montarRecomendacao)
                .toList();

    }

    public HobbyRecommendationDTO calcularRecomendacao(
            Long userId,
            Long hobbyId
    ) {
        RecommendationProfile perfil = buscarPerfil(userId);

        Hobby hobby =
                hobbyRepository
                        .findEligibleById(hobbyId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Hobby não encontrado ou indisponível para recomendação."
                                )
                        );

        return avaliarHobbyElegivel(userId, hobby, perfil).recomendacao();
    }

    RecommendationProfile buscarPerfil(Long userId) {
        return recommendationProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Perfil de recomendação não encontrado."));
    }

    public AvaliacaoEmLote pontuarHobbiesElegiveis(Long userId, RecommendationProfile perfil, List<Long> ids) {
        Map<Long, Hobby> hobbies = hobbyRepository.findEligibleByIds(ids).stream()
                .collect(Collectors.toMap(Hobby::getId, hobby -> hobby));
        if (!hobbies.keySet().containsAll(ids)) {
            throw new BusinessException("Hobby não encontrado ou indisponível para recomendação.");
        }

        List<Hobby> candidatos = ids.stream().map(hobbies::get).toList();
        RecommendationContext contexto = carregarContexto(userId, perfil, candidatos);
        List<PontuacaoHobby> pontuacoes = candidatos.stream()
                .map(hobby -> new PontuacaoHobby(hobby, pontuarHobby(hobby, contexto).score()))
                .toList();
        return new AvaliacaoEmLote(contexto, pontuacoes);
    }

    public record PontuacaoHobby(Hobby hobby, double score) {}

    public record AvaliacaoEmLote(RecommendationContext contexto, List<PontuacaoHobby> pontuacoes) {}

    public Avaliacao avaliarHobbyElegivel(Long userId, Hobby hobby, RecommendationProfile perfil) {
        RecommendationContext contexto = carregarContexto(userId, perfil, List.of(hobby));
        UserHobby vinculo = contexto.hobbiesDoUsuario().stream()
                .filter(item -> item.getHobby().getId().equals(hobby.getId()))
                .findFirst()
                .orElse(null);
        return new Avaliacao(avaliarHobby(hobby, contexto), contexto, vinculo);
    }

    public record Avaliacao(
            HobbyRecommendationDTO recomendacao, RecommendationContext contexto, UserHobby vinculo
    ) {}

    RecommendationContext carregarContexto(Long userId, RecommendationProfile perfil, List<Hobby> candidatos) {
        return carregarContexto(userId, perfil, candidatos,
                feedbackRepository.buscarAvaliacoesParaRecomendacao(userId),
                userRecommendationFeedbackRepository.findByUser_Id(userId));
    }

    private RecommendationContext carregarContexto(
            Long userId,
            RecommendationProfile perfil,
            List<Hobby> candidatos,
            List<HobbyRating> avaliacoes,
            List<UserRecommendationFeedback> decisoes
    ) {
        List<UserInterest> interesses = userInterestRepository.findByUserIdWithInterest(userId);
        List<UserObjective> objetivos = userObjectiveRepository.findByUserId(userId);
        List<UserHobby> hobbiesDoUsuario = userHobbyRepository.findByUser_Id(userId);
        List<Long> objetivosIds = objetivos.stream().map(objetivo -> objetivo.getId().getObjectiveId()).toList();
        List<Long> historicoIds = hobbiesDoUsuario.stream().map(hobby -> hobby.getHobby().getId()).toList();

        // Preserve a ordem das consultas originais para motivos e empates de afinidade.
        Map<Long, String> nomesDosObjetivos = objectiveRepository.findAllById(objetivosIds).stream()
                .collect(Collectors.toMap(Objective::getId, Objective::getNome));
        Map<Long, String> nomesDosHobbiesDoUsuario = historicoIds.isEmpty() ? Map.of()
                : hobbyRepository.findNomesByIds(historicoIds).stream()
                        .collect(Collectors.toMap(HobbyRepository.IdNome::getId, HobbyRepository.IdNome::getNome));

        List<Long> candidatosIds = candidatos.stream().map(Hobby::getId).toList();
        Set<Long> hobbiesComInteresses = new LinkedHashSet<>(candidatosIds);
        decisoes.forEach(decisao -> hobbiesComInteresses.add(decisao.getHobby().getId()));
        hobbiesComInteresses.addAll(historicoIds);

        Map<Long, List<HobbyAssociation>> interessesPorHobby = interesses.isEmpty() && decisoes.isEmpty() && hobbiesDoUsuario.isEmpty()
                ? Map.of()
                : hobbyInterestRepository.findAssociationsByHobbyIds(hobbiesComInteresses)
                        .stream().collect(Collectors.groupingBy(HobbyAssociation::hobbyId));
        Map<Long, List<HobbyAssociation>> objetivosPorHobby = objetivos.isEmpty()
                ? Map.of()
                : hobbyObjectiveRepository.findAssociationsByHobbyIds(candidatosIds)
                        .stream().collect(Collectors.groupingBy(HobbyAssociation::hobbyId));

        return new RecommendationContext(
                perfil, interesses, objetivos, avaliacoes, decisoes, hobbiesDoUsuario,
                nomesDosHobbiesDoUsuario, nomesDosObjetivos,
                interessesPorHobby, objetivosPorHobby
        );
    }

    HobbyRecommendationDTO avaliarHobby(Hobby hobby, RecommendationContext contexto) {
        return montarRecomendacao(pontuarHobby(hobby, contexto));
    }

    private record HobbyPontuado(Hobby hobby, double score, List<CriterionResult> resultados) {}

    private HobbyPontuado pontuarHobby(Hobby hobby, RecommendationContext contexto) {
        List<CriterionResult> resultados =
                criterios.stream()
                        .map(criterio -> criterio.avaliar(hobby, contexto))
                        .toList();

        if (log.isDebugEnabled()) {
            for (int i = 0; i < criterios.size(); i++) {
                log.debug("Hobby: {} | Critério: {} | Pontos: {}",
                        hobby.getNome(), criterios.get(i).getClass().getSimpleName(), resultados.get(i).pontos());
            }
        }

        double score =
                resultados.stream()
                        .mapToDouble(CriterionResult::pontos)
                        .sum();

        return new HobbyPontuado(hobby, Math.max(0, Math.round(score * 10.0) / 10.0), resultados);
    }

    private HobbyRecommendationDTO montarRecomendacao(HobbyPontuado pontuado) {
        List<CriterionResult> resultados = pontuado.resultados();
        List<String> motivos =
                resultados.stream()
                        .flatMap(resultado ->
                                resultado.motivos().stream()
                        )
                        .distinct()
                        .toList();

        List<String> alertas =
                resultados.stream()
                        .flatMap(resultado ->
                                resultado.alertas().stream()
                        )
                        .distinct()
                        .toList();

        List<CriterionScoreDTO> pontuacoesCriterios =
                new ArrayList<>();

        for (int i = 0; i < criterios.size(); i++) {
            pontuacoesCriterios.add(
                    new CriterionScoreDTO(
                            criterios.get(i).getClass().getSimpleName(),
                            resultados.get(i).pontos()
                    )
            );
        }

        return mapper.toDTO(
                pontuado.hobby(),
                pontuado.score(),
                motivos,
                alertas,
                pontuacoesCriterios
        );
    }
}
