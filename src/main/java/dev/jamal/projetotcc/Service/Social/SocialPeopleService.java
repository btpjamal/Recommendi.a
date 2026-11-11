package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Enum.SocialLocationScope;
import dev.jamal.projetotcc.Enum.SocialVisibility;
import dev.jamal.projetotcc.Service.Social.SocialLocationService.Localizacao;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SocialPeopleService {
    private final NamedParameterJdbcTemplate jdbc;
    private final SocialLocationService locations;
    private final SocialAffinityService affinities;
    private final SocialProfileResumeService resumes;

    private record Person(Long id, String name, String bio, boolean photo,
                          SocialVisibility bioRule, SocialVisibility photoRule,
                          SocialVisibility interestsRule, SocialVisibility hobbiesRule,
                          Localizacao location, boolean shares, boolean contact, boolean following) {}
    private record Ranked(Person person, String proximity, SocialProfileDTO visible, SocialAffinityDTO affinity) {}

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public SocialPeoplePageDTO listar(Long actor, SocialLocationScope scope, int page, int limit) {
        if (page < 0 || limit < 1 || limit > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma página válida e limite de 1 a 30.");
        }
        var profiles = profiles(actor);
        var own = profiles.stream().filter(p -> p.id().equals(actor)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Ative sua participação social."));
        try {
            locations.validarEscopo(own.location(), scope);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage());
        }
        Map<Long, String> proximity = new HashMap<>();
        List<Person> candidates = new ArrayList<>();
        for (Person candidate : profiles) {
            if (candidate.id().equals(actor)) continue;
            String near = candidate.shares() ? SocialLocationService.comparar(own.location(), candidate.location()) : null;
            boolean matches = switch (scope) {
                case GERAL -> true;
                case ESTADUAL -> near != null;
                case MUNICIPAL -> "MESMA_CIDADE".equals(near);
            };
            if (matches) {
                proximity.put(candidate.id(), near);
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) return new SocialPeoplePageDTO(List.of(), false);

        var ids = new ArrayList<>(candidates.stream().map(Person::id).toList());
        ids.add(actor);
        var interests = new HashMap<Long, List<String>>();
        var practicing = new HashMap<Long, List<SocialHobbyDTO>>();
        var interested = new HashMap<Long, List<SocialHobbyDTO>>();
        for (int start = 0; start < ids.size(); start += 1000) {
            Map<String, Object> batch = Map.of("ids", ids.subList(start, Math.min(ids.size(), start + 1000)));
            jdbc.query("""
                SELECT ui.user_id, i.nome FROM user_interest ui
                JOIN interest i ON i.id = ui.interest_id
                WHERE ui.user_id IN (:ids)
                """, batch, row -> {
                interests.computeIfAbsent(row.getLong("user_id"), id -> new ArrayList<>()).add(row.getString("nome"));
            });
            jdbc.query("""
                SELECT uh.user_id, h.id, h.nome, true AS practicing
                  FROM user_hobbies uh JOIN hobby h ON h.id = uh.hobby_id
                 WHERE uh.user_id IN (:ids) AND uh.status_atual = 'PRATICANDO'
                UNION ALL
                SELECT f.user_id, h.id, h.nome, false AS practicing
                  FROM user_recommendation_feedbacks f JOIN hobby h ON h.id = f.hobby_id
                  LEFT JOIN user_hobbies uh ON uh.user_id = f.user_id AND uh.hobby_id = f.hobby_id
                 WHERE f.user_id IN (:ids) AND f.tipo = 'INTERESSADO'
                   AND (uh.id IS NULL OR uh.status_atual = 'INTERESSADO')
                ORDER BY user_id, id
                """, batch, row -> {
                var target = row.getBoolean("practicing") ? practicing : interested;
                target.computeIfAbsent(row.getLong("user_id"), id -> new ArrayList<>())
                        .add(new SocialHobbyDTO(row.getLong("id"), row.getString("nome")));
            });
        }
        interests.values().forEach(names -> names.sort(Comparator.naturalOrder()));
        var ownPublic = visible(own, false, interests, practicing, interested);
        var ownContact = visible(own, true, interests, practicing, interested);
        var ranked = candidates.stream().map(person -> {
            var profile = visible(person, person.contact(), interests, practicing, interested);
            var affinity = affinities.calcular(person.contact() ? ownContact : ownPublic, profile);
            return new Ranked(person, proximity.get(person.id()), profile, affinity);
        }).sorted(Comparator.comparingInt((Ranked row) -> row.affinity().score()).reversed()
                .thenComparing(row -> row.person().id()))
          .skip((long) page * limit).limit(limit + 1L).toList();

        var cards = ranked.stream().limit(limit).map(row -> {
            var p = row.visible();
            var resume = resumes.construirResumo(p.nome(), p.interesses(), p.praticando(), p.interessados());
            var profile = new SocialProfileDTO(p.userId(), p.nome(), resume, p.interesses(), p.praticando(),
                    p.interessados(), row.affinity(), row.proximity(), p.biografia(), p.possuiFoto());
            return new SocialPeoplePageDTO.Card(profile, row.person().following());
        }).toList();
        return new SocialPeoplePageDTO(cards, ranked.size() > limit);
    }

    private List<Person> profiles(Long actor) {
        return jdbc.query("""
            SELECT u.id, u.nome, p.biografia, p.foto IS NOT NULL AS possui_foto,
                   p.biografia_visibilidade, p.foto_visibilidade, p.interesses_visibilidade, p.hobbies_visibilidade,
                   p.compartilhar_regiao, rp.estado, rp.cidade,
                   c.id IS NOT NULL AS contato, f.follower_id IS NOT NULL AS seguindo
              FROM social_profiles p JOIN users u ON u.id = p.user_id
              LEFT JOIN recommendation_profile rp ON rp.user_id = u.id
              LEFT JOIN social_contacts c ON c.low_user_id = LEAST(:actor, u.id)
                   AND c.high_user_id = GREATEST(:actor, u.id) AND c.status = 'ACEITA'
              LEFT JOIN social_follows f ON f.follower_id = :actor AND f.followed_id = u.id
             WHERE p.enabled AND u.data_nascimento <= :adult
               AND NOT EXISTS (SELECT 1 FROM social_user_blocks b
                    WHERE (b.blocker_id = :actor AND b.blocked_id = u.id)
                       OR (b.blocked_id = :actor AND b.blocker_id = u.id))
            """, Map.of("actor", actor, "adult", LocalDate.now().minusYears(18)), (row, index) ->
                new Person(row.getLong("id"), row.getString("nome"), row.getString("biografia"), row.getBoolean("possui_foto"),
                    SocialVisibility.valueOf(row.getString("biografia_visibilidade")),
                    SocialVisibility.valueOf(row.getString("foto_visibilidade")),
                    SocialVisibility.valueOf(row.getString("interesses_visibilidade")),
                    SocialVisibility.valueOf(row.getString("hobbies_visibilidade")),
                    new Localizacao(SocialLocationService.normalizarEstado(row.getString("estado")),
                            SocialLocationService.normalizarCidade(row.getString("cidade"))),
                    row.getBoolean("compartilhar_regiao"), row.getBoolean("contato"), row.getBoolean("seguindo")));
    }

    private boolean allowed(SocialVisibility rule, boolean contact) {
        return rule == SocialVisibility.PARTICIPANTES || (rule == SocialVisibility.CONTATOS && contact);
    }

    private SocialProfileDTO visible(Person person, boolean contact, Map<Long, List<String>> interests,
            Map<Long, List<SocialHobbyDTO>> practicing, Map<Long, List<SocialHobbyDTO>> interested) {
        boolean hobbies = allowed(person.hobbiesRule(), contact);
        return new SocialProfileDTO(person.id(), person.name(), null,
                allowed(person.interestsRule(), contact) ? interests.getOrDefault(person.id(), List.of()) : List.of(),
                hobbies ? practicing.getOrDefault(person.id(), List.of()) : List.of(),
                hobbies ? interested.getOrDefault(person.id(), List.of()) : List.of(), null, null,
                allowed(person.bioRule(), contact) ? person.bio() : null,
                person.photo() && allowed(person.photoRule(), contact));
    }
}
