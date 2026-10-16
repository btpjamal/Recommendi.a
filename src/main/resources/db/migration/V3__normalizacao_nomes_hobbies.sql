CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE OR REPLACE FUNCTION normalizar_nome_hobby(nome TEXT)
RETURNS TEXT
LANGUAGE SQL
STABLE
AS $$
    SELECT lower(
        regexp_replace(
            btrim(unaccent(nome)),
            '\s+',
            ' ',
            'g'
        )
    );
$$;