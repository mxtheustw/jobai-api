-- =====================================================================
-- V3: currículo no perfil + resultado do match nas vagas
--
-- Currículo: o ARQUIVO fica fora do banco (pasta local hoje, Supabase
-- Storage depois). Aqui guardamos só a referência e os dados do arquivo.
--
-- Match: divisão combinada com o time
--   - back-end: filtros objetivos (modalidade, local, nível, contrato, turno)
--   - IA: afinidade (área, curso, habilidades) e os textos
--   - back-end fecha a porcentagem final e garante que a soma dá 100
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Currículo (um por pessoa; enviar outro substitui o anterior)
-- ---------------------------------------------------------------------
ALTER TABLE perfis
    ADD COLUMN curriculo_chave      VARCHAR(255),   -- onde o arquivo está guardado
    ADD COLUMN curriculo_nome       VARCHAR(255),   -- nome original, ex.: "curriculo-ana.pdf"
    ADD COLUMN curriculo_tipo       VARCHAR(100),   -- ex.: application/pdf
    ADD COLUMN curriculo_tamanho    BIGINT,         -- em bytes
    ADD COLUMN curriculo_enviado_em TIMESTAMPTZ,
    ADD CONSTRAINT ck_curriculo_tamanho CHECK (curriculo_tamanho IS NULL OR curriculo_tamanho > 0);

-- ---------------------------------------------------------------------
-- 2. Resultado do match de cada vaga para cada pessoa
-- ---------------------------------------------------------------------
ALTER TABLE vagas_usuario
    -- notas parciais, para entender de onde veio a porcentagem
    ADD COLUMN nota_filtros    NUMERIC(5,2),    -- calculada pelo back-end
    ADD COLUMN nota_afinidade  NUMERIC(5,2),    -- calculada pela IA
    -- textos mostrados ao clicar na vaga
    ADD COLUMN por_que_combina TEXT,
    -- lista do que falta para 100%:
    -- [{ "item": "Inglês intermediário", "tipo": "idioma", "impacto": 10 }]
    ADD COLUMN o_que_falta     JSONB        NOT NULL DEFAULT '[]',
    ADD COLUMN calculado_em    TIMESTAMPTZ,
    ADD CONSTRAINT ck_score_ia       CHECK (score_ia IS NULL OR score_ia BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_nota_filtros   CHECK (nota_filtros IS NULL OR nota_filtros BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_nota_afinidade CHECK (nota_afinidade IS NULL OR nota_afinidade BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_o_que_falta_lista CHECK (jsonb_typeof(o_que_falta) = 'array');

-- O feed busca "as vagas desta pessoa, da maior porcentagem para a menor"
CREATE INDEX ix_vagas_usuario_feed ON vagas_usuario (usuario_id, score_ia DESC);
