CREATE TABLE hobby_location_result (
    hobby_id BIGINT NOT NULL REFERENCES hobby(id) ON DELETE CASCADE,
    municipio TEXT NOT NULL,
    estado TEXT NOT NULL,
    versao_regras TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    locais JSONB NOT NULL,
    consultado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (hobby_id, municipio, estado, versao_regras)
);
