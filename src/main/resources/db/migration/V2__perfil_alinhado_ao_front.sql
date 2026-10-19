-- =====================================================================
-- V2: alinha a tabela de perfis ao cadastro do front
--     (frontend/src/types/profile.ts)
--
-- Os valores aceitos são IGUAIS aos do front (ex.: 'jovem-aprendiz'),
-- assim o site e a API conversam sem precisar traduzir nada.
-- Perguntas com várias respostas viram listas (TEXT[]).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Remove o que o front não usa (ou usa de outro jeito)
-- ---------------------------------------------------------------------
ALTER TABLE perfis
    DROP CONSTRAINT ck_nivel,
    DROP CONSTRAINT ck_modalidade,
    DROP COLUMN nivel,               -- vira "niveis" (várias respostas)
    DROP COLUMN modalidade,          -- vira "modalidades" (várias respostas)
    DROP COLUMN pretensao_salarial,  -- o cadastro não pergunta
    DROP COLUMN resumo;              -- o cadastro não pergunta

ALTER TABLE perfis RENAME COLUMN estado TO uf;

-- As áreas no front são textos livres (string[]), então viram uma lista
-- dentro do perfil em vez de uma tabela separada.
DROP TABLE usuario_areas;
DROP TABLE areas_interesse;

-- ---------------------------------------------------------------------
-- 2. Novos campos, na mesma ordem do Profile do front
-- ---------------------------------------------------------------------
ALTER TABLE perfis
    ADD COLUMN momento               VARCHAR(30),
    ADD COLUMN areas                 TEXT[]       NOT NULL DEFAULT '{}',
    ADD COLUMN niveis                TEXT[]       NOT NULL DEFAULT '{}',
    ADD COLUMN contratos             TEXT[]       NOT NULL DEFAULT '{}',
    ADD COLUMN modalidades           TEXT[]       NOT NULL DEFAULT '{}',
    ADD COLUMN alcance               VARCHAR(30),
    ADD COLUMN turnos                TEXT[]       NOT NULL DEFAULT '{}',
    ADD COLUMN escolaridade_nivel    VARCHAR(30),
    ADD COLUMN escolaridade_status   VARCHAR(20),
    ADD COLUMN curso                 VARCHAR(150),
    ADD COLUMN experiencias          TEXT[]       NOT NULL DEFAULT '{}',
    ADD COLUMN experiencia_descricao TEXT,
    ADD COLUMN ultimo_cargo          VARCHAR(150),
    ADD COLUMN tempo_experiencia     VARCHAR(20),
    ADD COLUMN habilidades           TEXT[]       NOT NULL DEFAULT '{}',
    -- lista de { "idioma": "Inglês", "nivel": "basico" }
    ADD COLUMN idiomas               JSONB        NOT NULL DEFAULT '[]',
    ADD COLUMN criado_em             TIMESTAMPTZ  NOT NULL DEFAULT now();

-- ---------------------------------------------------------------------
-- 3. Regras: só aceita os valores que o front oferece
--    ( "<@" = "todos os itens da lista estão entre os permitidos" )
-- ---------------------------------------------------------------------
ALTER TABLE perfis
    ADD CONSTRAINT ck_momento CHECK (momento IN
        ('primeiro-emprego','quero-mudar','voltar-ao-mercado','mudar-de-area')),

    ADD CONSTRAINT ck_niveis CHECK (niveis <@ ARRAY
        ['jovem-aprendiz','estagio','trainee','junior','pleno','senior','gestao']::TEXT[]),

    ADD CONSTRAINT ck_contratos CHECK (contratos <@ ARRAY
        ['clt','pj','temporario','freela','tanto-faz']::TEXT[]),

    ADD CONSTRAINT ck_modalidades CHECK (modalidades <@ ARRAY
        ['presencial','hibrido','remoto']::TEXT[]),

    ADD CONSTRAINT ck_alcance CHECK (alcance IN
        ('minha-cidade','minha-regiao','qualquer-lugar')),

    ADD CONSTRAINT ck_turnos CHECK (turnos <@ ARRAY
        ['manha','tarde','noite','integral','flexivel']::TEXT[]),

    ADD CONSTRAINT ck_escolaridade_nivel CHECK (escolaridade_nivel IN
        ('fundamental','medio','tecnico','superior','pos','mestrado-doutorado')),

    ADD CONSTRAINT ck_escolaridade_status CHECK (escolaridade_status IN
        ('cursando','completo')),

    ADD CONSTRAINT ck_experiencias CHECK (experiencias <@ ARRAY
        ['nunca-trabalhei','estagio','aprendiz','bico-freela','voluntariado','projeto']::TEXT[]),

    ADD CONSTRAINT ck_tempo_experiencia CHECK (tempo_experiencia IN
        ('menos-1','1-3','3-5','5-10','mais-10')),

    ADD CONSTRAINT ck_idiomas_lista CHECK (jsonb_typeof(idiomas) = 'array');