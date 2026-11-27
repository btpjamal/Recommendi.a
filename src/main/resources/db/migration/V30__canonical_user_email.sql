-- PostgreSQL Flyway runs this migration in one transaction.
LOCK TABLE public.users IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.users
        GROUP BY translate(btrim(email COLLATE "C", ' '),
            'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'V30: canonical email collisions in public.users. Resolve account ownership before retrying.';
    END IF;
END
$$;

ALTER TABLE public.users
    ALTER COLUMN email TYPE varchar(255) COLLATE "C",
    ADD CONSTRAINT users_email_canonical_check CHECK (
        email = translate(btrim(email, ' '),
            'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')
    ) NOT VALID;

UPDATE public.users
SET email = translate(btrim(email, ' '),
    'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')
WHERE email <> translate(btrim(email, ' '),
    'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz');

ALTER TABLE public.users VALIDATE CONSTRAINT users_email_canonical_check;
ALTER TABLE public.users RENAME CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 TO users_email_canonical_key;
