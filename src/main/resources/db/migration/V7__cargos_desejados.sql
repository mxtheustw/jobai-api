-- =====================================================================
-- V7: cargos que a pessoa procura (até 3). Lista vazia = "ainda não sei".
-- Usados pelo match como termos de busca.
-- =====================================================================

ALTER TABLE perfis
    ADD COLUMN cargos_desejados TEXT[] NOT NULL DEFAULT '{}',
    ADD CONSTRAINT ck_cargos_desejados_max CHECK (cardinality(cargos_desejados) <= 3);
