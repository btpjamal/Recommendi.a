-- Impedir escritas concorrentes durante a transição.
LOCK TABLE social_profiles IN ACCESS EXCLUSIVE MODE;
LOCK TABLE social_profile_photos IN ACCESS EXCLUSIVE MODE;

-- Validar os formatos das fotografias existentes.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM social_profiles
        WHERE foto IS NOT NULL
          AND substring(foto FROM 1 FOR 3)
              <> decode('ffd8ff', 'hex')
          AND substring(foto FROM 1 FOR 8)
              <> decode('89504e470d0a1a0a', 'hex')
    ) THEN
        RAISE EXCEPTION 'Formato de fotografia desconhecido.';
END IF;
END $$;

-- Remover fotografias excluídas desde a V28.
DELETE FROM social_profile_photos ph
WHERE NOT EXISTS (
    SELECT 1
    FROM social_profiles p
    WHERE p.id = ph.profile_id
      AND p.foto IS NOT NULL
);

-- Sincronizar fotografias novas ou modificadas.
INSERT INTO social_profile_photos (
    profile_id,
    foto,
    content_type,
    updated_at
)
SELECT
    id,
    foto,
    CASE
        WHEN substring(foto FROM 1 FOR 3)
            = decode('ffd8ff', 'hex')
            THEN 'image/jpeg'
        ELSE 'image/png'
        END,
    CURRENT_TIMESTAMP
FROM social_profiles
WHERE foto IS NOT NULL

    ON CONFLICT (profile_id)
DO UPDATE SET
    foto = EXCLUDED.foto,
           content_type = EXCLUDED.content_type,
           updated_at = EXCLUDED.updated_at,
           miniatura = NULL
   WHERE social_profile_photos.foto
           IS DISTINCT FROM EXCLUDED.foto
           OR social_profile_photos.content_type
           IS DISTINCT FROM EXCLUDED.content_type;

-- Confirmar a integridade antes de excluir a coluna.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM social_profiles p
        LEFT JOIN social_profile_photos ph
            ON ph.profile_id = p.id
        WHERE p.foto IS DISTINCT FROM ph.foto
    ) THEN
        RAISE EXCEPTION 'Falha na sincronização das fotografias.';
END IF;
END $$;

-- Remover o armazenamento antigo.
ALTER TABLE social_profiles DROP COLUMN foto;