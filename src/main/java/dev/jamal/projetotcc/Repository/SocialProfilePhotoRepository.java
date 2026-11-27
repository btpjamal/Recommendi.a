package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.SocialProfilePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import java.time.LocalDateTime;

import java.util.Optional;

@Repository
public interface SocialProfilePhotoRepository
        extends JpaRepository<SocialProfilePhoto, Long> {

    interface PhotoData {

        byte[] getFoto();

        String getContentType();
    }

    interface ThumbnailData {

        String getVersao();

        byte[] getMiniatura();
    }

    @Query("""
        SELECT p.foto AS foto,
               p.contentType AS contentType
        FROM SocialProfilePhoto p
        WHERE p.profileId = :profileId
        """)
    Optional<PhotoData> findPhotoData(
            @Param("profileId") Long profileId
    );

    @Query("""
    SELECT p.miniatura
    FROM SocialProfilePhoto p
    WHERE p.profileId = :profileId
      AND p.miniatura IS NOT NULL
    """)
    Optional<byte[]> findThumbnail(
            @Param("profileId") Long profileId
    );

    @Query(value = """
    SELECT
        dados.versao AS versao,
        CASE
            WHEN :versaoCliente = '*'
              OR dados.versao = ANY(string_to_array(:versaoCliente, ','))
            THEN NULL
            ELSE dados.miniatura
        END AS miniatura
    FROM (
        SELECT
            miniatura,
            encode(
                sha256(miniatura),
                'hex'
            ) AS versao
        FROM social_profile_photos
        WHERE profile_id = :profileId
          AND miniatura IS NOT NULL
    ) dados
    """, nativeQuery = true)
    Optional<ThumbnailData> findThumbnailConditional(
            @Param("profileId") Long profileId,
            @Param("versaoCliente") String versaoCliente
    );

    interface ConditionalPhotoData {
        String getVersao();
        byte[] getFoto();
    }

    @Query(value = """
            SELECT dados.versao,
                   CASE WHEN :versaoCliente = '*'
                          OR dados.versao = ANY(string_to_array(:versaoCliente, ','))
                        THEN NULL ELSE dados.foto END AS foto
            FROM (
                SELECT foto, 'photo-' || encode(sha256(foto), 'hex') AS versao
                FROM social_profile_photos
                WHERE profile_id = :profileId
            ) dados
            """, nativeQuery = true)
    Optional<ConditionalPhotoData> findPhotoConditional(
            @Param("profileId") Long profileId,
            @Param("versaoCliente") String versaoCliente);

    @Modifying
    @Query(value = """
    INSERT INTO social_profile_photos (
        profile_id,
        foto,
        miniatura,
        content_type,
        updated_at
    )
    VALUES (
        :profileId,
        :foto,
        :miniatura,
        :contentType,
        :updatedAt
    )
    ON CONFLICT (profile_id)
    DO UPDATE SET
        foto = EXCLUDED.foto,
        miniatura = EXCLUDED.miniatura,
        content_type = EXCLUDED.content_type,
        updated_at = EXCLUDED.updated_at
    """, nativeQuery = true)
    int salvarOuAtualizar(
            @Param("profileId") Long profileId,
            @Param("foto") byte[] foto,
            @Param("miniatura") byte[] miniatura,
            @Param("contentType") String contentType,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying
    @Query("""
    DELETE FROM SocialProfilePhoto p
    WHERE p.profileId = :profileId
    """)
    int remover(@Param("profileId") Long profileId);


    interface PendingPhoto {

        byte[] getFoto();

        String getRevisao();
    }

    @Query(value = """
    SELECT max(profile_id)
    FROM social_profile_photos
    WHERE miniatura IS NULL
    """, nativeQuery = true)
    Long ultimoIdPendente();

    @Query(value = """
    SELECT profile_id
    FROM social_profile_photos
    WHERE miniatura IS NULL
      AND (CAST(:aposId AS bigint) IS NULL OR profile_id > :aposId)
      AND profile_id <= :ultimoId
    ORDER BY profile_id
    LIMIT :limite
    """, nativeQuery = true)
    java.util.List<Long> buscarMiniaturasPendentes(
            @Param("aposId") Long aposId,
            @Param("ultimoId") long ultimoId,
            @Param("limite") int limite
    );

    @Query(value = """
    SELECT foto, CAST(xmin AS text) AS revisao
    FROM social_profile_photos
    WHERE profile_id = :profileId
      AND miniatura IS NULL
    """, nativeQuery = true)
    Optional<PendingPhoto> buscarFotoPendente(@Param("profileId") long profileId);

    @Query(value = """
    SELECT count(*) FROM social_profile_photos WHERE miniatura IS NULL
    """, nativeQuery = true)
    long contarMiniaturasPendentes();

    // xmin identifica a versão MVCC lida antes da decodificação.
    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(value = """
    UPDATE social_profile_photos
    SET miniatura = :miniatura
    WHERE profile_id = :profileId
      AND miniatura IS NULL
      AND xmin = CAST(:revisao AS xid)
    """, nativeQuery = true)
    int preencherMiniatura(
            @Param("profileId") long profileId,
            @Param("miniatura") byte[] miniatura,
            @Param("revisao") String revisao
    );
}
