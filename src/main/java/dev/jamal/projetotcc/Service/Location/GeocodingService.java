package dev.jamal.projetotcc.Service.Location;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeocodingService {

    public record Coordinates(
            double latitude,
            double longitude
    ) {}

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://nominatim.openstreetmap.org")
            .defaultHeader(
                    "User-Agent",
                    "RecommendiA-TCC/1.0 (contato: recommendia.dev@gmail.com)"
            )
            .requestFactory(requestFactory())
            .build();

    private static org.springframework.http.client.JdkClientHttpRequestFactory requestFactory() {
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build());
        factory.setReadTimeout(java.time.Duration.ofSeconds(10));
        return factory;
    }

    private final com.github.benmanes.caffeine.cache.Cache<String, Coordinates> cache =
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder().maximumSize(1000)
                    .expireAfterWrite(java.time.Duration.ofHours(24)).build();

    public Coordinates buscarCoordenadas(String cidade, String estado) {

        String chave = cidade.trim().toLowerCase() + "|" +
                estado.trim().toUpperCase();

        Coordinates existente = cache.getIfPresent(chave);

        if (existente != null) {
            return existente;
        }

        synchronized (this) {

            existente = cache.getIfPresent(chave);

            if (existente != null) {
                return existente;
            }

            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("city", cidade)
                            .queryParam("state", estado)
                            .queryParam("country", "Brasil")
                            .queryParam("countrycodes", "br")
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 1)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.isArray()
                    || response.isEmpty()) {
                throw new IllegalStateException(
                        "Não foi possível localizar a cidade cadastrada."
                );
            }

            JsonNode local = response.get(0);

            Coordinates coordenadas = new Coordinates(
                    Double.parseDouble(local.path("lat").asText()),
                    Double.parseDouble(local.path("lon").asText())
            );

            cache.put(chave, coordenadas);

            return coordenadas;
        }
    }
}