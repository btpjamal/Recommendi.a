ALTER TABLE hobby_image_upload
DROP CONSTRAINT chk_hobby_image_upload_status;

ALTER TABLE hobby_image_upload
    ADD CONSTRAINT chk_hobby_image_upload_status
        CHECK (
            status IN (
                       'PENDENTE',
                       'CONFIRMADA',
                       'DESCARTADA',
                       'AGUARDANDO_EXCLUSAO',
                       'SUBSTITUIDA'
                )
            );