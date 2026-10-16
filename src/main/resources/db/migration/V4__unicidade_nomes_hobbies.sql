-- Função auxiliar para permitir indexação.
-- Utiliza o dicionário padrão do unaccent.
CREATE OR REPLACE FUNCTION normalizar_nome_hobby_indice(nome TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT lower(
        regexp_replace(
            btrim(public.unaccent('public.unaccent'::regdictionary, nome)),
            '\s+',
            ' ',
            'g'
        )
    );
$$;

-- Impede hobbies com nomes equivalentes,
-- incluindo registros inativos.
CREATE UNIQUE INDEX ux_hobby_nome_normalizado
ON hobby (
    normalizar_nome_hobby_indice(nome)
);

-- Impede sugestões pendentes com nomes equivalentes.
-- Sugestões aprovadas e rejeitadas permanecem no histórico.
CREATE UNIQUE INDEX ux_hobby_suggestion_pendente_nome
ON hobby_suggestion (
    normalizar_nome_hobby_indice(nome)
)
WHERE status = 'PENDENTE';