-- =====================================================================
-- V8: cidades onde a pessoa quer trabalhar + controle do limite semanal do Grátis
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Cidades onde a pessoa quer trabalhar (até 5) ou "qualquer cidade".
--    A cidade onde mora continua em perfis.cidade / perfis.uf.
--    Formato: [{ "cidade": "Campinas", "uf": "SP" }]
-- ---------------------------------------------------------------------
ALTER TABLE perfis
    ADD COLUMN cidades_trabalho JSONB   NOT NULL DEFAULT '[]',
    ADD COLUMN qualquer_cidade  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_cidades_trabalho_lista CHECK (jsonb_typeof(cidades_trabalho) = 'array'),
    ADD CONSTRAINT ck_cidades_trabalho_max   CHECK (jsonb_array_length(cidades_trabalho) <= 5);

-- ---------------------------------------------------------------------
-- 2. Quando a vaga foi "liberada" para a pessoa ver.
--    No Grátis, a API libera no máximo 3 vagas por semana (as de maior %).
--    Vagas sem porcentagem (score_ia nulo) são as "outras vagas que encontramos".
-- ---------------------------------------------------------------------
ALTER TABLE vagas_usuario
    ADD COLUMN liberada_em TIMESTAMPTZ;

CREATE INDEX ix_vagas_usuario_liberada ON vagas_usuario (usuario_id, liberada_em);
