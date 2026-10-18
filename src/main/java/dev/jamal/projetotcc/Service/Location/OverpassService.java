package dev.jamal.projetotcc.Service.Location;

import dev.jamal.projetotcc.DTO.Location.HobbyLocationDTO;
import dev.jamal.projetotcc.Entities.HobbyLocationRule;
import dev.jamal.projetotcc.Repository.HobbyLocationRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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

    @Value("${app.overpass.user-agent:Recommendi.a/1.0 (academic hobby recommendation project)}")
    private String userAgent;

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

        requestFactory.setReadTimeout(Duration.ofSeconds(35));

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
        // Uma tentativa por servidor evita sobrecarregar as instâncias públicas.
        for (int i = 0; i < servidores.size(); i++) {
            RestClient cliente = servidores.get(i);
            String servidor = i == 0 ? "principal" : "secundário";

            informarProgresso("Consultando servidor " + servidor + "...");
            long inicio = System.nanoTime();

            try {
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("data", query);

                JsonNode response = cliente.post()
                        .uri("/api/interpreter")
                        .header(HttpHeaders.USER_AGENT, userAgent)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(JsonNode.class);

                if (response == null || !response.path("elements").isArray()) {
                    System.out.println("Overpass " + servidor + ": resposta inválida.");
                    informarProgresso("Resposta inválida do servidor " + servidor + ".");
                    continue;
                }

                long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
                System.out.println("Overpass " + servidor + ": sucesso em " + duracaoMs + " ms.");
                return response;

            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
                System.out.println("Overpass " + servidor + ": HTTP " + status
                        + " após " + duracaoMs + " ms.");

                if (status == 429) {
                    informarProgresso("Servidor " + servidor
                            + " com limite de requisições. Tentando alternativa...");
                } else if (status >= 500) {
                    informarProgresso("Servidor " + servidor
                            + " indisponível. Tentando alternativa...");
                } else {
                    // Outros 4xx geralmente indicam requisição inválida:
                    // repetir em outro servidor não corrige a consulta.
                    informarProgresso("A consulta foi rejeitada pelo servidor Overpass.");
                    throw new OverpassUnavailableException();
                }

            } catch (ResourceAccessException e) {
                long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
                System.out.println("Overpass " + servidor
                        + ": falha de conexão ou timeout após " + duracaoMs + " ms.");
                informarProgresso("Sem resposta do servidor " + servidor
                        + ". Tentando alternativa...");

            } catch (RestClientException e) {
                long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
                System.out.println("Overpass " + servidor
                        + ": erro de comunicação após " + duracaoMs + " ms ("
                        + e.getClass().getSimpleName() + ").");
                informarProgresso("Falha na comunicação com o servidor " + servidor + ".");
            }
        }

        informarProgresso("Não foi possível consultar os servidores de locais neste momento.");
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