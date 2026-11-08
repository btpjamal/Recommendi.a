-- Os horarios legados de data_upload foram gerados pela aplicacao local
-- em America/Sao_Paulo. Interpretamos explicitamente esse fuso para
-- preservar os instantes, independentemente do TimeZone da sessao.
-- A aplicacao deve estar parada durante esta migration.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM hobby_image_upload
        WHERE exclusao_iniciada_em IS NOT NULL
    ) THEN
        RAISE EXCEPTION
            'Existem datas de reserva pendentes de auditoria. Confira sua origem antes de aplicar V13.';
END IF;
END
$$;

ALTER TABLE hobby_image_upload
    ALTER COLUMN data_upload DROP DEFAULT;

ALTER TABLE hobby_image_upload
ALTER COLUMN data_upload TYPE TIMESTAMP(6) WITH TIME ZONE
        USING data_upload AT TIME ZONE 'America/Sao_Paulo',
    ALTER COLUMN exclusao_iniciada_em TYPE TIMESTAMP(6) WITH TIME ZONE
        USING exclusao_iniciada_em AT TIME ZONE 'America/Sao_Paulo';

ALTER TABLE hobby_image_upload
    ALTER COLUMN data_upload SET DEFAULT CURRENT_TIMESTAMP;
