
package dev.jamal.projetotcc.Service.Location;

import dev.jamal.projetotcc.Entities.HobbyLocationCondition;
import dev.jamal.projetotcc.Entities.HobbyLocationRule;
import dev.jamal.projetotcc.Enum.OsmMatchType;
import dev.jamal.projetotcc.Repository.HobbyLocationRuleRepository;
import dev.jamal.projetotcc.Repository.HobbyRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class HobbyLocationQuery {

    private static final int RAIO_METROS = 12000;

    private static final Pattern OSM_TOKEN =
            Pattern.compile("[a-zA-Z0-9_:.-]+");

    private final HobbyRepository hobbyRepository;
    private final HobbyLocationRuleRepository ruleRepository;

    public Optional<String> construir(
            Long hobbyId,
            double latitude,
            double longitude
    ) {
        validarCoordenadas(latitude, longitude);

        if (hobbyId == null || !hobbyRepository.existsById(hobbyId)) {
            return Optional.empty();
        }

        List<HobbyLocationRule> regras =
                ruleRepository.findActiveWithConditions(hobbyId);

        if (regras.isEmpty()) {
            return Optional.empty();
        }

        StringBuilder filtros = new StringBuilder();

        for (HobbyLocationRule regra : regras) {

            if (regra.getConditions().isEmpty()) {
                continue;
            }

            StringBuilder filtro = new StringBuilder("nwr");

            for (HobbyLocationCondition condicao : regra.getConditions()) {

                String chave = validarToken(condicao.getOsmKey());

                if (condicao.getMatchType() == OsmMatchType.EXISTE) {

                    filtro.append("[\"")
                            .append(chave)
                            .append("\"]");

                } else if (condicao.getMatchType() == OsmMatchType.IGUAL) {

                    String valor = validarToken(condicao.getOsmValue());

                    filtro.append("[\"")
                            .append(chave)
                            .append("\"=\"")
                            .append(valor)
                            .append("\"]");

                } else {
                    throw new IllegalArgumentException(
                            "Tipo de correspondência OSM inválido."
                    );
                }
            }

            filtro.append("(around:")
                    .append(RAIO_METROS)
                    .append(",")
                    .append(latitude)
                    .append(",")
                    .append(longitude)
                    .append(");");

            filtros.append(filtro).append("\n");
        }

        if (filtros.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of("""
                [out:json][timeout:25];
                (
                %s
                );
                out center;
                """.formatted(filtros));
    }

    private String validarToken(String valor) {

        if (valor == null
                || !OSM_TOKEN.matcher(valor).matches()) {
            throw new IllegalArgumentException(
                    "Chave ou valor OSM inválido: " + valor
            );
        }

        return valor;
    }

    private void validarCoordenadas(
            double latitude,
            double longitude
    ) {
        if (!Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Coordenadas inválidas."
            );
        }
    }
}
