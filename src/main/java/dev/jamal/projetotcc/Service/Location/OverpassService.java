package dev.jamal.projetotcc.Service.Location;

import dev.jamal.projetotcc.DTO.Location.HobbyLocationDTO;
import dev.jamal.projetotcc.Entities.HobbyLocationRule;
import dev.jamal.projetotcc.Repository.HobbyLocationRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import dev.jamal.projetotcc.Exception.OverpassUnavailableException;
import org.springframework.web.client.RestClientException;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Consumer;

import java.util.Comparator;



@Service
public class OverpassService {

    private final List<RestClient> servidores;

    private final HobbyLocationRankingService rankingService;

    private final HobbyLocationQuery hobbyLocationQuery;

    private final HobbyLocationRuleRepository ruleRepository;

    public OverpassService(HobbyLocationRankingService rankingService, HobbyLocationQuery hobbyLocationQuery, HobbyLocationRuleRepository hobbyLocationRuleRepository) {
        this.rankingService = rankingService;
        this.hobbyLocationQuery = hobbyLocationQuery;
        this.ruleRepository = hobbyLocationRuleRepository;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(12));

        this.servidores = List.of(
                criarCliente("https://overpass-api.de", requestFactory),
                criarCliente("https://overpass.private.coffee", requestFactory)
        );
    }

    private RestClient criarCliente(
            String baseUrl,
            JdkClientHttpRequestFactory requestFactory
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    private JsonNode consultarOverpass(String query) {

        final int maxTentativasPorServidor = 2;

        for (int i = 0; i < servidores.size(); i++) {

            RestClient cliente = servidores.get(i);

            String servidor = i == 0
                    ? "Principal"
                    : "Secundário";

            for (int tentativa = 1;
                 tentativa <= maxTentativasPorServidor;
                 tentativa++) {

                informarProgresso(
                        "Consultando servidor " + servidor.toLowerCase()
                                + " — tentativa " + tentativa
                                + " de " + maxTentativasPorServidor
                );

                try {
                    MultiValueMap<String, String> form =
                            new LinkedMultiValueMap<>();

                    form.add("data", query);

                    System.out.println(
                            "Overpass " + servidor
                                    + " - tentativa " + tentativa
                                    + "/" + maxTentativasPorServidor
                    );

                    JsonNode response = cliente.post()
                            .uri("/api/interpreter")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(form)
                            .retrieve()
                            .body(JsonNode.class);

                    if (response == null
                            || !response.path("elements").isArray()) {
                        throw new OverpassUnavailableException();
                    }

                    System.out.println(
                            "Consulta realizada com sucesso - "
                                    + servidor
                    );

                    return response;

                } catch (RestClientException e) {

                    boolean erroTransitorio =
                            e instanceof ResourceAccessException;


                    if (e instanceof RestClientResponseException responseError) {

                        int status = responseError.getStatusCode().value();

                        erroTransitorio =
                                status == 429 ||
                                        status == 502 ||
                                        status == 503 ||
                                        status == 504;
                    }

                    System.out.println(
                            "Falha na Overpass " + servidor
                                    + ": " + e.getMessage()
                    );

                    informarProgresso(
                            "O servidor " + servidor.toLowerCase()
                                    + " não conseguiu concluir a consulta."
                    );

                    if (!erroTransitorio) {
                        throw new OverpassUnavailableException();
                    }

                } catch (OverpassUnavailableException e) {

                    System.out.println(
                            "Resposta inválida da Overpass - "
                                    + servidor
                    );

                    informarProgresso(
                            "O servidor " + servidor.toLowerCase()
                                    + " retornou uma resposta inválida."
                    );
                }

                // Aguarda somente entre tentativas do mesmo servidor.
                if (tentativa < maxTentativasPorServidor) {

                    try {
                        Thread.sleep(1500);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new OverpassUnavailableException();
                    }
                }
            }
        }
        informarProgresso(
                "Consulta concluída! Preparando os locais encontrados..."
        );

        throw new OverpassUnavailableException();
    }

    @Cacheable(
            cacheNames = "hobbyLocations",
            key = "#hobbyId + ':' + #latitude + ':' + #longitude",
            sync = true
    )
    public List<HobbyLocationDTO> buscarLocais(
            Long hobbyId,
            String hobbyNome,
            double latitude,
            double longitude
    ) {

        String query = hobbyLocationQuery
                .construir(hobbyId, latitude, longitude)
                .orElse(null);

        if (query == null) {
            return List.of();
        }

        JsonNode response = consultarOverpass(query);

        List<HobbyLocationDTO> locais = new ArrayList<>();

        if (response == null) {
            return locais;
        }

        JsonNode elementos = response.path("elements");

        System.out.println(
                "Locais válidos após filtragem: " + locais.size()
        );

        List<HobbyLocationRule> regras =
                ruleRepository.findActiveWithConditions(hobbyId);

        for (JsonNode elemento : elementos) {

            JsonNode tags = elemento.path("tags");

            String nome = tags.path("name").asText("");

            if (nome.isBlank()) {
                continue;
            }

            JsonNode coordenadas = elemento.has("center")
                    ? elemento.path("center")
                    : elemento;

            if (!coordenadas.has("lat") || !coordenadas.has("lon")) {
                continue;
            }

            String endereco = construirEndereco(tags);

            String relevancia = rankingService.classificar(
                    regras,
                    tags
            );

            double latitudeLocal =
                    coordenadas.path("lat").asDouble();

            double longitudeLocal =
                    coordenadas.path("lon").asDouble();

            double distanciaKm = rankingService.calcularDistanciaKm(
                    latitude,
                    longitude,
                    latitudeLocal,
                    longitudeLocal
            );

            locais.add(new HobbyLocationDTO(
                    elemento.path("type").asText()
                            + "/" + elemento.path("id").asText(),
                    nome,
                    latitudeLocal,
                    longitudeLocal,
                    endereco,
                    relevancia,
                    distanciaKm
            ));
        }

        locais.sort(
                Comparator
                        .comparingInt(
                                (HobbyLocationDTO local) ->
                                        switch (local.relevancia()) {
                                            case "ALTA" -> 0;
                                            case "MEDIA" -> 1;
                                            default -> 2;
                                        }
                        )
                        .thenComparing(
                                HobbyLocationDTO::distanciaKm
                        )
        );


        System.out.println(
                "Locais válidos após filtragem: " + locais.size()
        );

        return locais;
    }

    private String construirEndereco(JsonNode tags) {

        String rua = tags.path("addr:street").asText("");
        String numero = tags.path("addr:housenumber").asText("");
        String bairro = tags.path("addr:suburb").asText("");

        List<String> partes = new ArrayList<>();

        if (!rua.isBlank()) {
            partes.add(rua);
        }

        if (!numero.isBlank()) {
            partes.add(numero);
        }

        if (!bairro.isBlank()) {
            partes.add(bairro);
        }

        return String.join(", ", partes);
    }

    private final ThreadLocal<Consumer<String>> progressoAtual =
            new ThreadLocal<>();

    public void definirProgresso(Consumer<String> callback) {
        progressoAtual.set(callback);
    }

    public void limparProgresso() {
        progressoAtual.remove();
    }

    private void informarProgresso(String mensagem) {
        Consumer<String> callback = progressoAtual.get();

        if (callback != null) {
            callback.accept(mensagem);
        }
    }
}