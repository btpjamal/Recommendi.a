package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Location.HobbyLocationsResponseDTO;
import dev.jamal.projetotcc.Entities.RecommendationProfile;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Repository.HobbyRepository;
import dev.jamal.projetotcc.Repository.RecommendationProfileRepository;
import dev.jamal.projetotcc.Service.Location.GeocodingService;
import dev.jamal.projetotcc.Service.Location.OverpassService;

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
    private final ExecutorService executor =
            Executors.newFixedThreadPool(4);

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

        executor.execute(() -> {
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
                try {
                    enviarEvento(
                            emitter,
                            "erro",
                            "Não foi possível concluir a busca. Tente novamente."
                    );
                    emitter.complete();
                } catch (Exception ignored) {
                    emitter.completeWithError(e);
                }
            } finally {
                overpassService.limparProgresso();
            }
        });

        return emitter;
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
}