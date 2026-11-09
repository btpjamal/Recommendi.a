ALTER TABLE social_profiles
    ADD COLUMN compartilhar_regiao BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE communities DROP CONSTRAINT communities_tipo_check;

ALTER TABLE communities ADD CONSTRAINT communities_tipo_check
    CHECK (tipo IN ('GERAL', 'REGIONAL', 'MUNICIPAL'));

ALTER TABLE communities ADD CONSTRAINT chk_community_municipal_location
    CHECK (tipo <> 'MUNICIPAL' OR (
        estado IS NOT NULL AND length(trim(estado)) = 2
        AND cidade IS NOT NULL AND length(trim(cidade)) > 0
    ));

-- V7 ja garante unicidade de hobby + tipo + estado + cidade,
-- inclusive quando cidade/estado sao NULL nas comunidades antigas.
