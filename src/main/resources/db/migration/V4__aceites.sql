-- =====================================================================
-- V4: registro das declarações e aceites (LGPD: guardar só o necessário)
--
-- A idade NUNCA é guardada. Guardamos apenas QUE a pessoa declarou,
-- QUAL versão dos termos e QUANDO.
--
--   TERMOS                 -> no cadastro: aceita os termos e declara
--                             "Tenho 14 anos ou mais"
--   MAIORIDADE_ASSINATURA  -> ao assinar plano pago: "Tenho 18 anos ou mais,
--                             ou sou o responsável e autorizo a assinatura"
--
-- Quando os termos mudarem, entra uma versão nova e o histórico fica.
-- =====================================================================

CREATE TABLE aceites (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo        VARCHAR(30)  NOT NULL,
    versao      VARCHAR(20)  NOT NULL,   -- ex.: 2026-10
    aceito_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_aceites_tipo CHECK (tipo IN ('TERMOS', 'MAIORIDADE_ASSINATURA')),
    CONSTRAINT ux_aceites_usuario_tipo_versao UNIQUE (usuario_id, tipo, versao)
);

CREATE INDEX ix_aceites_usuario ON aceites (usuario_id);
