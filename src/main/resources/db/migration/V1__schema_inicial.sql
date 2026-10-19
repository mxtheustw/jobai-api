-- =====================================================================
-- Schema inicial do banco de dados (PostgreSQL 13+)
-- Uso: coloque este arquivo em src/main/resources/db/migration/
--      e o Flyway roda automaticamente quando o Spring Boot sobe.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. USUÁRIOS (login)
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome             VARCHAR(120)  NOT NULL,
    email            VARCHAR(255)  NOT NULL,
    senha_hash       VARCHAR(255),                 -- NULL se logar só com Google/Apple
    provedor         VARCHAR(20)   NOT NULL DEFAULT 'LOCAL', -- LOCAL, GOOGLE, APPLE
    provedor_id      VARCHAR(255),                 -- id do usuário no Google/Apple
    email_verificado BOOLEAN       NOT NULL DEFAULT FALSE,
    ativo            BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_provedor CHECK (provedor IN ('LOCAL', 'GOOGLE', 'APPLE'))
);

-- e-mail único sem diferenciar maiúsculas/minúsculas
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (LOWER(email));

-- ---------------------------------------------------------------------
-- 2. SESSÕES (refresh tokens) – permite estar logado no site E no app
--    ao mesmo tempo, e deslogar um dispositivo específico.
-- ---------------------------------------------------------------------
CREATE TABLE sessoes (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id   UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash   VARCHAR(255) NOT NULL UNIQUE,     -- nunca salvar o token puro
    dispositivo  VARCHAR(20)  NOT NULL,            -- WEB, ANDROID, IOS
    expira_em    TIMESTAMPTZ  NOT NULL,
    revogado     BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_dispositivo CHECK (dispositivo IN ('WEB', 'ANDROID', 'IOS'))
);
CREATE INDEX ix_sessoes_usuario ON sessoes (usuario_id);

-- ---------------------------------------------------------------------
-- 3. TOKENS DE E-MAIL (confirmar conta / recuperar senha)
-- ---------------------------------------------------------------------
CREATE TABLE tokens_email (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    tipo        VARCHAR(20)  NOT NULL,             -- VERIFICACAO, RESET_SENHA
    expira_em   TIMESTAMPTZ  NOT NULL,
    usado_em    TIMESTAMPTZ,
    CONSTRAINT ck_tipo_token CHECK (tipo IN ('VERIFICACAO', 'RESET_SENHA'))
);

-- ---------------------------------------------------------------------
-- 4. PERFIL PROFISSIONAL – o que a IA usa para filtrar as vagas
-- ---------------------------------------------------------------------
CREATE TABLE perfis (
    usuario_id         UUID PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    nivel              VARCHAR(20),    -- ESTAGIO, JUNIOR, PLENO, SENIOR, ESPECIALISTA
    modalidade         VARCHAR(20),    -- PRESENCIAL, REMOTO, HIBRIDO, QUALQUER
    cidade             VARCHAR(100),
    estado             CHAR(2),        -- UF: SP, RJ, MG...
    pretensao_salarial NUMERIC(10,2),
    resumo             TEXT,           -- texto livre / mini currículo para a IA
    atualizado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_nivel CHECK (nivel IN ('ESTAGIO','JUNIOR','PLENO','SENIOR','ESPECIALISTA')),
    CONSTRAINT ck_modalidade CHECK (modalidade IN ('PRESENCIAL','REMOTO','HIBRIDO','QUALQUER'))
);

-- Áreas de interesse (lista fixa) + relação N:N com usuários
CREATE TABLE areas_interesse (
    id    SERIAL PRIMARY KEY,
    nome  VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE usuario_areas (
    usuario_id UUID    NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    area_id    INTEGER NOT NULL REFERENCES areas_interesse(id),
    PRIMARY KEY (usuario_id, area_id)
);

INSERT INTO areas_interesse (nome) VALUES
    ('Tecnologia'), ('Administração'), ('Marketing'), ('Vendas'),
    ('Financeiro'), ('Recursos Humanos'), ('Saúde'), ('Engenharia'),
    ('Design'), ('Educação'), ('Jurídico'), ('Logística');

-- ---------------------------------------------------------------------
-- 5. PLANOS E ASSINATURAS (mensalidade)
-- ---------------------------------------------------------------------
CREATE TABLE planos (
    id            SERIAL PRIMARY KEY,
    codigo        VARCHAR(30)   NOT NULL UNIQUE,   -- GRATIS, PRO, PREMIUM
    nome          VARCHAR(60)   NOT NULL,
    preco_mensal  NUMERIC(10,2) NOT NULL DEFAULT 0,
    ativo         BOOLEAN       NOT NULL DEFAULT TRUE
);

INSERT INTO planos (codigo, nome, preco_mensal) VALUES
    ('GRATIS',  'Grátis',  0.00),
    ('PRO',     'Pro',     19.90),
    ('PREMIUM', 'Premium', 39.90);

CREATE TABLE assinaturas (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id         UUID        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    plano_id           INTEGER     NOT NULL REFERENCES planos(id),
    status             VARCHAR(20) NOT NULL,       -- ATIVA, CANCELADA, PENDENTE, EXPIRADA
    inicio_em          TIMESTAMPTZ NOT NULL DEFAULT now(),
    fim_periodo_em     TIMESTAMPTZ,                -- até quando está pago
    gateway            VARCHAR(30),                -- STRIPE, MERCADO_PAGO, APPLE, GOOGLE_PLAY
    gateway_assinatura_id VARCHAR(255),            -- id da assinatura no gateway
    criado_em          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_status_assinatura CHECK (status IN ('ATIVA','CANCELADA','PENDENTE','EXPIRADA'))
);
CREATE INDEX ix_assinaturas_usuario ON assinaturas (usuario_id);

-- ---------------------------------------------------------------------
-- 6. VAGAS SALVAS / RECOMENDADAS
--    vaga_id_externo = id que vem da API de vagas dos seus amigos.
-- ---------------------------------------------------------------------
CREATE TABLE vagas_usuario (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id       UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    vaga_id_externo  VARCHAR(255) NOT NULL,
    situacao         VARCHAR(20)  NOT NULL DEFAULT 'RECOMENDADA', -- RECOMENDADA, SALVA, CANDIDATOU, DESCARTADA
    score_ia         NUMERIC(5,2),                 -- 0 a 100, quanto combina
    criado_em        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (usuario_id, vaga_id_externo)
);