package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Location.HobbyLocationsResponseDTO;
import dev.jamal.projetotcc.Entities.RecommendationProfile;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Repository.HobbyRepository;
import dev.jamal.projetotcc.Repository.RecommendationProfileRepository;
import dev.jamal.projetotcc.Service.Location.GeocodingService;
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
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/hobbies")
@RequiredArgsConstructor
public class HobbyLocationController {

    private final OverpassService overpassService;
    private final GeocodingService geocodingService;
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

        RecommendationProfile perfil = profileRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Perfil não encontrado."
                ));

        String cidade = perfil.getCidade();
        String estado = perfil.getEstado();

        if (cidade == null || cidade.isBlank()
                || estado == null || estado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cadastre sua cidade e estado no perfil."
            );
        }

        var hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hobby não encontrado."
                ));

        HobbyLocationSearchStatus statusConfiguracao =
                verificarConfiguracaoGeografica(hobbyId);

        if (statusConfiguracao != null) {
            return respostaSemConfiguracao(
                    cidade,
                    estado,
                    0.0,
                    0.0,
                    statusConfiguracao
            );
        }

        GeocodingService.Coordinates coordenadas =
                geocodingService.buscarCoordenadas(cidade, estado);

        var locais = overpassService.buscarLocais(
                hobby.getId(),
                hobby.getNome(),
                coordenadas.latitude(),
                coordenadas.longitude()
        );

        return new HobbyLocationsResponseDTO(
                cidade,
                estado,
                coordenadas.latitude(),
                coordenadas.longitude(),
                locais
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

        SseEmitter emitter = new SseEmitter(120_000L);

        if (!activeSearches.add(user.getId())) {
            throw new dev.jamal.projetotcc.Exception.TooManyRequestsException();
        }
        try {
        locationExecutor.execute(() -> {
            try {
                enviarEvento(
                        emitter,
                        "progresso",
                        "Verificando perfil e localização..."
                );

                RecommendationProfile perfil = profileRepository
                        .findByUserId(user.getId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Perfil não encontrado."
                        ));

                String cidade = perfil.getCidade();
                String estado = perfil.getEstado();

                if (cidade == null || cidade.isBlank()
                        || estado == null || estado.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Cadastre sua cidade e estado no perfil."
                    );
                }

                var hobby = hobbyRepository.findById(hobbyId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Hobby não encontrado."
                        ));


                HobbyLocationSearchStatus statusConfiguracao =
                        verificarConfiguracaoGeografica(hobbyId);

                if (statusConfiguracao != null) {

                    enviarEvento(
                            emitter,
                            "progresso",
                            "Verificando disponibilidade da busca geográfica..."
                    );

                    HobbyLocationsResponseDTO resultado =
                            respostaSemConfiguracao(
                                    cidade,
                                    estado,
                                    null,
                                    null,
                                    statusConfiguracao
                            );

                    enviarEvento(emitter, "resultado", resultado);
                    emitter.complete();

                    return;
                }


                enviarEvento(
                        emitter,
                        "progresso",
                        "Localizando " + cidade + " - " + estado + "..."
                );

                var coordenadas =
                        geocodingService.buscarCoordenadas(cidade, estado);

                overpassService.definirProgresso(
                        mensagem -> enviarEvento(
                                emitter,
                                "progresso",
                                mensagem
                        )
                );

                var locais = overpassService.buscarLocais(
                        hobby.getId(),
                        hobby.getNome(),
                        coordenadas.latitude(),
                        coordenadas.longitude()
                );

                HobbyLocationsResponseDTO resultado =
                        new HobbyLocationsResponseDTO(
                                cidade,
                                estado,
                                coordenadas.latitude(),
                                coordenadas.longitude(),
                                locais
                        );

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


    private HobbyLocationSearchStatus verificarConfiguracaoGeografica(
            Long hobbyId
    ) {
        List<HobbyLocationRule> regras =
                ruleRepository.findByHobbyId(hobbyId);

        if (regras.isEmpty()) {
            return HobbyLocationSearchStatus.SEM_REGRAS;
        }

        boolean possuiRegraAtiva = regras.stream()
                .anyMatch(HobbyLocationRule::isAtivo);

        if (!possuiRegraAtiva) {
            return HobbyLocationSearchStatus.REGRAS_INATIVAS;
        }

        return null;
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