-- =====================================================================
-- V5: liga o RLS (Row Level Security) em todas as tabelas
--
-- Por quê: no Supabase, as tabelas do schema "public" podem ser lidas
-- pela "Data API" do Supabase usando uma chave que é PÚBLICA (anon key).
-- Nós não usamos essa API: tudo passa pela api-contas.
--
-- Ligando o RLS sem criar nenhuma regra (policy), a Data API não
-- consegue ler nem escrever nada. A api-contas continua funcionando
-- normalmente, porque ela entra como dona das tabelas (e o dono passa
-- por cima do RLS).
--
-- No banco local (seu PC) isso não muda nada.
-- =====================================================================

ALTER TABLE usuarios      ENABLE ROW LEVEL SECURITY;
ALTER TABLE sessoes       ENABLE ROW LEVEL SECURITY;
ALTER TABLE tokens_email  ENABLE ROW LEVEL SECURITY;
ALTER TABLE perfis        ENABLE ROW LEVEL SECURITY;
ALTER TABLE planos        ENABLE ROW LEVEL SECURITY;
ALTER TABLE assinaturas   ENABLE ROW LEVEL SECURITY;
ALTER TABLE vagas_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE aceites       ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS flyway_schema_history ENABLE ROW LEVEL SECURITY;
