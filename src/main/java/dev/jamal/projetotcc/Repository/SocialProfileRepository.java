package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.DTO.Social.SocialProfileEditDTO;
import dev.jamal.projetotcc.Entities.SocialProfile;
import dev.jamal.projetotcc.Enum.SocialVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialProfileRepository
        extends JpaRepository<SocialProfile, Long> {

    Optional<SocialProfile> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    boolean existsByUser_IdAndEnabledTrue(Long userId);

    @Query("""
            select new dev.jamal.projetotcc.DTO.Social.SocialProfileEditDTO(
                p.biografia, p.fotoVisibilidade, p.biografiaVisibilidade,
                p.interessesVisibilidade, p.hobbiesVisibilidade)
            from SocialProfile p where p.user.id = :userId and p.enabled = true
            """)
    Optional<SocialProfileEditDTO> findEditableByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update SocialProfile p
               set p.biografia = :biografia, p.fotoVisibilidade = :foto,
                   p.biografiaVisibilidade = :bio, p.interessesVisibilidade = :interesses,
                   p.hobbiesVisibilidade = :hobbies, p.updatedAt = :updatedAt
             where p.user.id = :userId and p.enabled = true
            """)
    int updateTextSettings(@Param("userId") Long userId, @Param("biografia") String biografia,
                           @Param("foto") SocialVisibility foto, @Param("bio") SocialVisibility bio,
                           @Param("interesses") SocialVisibility interesses, @Param("hobbies") SocialVisibility hobbies,
                           @Param("updatedAt") LocalDateTime updatedAt);

    String PUBLIC_PROFILE_SELECT = """
            select new dev.jamal.projetotcc.Repository.SocialProfileSummary(
                u.id, u.nome, u.dataNascimento, p.compartilharRegiao, p.biografia,
                p.fotoVisibilidade, p.biografiaVisibilidade, p.interessesVisibilidade,
                p.hobbiesVisibilidade,
                case when exists (
                    select ph.profileId
                    from SocialProfilePhoto ph
                    where ph.profileId = p.id
                ) then true else false end)
            from SocialProfile p join p.user u where p.enabled = true
            """;

    @Query(PUBLIC_PROFILE_SELECT + " and u.id = :userId")
    Optional<SocialProfileSummary> findPublicSummary(@Param("userId") Long userId);

    @Query(PUBLIC_PROFILE_SELECT + " and u.id in :userIds")
    List<SocialProfileSummary> findPublicSummaries(@Param("userIds") List<Long> userIds);

    @Query(PUBLIC_PROFILE_SELECT)
    List<SocialProfileSummary> findEnabledSummaries();

    boolean existsByUser_IdAndEnabledTrueAndCompartilharRegiaoTrue(
            Long userId
    );
}
