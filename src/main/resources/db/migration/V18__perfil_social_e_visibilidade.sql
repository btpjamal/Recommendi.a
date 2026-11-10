ALTER TABLE social_profiles
 ADD COLUMN biografia varchar(500),
 ADD COLUMN foto bytea,
 ADD COLUMN foto_visibilidade varchar(255) NOT NULL DEFAULT 'PARTICIPANTES',
 ADD COLUMN biografia_visibilidade varchar(255) NOT NULL DEFAULT 'PARTICIPANTES',
 ADD COLUMN interesses_visibilidade varchar(255) NOT NULL DEFAULT 'PARTICIPANTES',
 ADD COLUMN hobbies_visibilidade varchar(255) NOT NULL DEFAULT 'PARTICIPANTES';
ALTER TABLE social_profiles
 ADD CONSTRAINT ck_social_foto_visibility CHECK (foto_visibilidade IN ('PARTICIPANTES','CONTATOS','SOMENTE_EU')),
 ADD CONSTRAINT ck_social_bio_visibility CHECK (biografia_visibilidade IN ('PARTICIPANTES','CONTATOS','SOMENTE_EU')),
 ADD CONSTRAINT ck_social_interesses_visibility CHECK (interesses_visibilidade IN ('PARTICIPANTES','CONTATOS','SOMENTE_EU')),
 ADD CONSTRAINT ck_social_hobbies_visibility CHECK (hobbies_visibilidade IN ('PARTICIPANTES','CONTATOS','SOMENTE_EU'));
