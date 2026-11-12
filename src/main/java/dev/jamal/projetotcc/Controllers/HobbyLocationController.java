package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Location.HobbyLocationsResponseDTO;
import dev.jamal.projetotcc.Entities.RecommendationProfile;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Repository.HobbyRepository;
import dev.jamal.projetotcc.Repository.RecommendationProfileRepository;
import dev.jamal.projetotcc.Service.Location.HobbyLocationSearchService;
import dev.jamal.projetotcc.Service.Location.OverpassService;
import dev.jamal.projetotcc.Exception.OverpassUnavailableException;
import dev.jamal.projetotcc.Repository.HobbyLocationRuleRepository;
import dev.jamal.projetotcc.Enum.HobbyLocationSearchStatus;
import dev.jamal.projetotcc.Entities.HobbyLocationRule;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;


@RestController
@RequestMapping("/api/v1/hobbies")
@RequiredArgsConstructor
public class HobbyLocationController {

    private final OverpassService overpassService;
    private final HobbyLocationSearchService buscaLocais;
    private final RecommendationProfileRepository profileRepository;
    private final HobbyRepository hobbyRepository;
    private final ExecutorService locationExecutor;
    private final java.util.Set<Long> activeSearches = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final HobbyLocationRuleRepository ruleRepository;

    @GetMapping("/{hobbyId}/locations")
    public HobbyLocationsResponseDTO buscarLocais(
            @PathVariable Long hobbyId,
            @AuthenticationPrincipal User user
    ) {

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado."
            );
        }

        return buscarDados(hobbyId, user, false);
    }

    private HobbyLocationsResponseDTO buscarDados(Long hobbyId, User user, boolean stream) {
        RecommendationProfile perfil = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado."));
        String cidade = perfil.getCidade();
        String estado = perfil.getEstado();
        if (cidade == null || cidade.isBlank() || estado == null || estado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cadastre sua cidade e estado no perfil.");
        }

        if (!hobbyRepository.existsById(hobbyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hobby não encontrado.");
        }

        List<HobbyLocationRule> regras = ruleRepository.findAllWithConditionsByHobbyId(hobbyId);
        List<HobbyLocationRule> ativas = regras.stream().filter(HobbyLocationRule::isAtivo).toList();
        if (ativas.isEmpty()) {
            var status = regras.isEmpty() ? HobbyLocationSearchStatus.SEM_REGRAS : HobbyLocationSearchStatus.REGRAS_INATIVAS;
            Double coordenada = stream ? null : 0.0;
            return respostaSemConfiguracao(cidade, estado, coordenada, coordenada, status);
        }

        var resultado = buscaLocais.buscar(hobbyId, cidade, estado, ativas);
        return new HobbyLocationsResponseDTO(
                cidade, estado, resultado.latitude(), resultado.longitude(), resultado.locais(),
                resultado.consultadoEm().toString(), resultado.dadosDesatualizados()
        );
    }

    @GetMapping(
            value = "/{hobbyId}/locations/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter buscarLocaisStream(
            @PathVariable Long hobbyId,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado."
            );
        }

        SseEmitter emitter = new SseEmitter(130_000L);

        if (!activeSearches.add(user.getId())) {
            throw new dev.jamal.projetotcc.Exception.TooManyRequestsException();
        }
        try {
            locationExecutor.execute(() -> {
                try {
                    enviarProgresso(emitter, "Verificando perfil, localização e resultados disponíveis...");
                    overpassService.definirProgresso(mensagem -> enviarProgresso(emitter, mensagem));
                    var resultado = buscarDados(hobbyId, user, true);

                    enviarEvento(emitter, "resultado", resultado);
                    emitter.complete();

                } catch (Exception e) {
                    // Não expõe mensagens internas de serviços externos ao frontend.
                    String mensagem = mensagemErroBusca(e);
                    System.err.println("Falha na busca geográfica (hobby "
                            + hobbyId + "): " + e.getClass().getSimpleName()
                            + " - " + e.getMessage());
                    try {
                        enviarEvento(emitter, "erro", mensagem);
                        emitter.complete();
                    } catch (Exception falhaEnvio) {
                        emitter.completeWithError(falhaEnvio);
                    }
                } finally {
                    overpassService.limparProgresso();
                    activeSearches.remove(user.getId());
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            activeSearches.remove(user.getId());
            emitter.complete();
            throw new dev.jamal.projetotcc.Exception.TooManyRequestsException();
        }

        return emitter;
    }

    private String mensagemErroBusca(Exception erro) {
        if (erro instanceof OverpassUnavailableException) {
            return "O serviço de busca de locais está temporariamente indisponível. Tente novamente mais tarde.";
        }

        if (erro instanceof ResponseStatusException resposta) {
            if (resposta.getStatusCode().value() == 404) {
                return "Não foi possível encontrar o hobby ou o perfil necessário para a busca.";
            }
            if (resposta.getStatusCode().value() == 400) {
                return "Confira se sua cidade e seu estado estão cadastrados no perfil.";
            }
            if (resposta.getStatusCode().value() == 401 || resposta.getStatusCode().value() == 403) {
                return "Sua sessão não está autorizada. Entre novamente e tente realizar a busca.";
            }
        }

        return "Não foi possível concluir a busca de locais. Tente novamente.";
    }

    private void enviarEvento(
            SseEmitter emitter,
            String tipo,
            Object dados
    ) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(tipo)
                            .data(dados)
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível enviar a atualização da busca.",
                    e
            );
        }
    }


    private void enviarProgresso(SseEmitter emitter, String mensagem) {
        try {
            enviarEvento(emitter, "progresso", mensagem);
        } catch (IllegalStateException desconectado) {
            // A busca compartilhada ainda pode servir a outras pessoas.
        }
    }

    private HobbyLocationsResponseDTO respostaSemConfiguracao(
            String cidade,
            String estado,
            Double latitude,
            Double longitude,
            HobbyLocationSearchStatus status
    ) {
        String mensagem = switch (status) {
            case SEM_REGRAS ->
                    "Este hobby ainda não possui locais de prática configurados.";

            case REGRAS_INATIVAS ->
                    "A busca de locais está indisponível para este hobby.";

            default ->
                    throw new IllegalArgumentException(
                            "Status de configuração inválido."
                    );
        };

        return new HobbyLocationsResponseDTO(
                cidade,
                estado,
                latitude,
                longitude,
                List.of(),
                status,
                mensagem
        );
    }

}