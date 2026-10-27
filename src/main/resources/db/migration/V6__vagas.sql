-- =====================================================================
-- V6: tabela de vagas (gravada pelo coletor via POST /interno/vagas)
--
-- Colunas iguais às do coletor (coletor/normalizar.py), mais:
--   ativa               -> a vaga ainda está aberta?
--   vista_por_ultimo_em -> última vez que o coletor encontrou a vaga
-- Uma vaga é única pela dupla (fonte, id_fonte): se o coletor mandar de
-- novo, ela é atualizada em vez de duplicada.
-- =====================================================================

CREATE TABLE vagas (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fonte               VARCHAR(30)  NOT NULL,          -- ex.: gupy, adzuna
    id_fonte            VARCHAR(100) NOT NULL,          -- id da vaga na fonte
    titulo              VARCHAR(300) NOT NULL,
    empresa             VARCHAR(200),
    cidade              VARCHAR(120),
    uf                  CHAR(2),
    modelo              VARCHAR(20),                    -- presencial, hibrido, remoto
    contrato            VARCHAR(30),                    -- efetivo, estagio, pj... (lista do coletor)
    senioridade         VARCHAR(20),                    -- estagio, trainee, junior, pleno, senior, especialista
    ferramentas         TEXT[]       NOT NULL DEFAULT '{}',
    anos_experiencia    SMALLINT,
    publicada_em        DATE,
    prazo_inscricao     DATE,
    url                 TEXT         NOT NULL,
    descricao           TEXT,
    ativa               BOOLEAN      NOT NULL DEFAULT TRUE,
    vista_por_ultimo_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    criada_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizada_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_vagas_fonte_id_fonte UNIQUE (fonte, id_fonte),
    CONSTRAINT ck_vagas_modelo CHECK (modelo IS NULL OR modelo IN ('presencial', 'hibrido', 'remoto')),
    CONSTRAINT ck_vagas_senioridade CHECK (senioridade IS NULL OR senioridade IN
        ('estagio', 'trainee', 'junior', 'pleno', 'senior', 'especialista')),
    CONSTRAINT ck_vagas_anos CHECK (anos_experiencia IS NULL OR anos_experiencia >= 0)
);

-- Buscas mais comuns: vagas abertas por estado, e as mais recentes
CREATE INDEX ix_vagas_ativas_uf ON vagas (uf) WHERE ativa;
CREATE INDEX ix_vagas_publicada ON vagas (publicada_em DESC);

-- ---------------------------------------------------------------------
-- vagas_usuario passa a apontar para a tabela vagas (antes guardava só o
-- id externo em texto). A tabela ainda está vazia, então é seguro trocar.
-- ---------------------------------------------------------------------
ALTER TABLE vagas_usuario
    DROP COLUMN vaga_id_externo,
    ADD COLUMN vaga_id UUID NOT NULL REFERENCES vagas(id) ON DELETE CASCADE,
    ADD CONSTRAINT ux_vagas_usuario_usuario_vaga UNIQUE (usuario_id, vaga_id);

-- Mesma proteção da V5 (Data API do Supabase não acessa)
ALTER TABLE vagas ENABLE ROW LEVEL SECURITY;
