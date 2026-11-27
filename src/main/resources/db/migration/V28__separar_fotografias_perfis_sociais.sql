CREATE TABLE social_profile_photos (
                                       profile_id BIGINT PRIMARY KEY,

                                       foto BYTEA NOT NULL,

                                       miniatura BYTEA,

                                       content_type VARCHAR(32) NOT NULL,

                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_social_profile_photos_profile
                                           FOREIGN KEY (profile_id)
                                               REFERENCES social_profiles(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT ck_social_profile_photos_content_type
                                           CHECK (content_type IN ('image/png', 'image/jpeg'))
);

-- Preservar as fotografias existentes.
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
        WHEN encode(substring(foto FROM 1 FOR 3), 'hex') = 'ffd8ff'
            THEN 'image/jpeg'
        ELSE 'image/png'
        END,
    CURRENT_TIMESTAMP
FROM social_profiles
WHERE foto IS NOT NULL;