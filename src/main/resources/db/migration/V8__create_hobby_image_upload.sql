CREATE TABLE hobby_image_upload (
    id BIGSERIAL PRIMARY KEY,

    imagem_url VARCHAR(2000) NOT NULL,

    public_id VARCHAR(255) NOT NULL UNIQUE,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',

    data_upload TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    hobby_id BIGINT,

    CONSTRAINT fk_hobby_image_upload_hobby
        FOREIGN KEY (hobby_id)
        REFERENCES hobby(id),

    CONSTRAINT chk_hobby_image_upload_status
        CHECK (status IN (
            'PENDENTE',
            'CONFIRMADA',
            'DESCARTADA'
        ))
);

CREATE INDEX idx_hobby_image_upload_status
    ON hobby_image_upload(status);

CREATE INDEX idx_hobby_image_upload_hobby
    ON hobby_image_upload(hobby_id);