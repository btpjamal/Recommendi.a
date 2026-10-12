
package dev.jamal.projetotcc.Service.Location;

import dev.jamal.projetotcc.Entities.Hobby;
import dev.jamal.projetotcc.Entities.HobbyLocationCondition;
import dev.jamal.projetotcc.Entities.HobbyLocationRule;
import dev.jamal.projetotcc.Enum.HobbyLocationRelevance;
import dev.jamal.projetotcc.Enum.OsmMatchType;
import dev.jamal.projetotcc.Repository.HobbyLocationRuleRepository;
import dev.jamal.projetotcc.Repository.HobbyRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HobbyLocationRuleMigrationService {

    private final HobbyRepository hobbyRepository;
    private final HobbyLocationRuleRepository ruleRepository;

    @Transactional
    public void migrarRegrasIniciais() {

        cadastrar(
                "Futebol",
                HobbyLocationRelevance.ALTA,
                igual("sport", "soccer")
        );

        cadastrar(
                "Futebol",
                HobbyLocationRelevance.COMPLEMENTAR,
                igual("leisure", "pitch"),
                igual("sport", "multi")
        );

        cadastrar(
                "Boliche",
                HobbyLocationRelevance.ALTA,
                igual("leisure", "bowling_alley")
        );

        cadastrar(
                "Boliche",
                HobbyLocationRelevance.ALTA,
                igual("sport", "bowling")
        );


        // CORRIDA
        cadastrar("Corrida", HobbyLocationRelevance.COMPLEMENTAR,
                igual("leisure", "park"));
        cadastrar("Corrida", HobbyLocationRelevance.ALTA,
                igual("sport", "athletics"));

        // CICLISMO
        cadastrar("Ciclismo", HobbyLocationRelevance.ALTA,
                igual("sport", "cycling"));
        cadastrar("Ciclismo", HobbyLocationRelevance.COMPLEMENTAR,
                igual("leisure", "park"));

        // NATAÇÃO
        cadastrar("Natação", HobbyLocationRelevance.ALTA,
                igual("sport", "swimming"));
        cadastrar("Natação", HobbyLocationRelevance.ALTA,
                igual("leisure", "swimming_pool"));

        // FOTOGRAFIA
        cadastrar("Fotografia", HobbyLocationRelevance.ALTA,
                igual("tourism", "viewpoint"));
        cadastrar("Fotografia", HobbyLocationRelevance.ALTA,
                igual("tourism", "attraction"));

        // DESENHO E PINTURA
        for (String hobby : List.of("Desenho", "Pintura")) {
            cadastrar(hobby, HobbyLocationRelevance.COMPLEMENTAR,
                    igual("shop", "art"));
            cadastrar(hobby, HobbyLocationRelevance.ALTA,
                    igual("amenity", "arts_centre"));
        }

        // VIOLÃO
        cadastrar("Violão", HobbyLocationRelevance.COMPLEMENTAR,
                igual("shop", "musical_instrument"));
        cadastrar("Violão", HobbyLocationRelevance.ALTA,
                igual("music_school", "yes"));

        // XADREZ
        cadastrar("Xadrez", HobbyLocationRelevance.ALTA,
                igual("sport", "chess"));
        cadastrar("Xadrez", HobbyLocationRelevance.COMPLEMENTAR,
                igual("amenity", "library"));

        // LEITURA
        cadastrar("Leitura", HobbyLocationRelevance.ALTA,
                igual("amenity", "library"));
        cadastrar("Leitura", HobbyLocationRelevance.COMPLEMENTAR,
                igual("shop", "books"));

        // ESCRITA
        cadastrar("Escrita", HobbyLocationRelevance.ALTA,
                igual("amenity", "library"));
        cadastrar("Escrita", HobbyLocationRelevance.COMPLEMENTAR,
                igual("amenity", "arts_centre"));

        // ESTUDO DE IDIOMAS
        cadastrar("Estudo de idiomas", HobbyLocationRelevance.ALTA,
                existe("language:teaching"));
        cadastrar("Estudo de idiomas", HobbyLocationRelevance.ALTA,
                igual("amenity", "language_school"));

        // TEATRO
        cadastrar("Teatro", HobbyLocationRelevance.ALTA,
                igual("amenity", "theatre"));
        cadastrar("Teatro", HobbyLocationRelevance.ALTA,
                existe("theatre:genre"));

        // DANÇA
        cadastrar("Dança", HobbyLocationRelevance.ALTA,
                igual("dance:teaching", "yes"));
        cadastrar("Dança", HobbyLocationRelevance.ALTA,
                igual("sport", "dance"));

        // VOLUNTARIADO
        cadastrar("Voluntariado", HobbyLocationRelevance.ALTA,
                igual("office", "ngo"));
        cadastrar("Voluntariado", HobbyLocationRelevance.COMPLEMENTAR,
                igual("office", "association"));

        // CLUBE DE JOGOS
        cadastrar("Clube de jogos", HobbyLocationRelevance.ALTA,
                igual("leisure", "board_games"));
        cadastrar("Clube de jogos", HobbyLocationRelevance.COMPLEMENTAR,
                igual("shop", "games"));

        // PROGRAMAÇÃO CRIATIVA
        cadastrar("Programação criativa",
                HobbyLocationRelevance.COMPLEMENTAR,
                igual("amenity", "library"));
        cadastrar("Programação criativa",
                HobbyLocationRelevance.COMPLEMENTAR,
                igual("office", "coworking"));

        // EDIÇÃO DE VÍDEO
        cadastrar("Edição de vídeo",
                HobbyLocationRelevance.COMPLEMENTAR,
                igual("office", "coworking"));
        cadastrar("Edição de vídeo",
                HobbyLocationRelevance.COMPLEMENTAR,
                igual("amenity", "arts_centre"));

        // ROBÓTICA BÁSICA
        cadastrar("Robótica básica", HobbyLocationRelevance.ALTA,
                igual("club", "robotics"));
        cadastrar("Robótica básica", HobbyLocationRelevance.COMPLEMENTAR,
                igual("craft", "electronics"));

        // CRIAÇÃO DE JOGOS
        cadastrar("Criação de jogos",
                HobbyLocationRelevance.COMPLEMENTAR,
                igual("office", "coworking"));
        cadastrar("Criação de jogos",
                HobbyLocationRelevance.COMPLEMENTAR,
                igual("amenity", "library"));

        // MEDITAÇÃO
        cadastrar("Meditação", HobbyLocationRelevance.COMPLEMENTAR,
                igual("leisure", "park"));
        cadastrar("Meditação", HobbyLocationRelevance.ALTA,
                igual("leisure", "garden"));

        // JARDINAGEM
        cadastrar("Jardinagem", HobbyLocationRelevance.COMPLEMENTAR,
                igual("shop", "garden_centre"));
        cadastrar("Jardinagem", HobbyLocationRelevance.ALTA,
                igual("leisure", "garden"));

        // CULINÁRIA
        cadastrar("Culinária", HobbyLocationRelevance.ALTA,
                igual("cooking:teaching", "yes"));
        cadastrar("Culinária", HobbyLocationRelevance.COMPLEMENTAR,
                igual("shop", "kitchen"));

        // CAMINHADA
        cadastrar("Caminhada", HobbyLocationRelevance.ALTA,
                igual("leisure", "park"));
        cadastrar("Caminhada", HobbyLocationRelevance.ALTA,
                igual("leisure", "nature_reserve"));

    }

    private void cadastrar(
            String nomeHobby,
            HobbyLocationRelevance relevancia,
            CondicaoDefinicao... condicoes
    ) {
        Optional<Hobby> hobbyOpt = hobbyRepository.findAll()
                .stream()
                .filter(h -> normalizar(h.getNome())
                        .equals(normalizar(nomeHobby)))
                .findFirst();

        if (hobbyOpt.isEmpty()) {
            System.out.println(
                    "Migração OSM: hobby não encontrado: " + nomeHobby
            );
            return;
        }

        Hobby hobby = hobbyOpt.get();

        Set<String> assinaturaNova = assinatura(
                Arrays.asList(condicoes)
        );

        List<HobbyLocationRule> existentes =
                ruleRepository.findActiveWithConditions(hobby.getId());

        boolean duplicada = existentes.stream()
                .anyMatch(regra ->
                        assinatura(regra.getConditions().stream()
                                .map(c -> new CondicaoDefinicao(
                                        c.getOsmKey(),
                                        c.getOsmValue(),
                                        c.getMatchType()
                                ))
                                .toList()
                        ).equals(assinaturaNova)
                );

        if (duplicada) {
            return;
        }

        HobbyLocationRule regra = new HobbyLocationRule();
        regra.setHobby(hobby);
        regra.setRelevancia(relevancia);
        regra.setAtivo(true);

        for (CondicaoDefinicao condicao : condicoes) {

            HobbyLocationCondition entidade =
                    new HobbyLocationCondition();

            entidade.setOsmKey(condicao.chave());
            entidade.setOsmValue(condicao.valor());
            entidade.setMatchType(condicao.tipo());

            regra.adicionarCondicao(entidade);
        }

        ruleRepository.save(regra);

        System.out.println(
                "Migração OSM: regra cadastrada para " + nomeHobby
        );
    }

    private CondicaoDefinicao igual(String chave, String valor) {
        return new CondicaoDefinicao(
                chave,
                valor,
                OsmMatchType.IGUAL
        );
    }

    private CondicaoDefinicao existe(String chave) {
        return new CondicaoDefinicao(
                chave,
                null,
                OsmMatchType.EXISTE
        );
    }

    private Set<String> assinatura(
            List<CondicaoDefinicao> condicoes
    ) {
        return condicoes.stream()
                .map(c ->
                        c.tipo() + "|" +
                                c.chave() + "|" +
                                (c.valor() == null ? "" : c.valor())
                )
                .collect(Collectors.toSet());
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(
                        texto.trim().toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ");
    }

    private record CondicaoDefinicao(
            String chave,
            String valor,
            OsmMatchType tipo
    ) {}
}
