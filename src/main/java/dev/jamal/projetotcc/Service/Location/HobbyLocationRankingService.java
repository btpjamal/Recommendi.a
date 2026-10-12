
package dev.jamal.projetotcc.Service.Location;

import dev.jamal.projetotcc.Entities.HobbyLocationCondition;
import dev.jamal.projetotcc.Entities.HobbyLocationRule;
import dev.jamal.projetotcc.Enum.HobbyLocationRelevance;
import dev.jamal.projetotcc.Enum.OsmMatchType;
import dev.jamal.projetotcc.Repository.HobbyLocationRuleRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HobbyLocationRankingService {


    public String classificar(List<HobbyLocationRule> regras, JsonNode tags) {


        HobbyLocationRelevance maiorRelevancia =
                HobbyLocationRelevance.COMPLEMENTAR;

        for (HobbyLocationRule regra : regras) {

            if (!corresponde(regra, tags)) {
                continue;
            }

            HobbyLocationRelevance relevancia =
                    regra.getRelevancia();

            if (relevancia == HobbyLocationRelevance.ALTA) {
                return HobbyLocationRelevance.ALTA.name();
            }

            if (relevancia == HobbyLocationRelevance.MEDIA) {
                maiorRelevancia = HobbyLocationRelevance.MEDIA;
            }
        }

        return maiorRelevancia.name();
    }

    private boolean corresponde(
            HobbyLocationRule regra,
            JsonNode tags
    ) {
        List<HobbyLocationCondition> condicoes =
                regra.getConditions();

        if (condicoes.isEmpty()) {
            return false;
        }

        for (HobbyLocationCondition condicao : condicoes) {

            String chave = condicao.getOsmKey();

            if (!tags.has(chave)) {
                return false;
            }

            if (condicao.getMatchType() == OsmMatchType.EXISTE) {
                continue;
            }

            if (condicao.getMatchType() != OsmMatchType.IGUAL) {
                return false;
            }

            String valorEsperado = condicao.getOsmValue();
            String valorEncontrado = tags.path(chave).asText("");

            if (valorEsperado == null
                    || !valorEsperado.equalsIgnoreCase(valorEncontrado)) {
                return false;
            }
        }

        return true;
    }

    public double calcularDistanciaKm(
            double latitudeOrigem,
            double longitudeOrigem,
            double latitudeDestino,
            double longitudeDestino
    ) {
        final double raioTerraKm = 6371.0;

        double diferencaLatitude = Math.toRadians(
                latitudeDestino - latitudeOrigem
        );

        double diferencaLongitude = Math.toRadians(
                longitudeDestino - longitudeOrigem
        );

        double a =
                Math.pow(Math.sin(diferencaLatitude / 2), 2)
                        + Math.cos(Math.toRadians(latitudeOrigem))
                        * Math.cos(Math.toRadians(latitudeDestino))
                        * Math.pow(Math.sin(diferencaLongitude / 2), 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(Math.max(0, 1 - a))
        );

        return Math.round(raioTerraKm * c * 10.0) / 10.0;
    }
}
