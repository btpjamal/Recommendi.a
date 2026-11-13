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

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public SocialPeoplePageDTO listar(Long actor, SocialLocationScope scope, int page, int limit) {
        if (page < 0 || limit < 1 || limit > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma página válida e limite de 1 a 30.");
        }
        var own = profiles(actor, List.of(actor)).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Ative sua participação social."));
        try {
            locations.validarEscopo(own.location(), scope);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage());
        }
        var rankedIds = rankedIds(actor, own, scope, page, limit);
        if (rankedIds.isEmpty()) return new SocialPeoplePageDTO(List.of(), false);

        var ids = new ArrayList<>(rankedIds.stream().limit(limit).toList());
        ids.add(actor);
        var candidates = new HashMap<Long, Person>();
        profiles(actor, ids).forEach(person -> candidates.put(person.id(), person));

        var interests = new HashMap<Long, List<String>>();
        var practicing = new HashMap<Long, List<SocialHobbyDTO>>();
        var interested = new HashMap<Long, List<SocialHobbyDTO>>();
        Map<String, Object> batch = Map.of("ids", ids);
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
        interests.values().forEach(names -> names.sort(Comparator.naturalOrder()));
        var ownPublic = visible(own, false, interests, practicing, interested);
        var ownContact = visible(own, true, interests, practicing, interested);
        var cards = rankedIds.stream().limit(limit).map(id -> {
            var person = candidates.get(id);
            var p = visible(person, person.contact(), interests, practicing, interested);
            var affinity = affinities.calcular(person.contact() ? ownContact : ownPublic, p);
            var proximity = person.shares() ? SocialLocationService.comparar(own.location(), person.location()) : null;


            var resume = resumes.construirResumo(p.nome(), p.interesses(), p.praticando(), p.interessados());
            var profile = new SocialProfileDTO(p.userId(), p.nome(), resume, p.interesses(), p.praticando(),
                    p.interessados(), affinity, proximity, p.biografia(), p.possuiFoto());
            return new SocialPeoplePageDTO.Card(profile, person.following());
        }).toList();
        return new SocialPeoplePageDTO(cards, rankedIds.size() > limit);
    }

    private List<Long> rankedIds(Long actor, Person own, SocialLocationScope scope, int page, int limit) {
        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("actor", actor)
                .addValue("adult", LocalDate.now().minusYears(18))
                .addValue("scope", scope.name())
                .addValue("state", own.location().estado(), java.sql.Types.VARCHAR)
                .addValue("city", own.location().cidade(), java.sql.Types.VARCHAR)
                .addValue("interestWeight", SocialAffinityService.PESO_INTERESSE)
                .addValue("practicingWeight", SocialAffinityService.PESO_PRATICANDO)
                .addValue("interestedWeight", SocialAffinityService.PESO_HOBBY_INTERESSE)
                .addValue("offset", (long) page * limit)
                .addValue("limit", limit + 1);

        return jdbc.queryForList("""
                WITH candidates AS (
                    SELECT p.user_id AS id, c.id IS NOT NULL AS contact,
                           p.interesses_visibilidade AS interests_rule,
                           p.hobbies_visibilidade AS hobbies_rule
                    FROM social_profiles p
                    JOIN users u ON u.id = p.user_id
                    LEFT JOIN recommendation_profile rp ON rp.user_id = u.id
                    LEFT JOIN social_contacts c ON c.low_user_id = LEAST(:actor, u.id)
                         AND c.high_user_id = GREATEST(:actor, u.id) AND c.status = 'ACEITA'
                    WHERE p.user_id <> :actor AND p.enabled AND u.data_nascimento <= :adult
                      AND NOT EXISTS (
                          SELECT 1 FROM social_user_blocks b
                          WHERE (b.blocker_id = :actor AND b.blocked_id = u.id)
                             OR (b.blocked_id = :actor AND b.blocker_id = u.id)
                      )
                      AND (:scope = 'GERAL' OR (p.compartilhar_regiao
                           AND upper(trim(rp.estado)) = :state
                           AND (:scope = 'ESTADUAL' OR upper(trim(regexp_replace(
                               regexp_replace(normalize(rp.cidade, NFD), U&'[\\0300-\\036F]', '', 'g'),
                               '[[:space:]]+', ' ', 'g'))) = :city)))
                ),
                shared_interests AS (
                    SELECT other.user_id, COUNT(DISTINCT mine.interest_id) * :interestWeight AS score
                    FROM user_interest mine
                    JOIN interest own_interest ON own_interest.id = mine.interest_id
                    JOIN interest matched ON matched.nome = own_interest.nome
                    JOIN user_interest other ON other.interest_id = matched.id
                    WHERE mine.user_id = :actor
                    GROUP BY other.user_id
                ),
                shared_practicing AS (
                    SELECT other.user_id, COUNT(*) * :practicingWeight AS score
                    FROM user_hobbies mine
                    JOIN user_hobbies other ON other.hobby_id = mine.hobby_id
                    WHERE mine.user_id = :actor AND mine.status_atual = 'PRATICANDO'
                      AND other.status_atual = 'PRATICANDO'
                    GROUP BY other.user_id
                ),
                shared_interested AS (
                    SELECT other.user_id, COUNT(*) * :interestedWeight AS score
                    FROM user_recommendation_feedbacks mine
                    JOIN user_recommendation_feedbacks other ON other.hobby_id = mine.hobby_id
                    LEFT JOIN user_hobbies own_hobby ON own_hobby.user_id = mine.user_id AND own_hobby.hobby_id = mine.hobby_id
                    LEFT JOIN user_hobbies other_hobby ON other_hobby.user_id = other.user_id AND other_hobby.hobby_id = other.hobby_id
                    WHERE mine.user_id = :actor AND mine.tipo = 'INTERESSADO' AND other.tipo = 'INTERESSADO'
                      AND (own_hobby.id IS NULL OR own_hobby.status_atual = 'INTERESSADO')
                      AND (other_hobby.id IS NULL OR other_hobby.status_atual = 'INTERESSADO')
                    GROUP BY other.user_id
                )
                SELECT c.id FROM candidates c
                LEFT JOIN shared_interests i ON i.user_id = c.id
                LEFT JOIN shared_practicing p ON p.user_id = c.id
                LEFT JOIN shared_interested h ON h.user_id = c.id
                ORDER BY
                    CASE WHEN (c.interests_rule = 'PARTICIPANTES' OR (c.contact AND c.interests_rule = 'CONTATOS'))
                         AND (:ownInterests = 'PARTICIPANTES' OR (c.contact AND :ownInterests = 'CONTATOS'))
                         THEN COALESCE(i.score, 0) ELSE 0 END
                  + CASE WHEN (c.hobbies_rule = 'PARTICIPANTES' OR (c.contact AND c.hobbies_rule = 'CONTATOS'))
                         AND (:ownHobbies = 'PARTICIPANTES' OR (c.contact AND :ownHobbies = 'CONTATOS'))
                         THEN COALESCE(p.score, 0) + COALESCE(h.score, 0) ELSE 0 END DESC,
                    c.id
                LIMIT :limit OFFSET :offset
                """, params.addValue("ownInterests", own.interestsRule().name())
                          .addValue("ownHobbies", own.hobbiesRule().name()), Long.class);
    }

    private List<Person> profiles(Long actor, List<Long> ids) {
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
             WHERE u.id IN (:ids) AND p.enabled AND u.data_nascimento <= :adult
               AND NOT EXISTS (SELECT 1 FROM social_user_blocks b
                    WHERE (b.blocker_id = :actor AND b.blocked_id = u.id)
                       OR (b.blocked_id = :actor AND b.blocker_id = u.id))
            """, Map.of("actor", actor, "ids", ids, "adult", LocalDate.now().minusYears(18)), (row, index) ->
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
