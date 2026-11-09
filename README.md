# API de Contas — JobAI

Serviço responsável pelas **contas dos usuários** do JobAI: cadastro, login, perfil profissional e assinaturas.
É usado pelo site e, no futuro, pelos apps Android e iOS.

**Responsável:** Matheus

## Tecnologias

- Java 21
- Spring Boot 4
- PostgreSQL 18
- Flyway (cria as tabelas automaticamente)
- Spring Security + JWT (login)
- Maven (via Maven Wrapper, não precisa instalar)

## Como rodar no seu computador

### 1. Pré-requisitos

- **Java 21 (JDK)** instalado
- **PostgreSQL 18** instalado e rodando

### 2. Criar o banco de dados

No PowerShell:

```
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres
```

Digite a senha do PostgreSQL e depois:

```sql
CREATE DATABASE contas;
```

Digite `\q` para sair.

### 3. Configurar as variáveis de ambiente

| Variável           | Obrigatória | Valor padrão                              | Para que serve                                   |
|--------------------|-------------|-------------------------------------------|--------------------------------------------------|
| `DB_PASSWORD`      | sim         | —                                         | senha do seu PostgreSQL                          |
| `JWT_SECRET`       | sim         | —                                         | chave que assina os logins (mínimo 32 caracteres)|
| `JAVA_HOME`        | sim         | —                                         | caminho do JDK 21                                |
| `DB_URL`           | não         | `jdbc:postgresql://localhost:5432/contas` | endereço do banco                                |
| `DB_USER`          | não         | `postgres`                                | usuário do banco                                 |
| `GOOGLE_CLIENT_ID` | não         | vazio (Google desligado)                  | ID do app no Google Cloud, para "Entrar com Google" |
| `CORS_ORIGENS`     | não         | `http://localhost:3000`                   | sites que podem chamar a API (separados por vírgula) |
| `CURRICULOS_PASTA` | não         | `dados/curriculos`                        | pasta onde os currículos ficam guardados         |
| `INTERNO_CHAVE`    | não         | vazio (rotas internas desligadas)         | chave do coletor/match para `/interno/**` (mínimo 32 caracteres) |
| `PLANOS_LIBERAR_TUDO` | não      | `false`                                   | só para teste: trata todos como plano pago (mostra o "o que falta") |
| `TERMOS_VERSAO`    | não         | `2026-10`                                 | versão atual dos termos de uso                   |
| `BREVO_API_KEY`    | não         | vazio (e-mails só aparecem no log)        | chave da Brevo, que envia os e-mails             |
| `EMAIL_REMETENTE`  | não         | vazio                                     | e-mail que aparece como remetente (validado na Brevo) |
| `SITE_URL`         | não         | `http://localhost:3000`                   | endereço do site, usado nos links dos e-mails    |

**Gerar um `JWT_SECRET`** no PowerShell:

```
-join ((1..48) | ForEach-Object { [char](Get-Random -Minimum 65 -Maximum 91) })
```

**No Windows:** menu Iniciar → pesquise "variáveis de ambiente" → **Editar as variáveis de ambiente para sua conta** → **Novo...**
Depois de criar, **feche e abra de novo** o IntelliJ/terminal.

> ⚠️ Nunca escreva senhas ou chaves direto no `application.properties`. Use sempre as variáveis.

### Usar o banco do Supabase (compartilhado pelo time)

Em vez do banco local, a API pode usar o Postgres do Supabase. Não muda nenhuma linha de código:
só as variáveis de ambiente.

| Variável      | Valor                                                                                   |
|---------------|-----------------------------------------------------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://<host-do-pooler>:5432/postgres?sslmode=require`                      |
| `DB_USER`     | `postgres.<id-do-projeto>`                                                              |
| `DB_PASSWORD` | a senha do banco do Supabase (peça no gerenciador de senhas do time)                    |

- Os dados ficam em **Supabase → Connect → Session pooler** (funciona em qualquer internet, IPv4).
- Na primeira vez, o Flyway cria todas as tabelas no Supabase sozinho.
- As tabelas têm **RLS ligado** (V5), então a "Data API" do Supabase não acessa nada: tudo passa pela api-contas.
- Para voltar ao banco local, apague `DB_URL` e `DB_USER` e volte o `DB_PASSWORD` para a senha local.
- Plano grátis: o projeto **pausa depois de uma semana sem uso**. Para reativar: painel do Supabase → **Restore**.
- Os **currículos** ainda ficam na pasta local de quem roda a API (o Supabase Storage é o próximo passo).

### 4. Rodar a API

Dentro da pasta `backend/api-contas`:

```
# Windows
.\mvnw.cmd spring-boot:run

# Mac / Linux
./mvnw spring-boot:run
```

A API sobe em **http://localhost:8080**.
Na primeira execução, o Flyway cria todas as tabelas sozinho.

Para parar: `Ctrl + C`.

## Limpeza automática do banco

A própria API apaga, sozinha, o que não serve mais: **sessões vencidas ou encerradas** (logout, troca de senha,
refresh já usado) e **links de "esqueci minha senha" vencidos ou já usados**. Roda 5 minutos depois que a API
liga e, depois, a cada 6 horas. Quando apaga algo, aparece no log: `Limpeza do banco: X sessões e Y links...`.

Economiza espaço no Supabase grátis e segue a LGPD (não guardar dados além do necessário).

## Testes automáticos

São programas que conferem sozinhos se a API continua funcionando: cadastro, login, bloqueio por senhas erradas,
"esqueci minha senha", plano Grátis (3 vagas por semana), rotas internas, exportar dados e excluir conta.
**Rode antes de mandar qualquer mudança para a `main`.**

Os testes usam um banco **separado**, só deles: `contas_teste`, no PostgreSQL do seu PC. Nunca o Supabase.

### Preparar (uma vez só)

1. No **pgAdmin**: clique com o botão direito em **Databases** → **Create** → **Database...** → nome `contas_teste` → **Save**.
2. Crie a variável de ambiente `DB_PASSWORD_TESTE` com a senha do PostgreSQL **do seu PC**
   (a mesma que você usa no pgAdmin). Feche e abra o terminal depois.

### Rodar

```
cd backend\api-contas
.\mvnw.cmd test
```

No fim aparece `Tests run: X, Failures: 0, Errors: 0` e **BUILD SUCCESS**. Se algum falhar, o nome do teste
diz o que quebrou (ex.: `AuthTest.bloqueiaDepoisDe5SenhasErradas`).

Os testes ficam em `src/test/java/com/vagas/contas/`. Nenhum e-mail sai de verdade: o envio é trocado por um "dublê".

## Hospedagem (Render, plano grátis)

A API roda no **Render** a partir do `Dockerfile` desta pasta.

| Configuração no Render | Valor                                  |
|------------------------|----------------------------------------|
| Tipo                   | Web Service                            |
| Root Directory         | `backend/api-contas`                   |
| Runtime / Language     | Docker                                 |
| Region                 | Oregon (US West), a mesma do Supabase  |
| Instance Type          | Free                                   |
| Health Check Path      | `/saude`                               |

Variáveis de ambiente no Render: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (**uma chave nova, só do servidor**),
`GOOGLE_CLIENT_ID`, `CORS_ORIGENS`, `INTERNO_CHAVE`, `BREVO_API_KEY`, `EMAIL_REMETENTE` e `SITE_URL`. A porta vem sozinha (`PORT`).

Limites do plano grátis:
- **Dorme depois de 15 minutos sem uso** e leva por volta de 1 minuto (ou mais) para acordar. O site pode chamar
  `GET /saude` ao abrir, para ir acordando a API.
- **Arquivos não ficam salvos**: a cada reinício o disco é zerado. Por isso os currículos precisam ir para o
  Supabase Storage antes de usar isso de verdade.
- Toda vez que a `main` muda, o Render publica a versão nova sozinho.

## Estrutura do banco

| Tabela          | O que guarda                                                                 |
|-----------------|------------------------------------------------------------------------------|
| `usuarios`      | dados de login (nome, e-mail, senha criptografada)                           |
| `sessoes`       | logins ativos em cada dispositivo (web, Android, iOS)                        |
| `tokens_email`  | confirmação de e-mail e recuperação de senha                                 |
| `perfis`        | tudo que o cadastro do site pergunta (momento, áreas, níveis, contratos, modalidades, turnos, escolaridade, experiências, habilidades, idiomas, cidade/UF) + a referência do currículo |
| `planos`        | planos disponíveis (Grátis, Pro, Premium)                                    |
| `assinaturas`   | assinatura de cada usuário e status do pagamento                             |
| `aceites`       | declarações: aceite dos termos ("Tenho 14 anos ou mais") e maioridade para plano pago. Guarda tipo, versão e data, **nunca a idade** |
| `vagas`         | vagas coletadas (Gupy etc.), únicas por `fonte` + `id_fonte`, com `ativa` e `vista_por_ultimo_em` |
| `vagas_usuario` | vagas de cada usuário + resultado do match (porcentagem, por que combina, o que falta) |

### Regra importante para alterar o banco

As tabelas são criadas pelos arquivos em `src/main/resources/db/migration`.

- **Nunca edite** um arquivo de migração que já foi enviado (ex.: `V1__schema_inicial.sql`).
- Para mudar algo, **crie um arquivo novo** com o próximo número: `V9__descricao_da_mudanca.sql`.

## Endpoints (para o front)

Endereço base: `http://localhost:8080`. Tudo é JSON.

| Método | Rota             | Precisa estar logado? | O que faz                                   |
|--------|------------------|-----------------------|---------------------------------------------|
| GET    | `/saude`         | não                   | responde `{"status":"ok"}` se a API está no ar |
| POST   | `/auth/cadastro` | não                   | cria a conta + perfil e já devolve logado   |
| POST   | `/auth/login`    | não                   | entra com e-mail e senha                    |
| POST   | `/auth/google`   | não                   | entra (ou cria conta) com Google            |
| POST   | `/auth/refresh`  | não                   | troca o refreshToken por tokens novos       |
| POST   | `/auth/logout`   | não                   | encerra a sessão daquele dispositivo        |
| POST   | `/auth/esqueci-senha`   | não            | manda por e-mail o link para criar senha nova |
| POST   | `/auth/redefinir-senha` | não            | cria a senha nova com o código do link      |
| POST   | `/auth/confirmar-email` | não            | confirma o e-mail com o código do link      |
| POST   | `/me/reenviar-confirmacao` | sim         | manda de novo o link de confirmar e-mail    |
| GET    | `/me`            | sim                   | dados da conta + perfil                     |
| PUT    | `/me/perfil`     | sim                   | atualiza o perfil                           |
| GET    | `/me/vagas`      | sim                   | vagas da pessoa com o match (maior % primeiro) |
| GET    | `/me/vagas/outras` | sim                 | "outras vagas que encontramos" (sem %)      |
| GET    | `/me/vagas/{id}` | sim                   | uma vaga com o match                        |
| PUT    | `/me/senha`      | sim                   | troca a senha                               |
| DELETE | `/me`            | sim                   | exclui a conta e todos os dados             |
| GET    | `/me/exportar`   | sim                   | baixa um arquivo com todos os dados da pessoa (LGPD) |
| POST   | `/me/aceites`    | sim                   | registra um aceite (termos ou maioridade)   |
| POST   | `/me/curriculo`  | sim                   | envia ou substitui o currículo (PDF)        |
| GET    | `/me/curriculo`  | sim                   | baixa o currículo enviado                   |
| DELETE | `/me/curriculo`  | sim                   | remove o currículo                          |

### Como funciona o login

Cadastro, login, Google e refresh devolvem:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "q1w2e3...",
  "tipo": "Bearer",
  "expiraEmSegundos": 900,
  "usuario": { "id": "…", "nome": "Ana", "email": "ana@email.com" },
  "novoUsuario": true
}
```

- **accessToken**: vale 15 minutos. Envie em toda rota que precisa de login, no cabeçalho
  `Authorization: Bearer <accessToken>`.
- **refreshToken**: vale 30 dias. Quando uma rota responder **401**, chame `/auth/refresh` com ele para pegar tokens novos.
  Cada refreshToken só pode ser usado **uma vez** (sempre guarde o novo).
- **novoUsuario**: no Google, `true` quer dizer que a conta acabou de ser criada → leve a pessoa ao cadastro e salve com `PUT /me/perfil`.

### `POST /auth/cadastro`

O campo `perfil` é **exatamente o `Profile`** de `frontend/src/types/profile.ts`:

```json
{
  "perfil": {
    "nome": "Ana Souza",
    "email": "ana@email.com",
    "local": { "cidade": "São Paulo", "uf": "SP" },
    "momento": "primeiro-emprego",
    "areas": ["Tecnologia"],
    "niveis": ["estagio", "junior"],
    "contratos": ["clt"],
    "modalidades": ["remoto", "hibrido"],
    "alcance": "minha-cidade",
    "turnos": ["integral"],
    "escolaridade": { "nivel": "superior", "status": "cursando" },
    "curso": "Análise e Desenvolvimento de Sistemas",
    "experiencias": ["projeto"],
    "habilidades": ["Excel", "Comunicação"],
    "idiomas": [{ "idioma": "Inglês", "nivel": "basico" }],
    "cargosDesejados": ["Analista de Dados", "Assistente Administrativo"],
    "cidadesTrabalho": [{ "cidade": "Campinas", "uf": "SP" }],
    "qualquerCidade": false
  },
  "senha": "minhasenha123",
  "aceiteTermos": { "versao": "2026-10" },
  "dispositivo": "WEB"
}
```

- `aceiteTermos`: **obrigatório**. É o checkbox dos termos com "Tenho 14 anos ou mais". Sem ele (ou com versão diferente da atual), a conta não é criada.

- `cargosDesejados`: até 3 textos. Lista vazia = "ainda não sei".
- `cidadesTrabalho`: até 5 `{ cidade, uf }` onde a pessoa quer trabalhar (a cidade onde mora fica em `local`).
  `qualquerCidade: true` aceita qualquer cidade, e aí a lista vem vazia. Remoto depende de `modalidades`.
- `senha`: de 8 a 72 caracteres.
- `dispositivo`: `WEB`, `ANDROID` ou `IOS` (se não enviar, vale `WEB`).
- Os valores das listas (ex.: `"estagio"`) precisam ser os mesmos do `profile.ts`. Valores diferentes são recusados.

### `POST /auth/login`

```json
{ "email": "ana@email.com", "senha": "minhasenha123", "dispositivo": "WEB" }
```

### `POST /auth/google`

```json
{ "idToken": "<credential devolvido pelo botão do Google>", "dispositivo": "WEB" }
```

Só funciona quando `GOOGLE_CLIENT_ID` estiver configurado (o mesmo Client ID usado no botão do site).

### `POST /auth/refresh` e `POST /auth/logout`

```json
{ "refreshToken": "q1w2e3..." }
```

### `GET /me`

```json
{
  "id": "…",
  "provedor": "LOCAL",
  "emailVerificado": false,
  "termosAceitos": true,
  "perfilCompleto": true,
  "perfil": { "...mesmo formato do Profile..." },
  "curriculo": { "nome": "curriculo-ana.pdf", "tamanhoBytes": 183204, "enviadoEm": "2026-10-20T23:10:00Z" }
}
```

`curriculo` vem `null` enquanto a pessoa não enviar um.

- `termosAceitos`: a pessoa aceitou a **versão atual** dos termos.
- `perfilCompleto`: respondeu as perguntas obrigatórias do cadastro: cidade/UF, momento, áreas, níveis, contratos,
  modalidades, turnos e escolaridade (habilidades e idiomas podem ficar vazios).

### `PUT /me/perfil`

Envie o `Profile` completo (mesmo formato do cadastro, sem a senha). O e-mail não muda por aqui.
Exige que a pessoa tenha aceitado os termos (senão responde **403**).

### `GET /me/vagas` e `GET /me/vagas/{id}`

`GET /me/vagas?limite=50` (de 1 a 100) devolve uma lista, da maior porcentagem para a menor, só com vagas abertas.
`GET /me/vagas/{id}` devolve uma (o `id` é o `vaga.id`). Formato de cada item:

```json
{
  "vaga": {
    "id": "…", "fonte": "gupy", "titulo": "Estágio em Dados", "empresa": "Empresa X",
    "cidade": "São Paulo", "uf": "SP", "modelo": "hibrido", "contrato": "estagio", "senioridade": "estagio",
    "ferramentas": ["SQL", "Excel"], "anosExperiencia": null,
    "publicadaEm": "2026-10-25", "prazoInscricao": null, "encontradaEm": "2026-10-26T09:12:00Z",
    "url": "https://...", "descricao": "..."
  },
  "match": {
    "percentual": 82,
    "porQueCombina": "Pede Excel e SQL, e você tem os dois.",
    "oQueFalta": [{ "item": "Power BI", "tipo": "habilidade", "impacto": 18 }],
    "oQueFaltaBloqueado": false,
    "destaque": true
  }
}
```

- `encontradaEm`: quando o coletor achou a vaga. Sempre vem preenchido (use quando `publicadaEm` vier `null`).
- `destaque`: `true` quando o percentual é **80 ou mais** (selo amarelo).
- No plano **Grátis**:
  - aparecem só **3 vagas novas por semana** (segunda a domingo), as de maior %. Elas são "liberadas" na primeira vez
    que a pessoa abre o feed na semana e continuam aparecendo nas semanas seguintes (enquanto a vaga estiver aberta);
  - `oQueFalta` vem `[]` e `oQueFaltaBloqueado` vem `true`;
  - `GET /me/vagas/{id}` de uma vaga não liberada responde **404**.
- `GET /me/vagas/outras?limite=20`: vagas sem porcentagem ("outras vagas que encontramos"), com `match: null`,
  das mais recentes para as mais antigas. O site decide quando mostrar (ex.: quando vierem poucas vagas com match).
- Para testar o "o que falta" sem pagamento, rode a API com `PLANOS_LIBERAR_TUDO=true` (nunca no servidor de verdade).

### `PUT /me/senha`

```json
{ "senhaAtual": "minhasenha123", "novaSenha": "outrasenha456", "dispositivo": "WEB" }
```

- A nova senha segue a mesma regra do cadastro (8 a 72 caracteres) e precisa ser diferente da atual.
- Por segurança, **encerra a sessão em todos os dispositivos** e devolve tokens novos (mesmo formato do login)
  só para quem trocou. Guarde os tokens novos.
- Contas que entram só pelo Google não têm senha: respondem 400.

### Esqueci minha senha: `POST /auth/esqueci-senha` e `POST /auth/redefinir-senha`

1. Na tela "Esqueci minha senha", o site manda:
   ```json
   { "email": "ana@email.com" }
   ```
   A resposta é **sempre** a mesma, exista ou não a conta (assim ninguém descobre quem está cadastrado):
   ```json
   { "mensagem": "Se existir uma conta com esse e-mail, enviamos um link para criar uma senha nova." }
   ```
   Se a conta existir, chega um e-mail com o link `SITE_URL/redefinir-senha?token=...`, que **vale por 1 hora**.
   Só o último link pedido vale. Contas só do Google recebem um e-mail dizendo para entrar com o Google.
   Limite: 5 pedidos a cada 15 minutos por e-mail (depois, 429).
2. A página `/redefinir-senha` do site pega o `token` da URL e manda:
   ```json
   { "token": "o-token-da-url", "novaSenha": "minhasenhanova123" }
   ```
   - Resposta **204**: senha trocada. Mande a pessoa para o login.
   - **Encerra a sessão em todos os dispositivos** e libera o login, se estava bloqueado por senhas erradas.
   - Link usado, vencido ou inválido: **400** com `"Esse link é inválido ou já venceu. Peça um novo em \"Esqueci minha senha\"."`

Os e-mails saem pela **Brevo**. Sem `BREVO_API_KEY`, nada é enviado; no seu PC (com `SITE_URL` em localhost),
o link aparece no log da API, para testar.

### Confirmar e-mail: `POST /auth/confirmar-email` e `POST /me/reenviar-confirmacao`

Quem se cadastra com e-mail e senha recebe na hora um e-mail "Confirme seu e-mail no JobAI" com o link
`SITE_URL/confirmar-email?token=...`, que **vale por 24 horas**. Quem entra pelo Google já chega confirmado.

- O `GET /me` diz se já confirmou: `"emailVerificado": true/false`. **Por enquanto quem não confirmou usa o site
  normalmente**; o site só mostra um aviso ("Confirme seu e-mail") com um botão de reenviar.
- A página `/confirmar-email` do site pega o `token` da URL e manda:
  ```json
  { "token": "o-token-da-url" }
  ```
  **204** = confirmado. **400** = link usado, vencido ou inválido (`"Esse link é inválido ou já venceu. Entre na sua conta e peça um novo."`).
  Não precisa estar logado (a pessoa pode abrir o link no celular).
- `POST /me/reenviar-confirmacao` (com token, sem corpo): manda um link novo; o anterior para de valer.
  Se já estiver confirmado, não faz nada. Limite: 5 pedidos a cada 15 minutos (depois, 429).
- Redefinir a senha pelo "esqueci minha senha" também confirma o e-mail (a pessoa abriu um link que chegou nele).

**Proteção:** se alguém criar uma conta com o e-mail de outra pessoa e não confirmar, e a dona do e-mail depois
entrar com o Google, a API apaga a senha criada pelo intruso e derruba as sessões dele. A conta passa a ser só da
dona do e-mail (entrando pelo Google).

### `DELETE /me`

```json
{ "senha": "minhasenha123" }
```

- Exclui a conta **e tudo ligado a ela**: perfil, currículo (o arquivo também), sessões, aceites, assinaturas e vagas.
  **Não tem volta.**
- Contas com senha precisam mandar a `senha` para confirmar. Contas do Google podem mandar `{}`
  (o site deve pedir uma confirmação na tela antes).
- Resposta: **204** (sem corpo). Depois disso, apague os tokens guardados no site/app.
- ⚠️ Quando existirem pagamentos, a exclusão também vai precisar cancelar a assinatura no meio de pagamento.

### `GET /me/exportar` (Baixar meus dados)

Direito de acesso e portabilidade da LGPD. Devolve um arquivo `meus-dados-jobai.json` com:

| Campo           | O que tem                                                                 |
|-----------------|---------------------------------------------------------------------------|
| `conta`         | id, nome, e-mail, forma de entrar, id do Google (se tiver), data de criação |
| `perfil`        | o perfil completo (mesmo formato do `GET /me`)                            |
| `curriculo`     | nome, tamanho e data do currículo (o arquivo em si fica em `GET /me/curriculo`) |
| `plano`         | `GRATIS` ou `PAGO`                                                        |
| `aceites`       | termos e declarações aceitos, com versão e data                          |
| `sessoes`       | dispositivos em que a pessoa entrou, quando, e se ainda estão ativos     |
| `vagasComMatch` | as vagas com match, **seguindo as regras do plano** (igual ao `GET /me/vagas`) |
| `outrasVagas`   | as "outras vagas que encontramos"                                         |

**Não entram:** a senha (nem criptografada) e os códigos de login. No site, basta um botão "Baixar meus dados"
que chama a rota com o token e salva a resposta como arquivo.

### `POST /me/aceites`

```json
{ "tipo": "TERMOS", "versao": "2026-10" }
```

- `tipo`: `TERMOS` (aceite dos termos + "Tenho 14 anos ou mais") ou `MAIORIDADE_ASSINATURA`
  ("Tenho 18 anos ou mais, ou sou o responsável e autorizo a assinatura").
- Resposta (201): `{ "tipo": "TERMOS", "versao": "2026-10", "aceitoEm": "..." }`. Repetir não dá erro.
- A API **não guarda idade**: só a declaração, a versão e a data.

### Fluxo do cadastro pelo Google

1. A pessoa marca o checkbox dos termos e clica em "Entrar com Google".
2. `POST /auth/google` → se `novoUsuario: true`, a conta acabou de ser criada.
3. `POST /me/aceites` com `{ "tipo": "TERMOS", "versao": "2026-10" }`.
4. `PUT /me/perfil` com as respostas do cadastro.

Se alguém entrar pelo Google na tela de **login** sem ter conta: o `GET /me` vem com `termosAceitos: false`
e `perfilCompleto: false` → mostre os termos e mande para o cadastro.

### Currículo: `POST /me/curriculo`

> ⚠️ **Desligado na primeira versão** (responde **503**): o JobAI não guarda currículos, como diz a política de
> privacidade. Para religar no futuro: `CURRICULOS_HABILITADO=true`.

- Formato **multipart/form-data**, com o arquivo no campo `arquivo`.
- Só **PDF**, até **5 MB**. Enviar outro substitui o anterior.
- Resposta (201): `{ "nome": "...", "tamanhoBytes": 183204, "enviadoEm": "..." }`

No front (exemplo):

```ts
const dados = new FormData();
dados.append("arquivo", arquivoPdf); // File vindo do <input type="file">
await fetch("http://localhost:8080/me/curriculo", {
  method: "POST",
  headers: { Authorization: `Bearer ${accessToken}` }, // NÃO defina Content-Type: o navegador faz isso
  body: dados,
});
```

Os arquivos ficam na pasta `dados/curriculos` (ignorada pelo Git). Quando o banco for para o Supabase,
eles passam para o **Supabase Storage** sem mudar as rotas.

### Rotas internas (coletor e match)

Não são usadas pelo site. Exigem o cabeçalho `X-Chave-Interna: <INTERNO_CHAVE>` (sem ele: **401**).
Se `INTERNO_CHAVE` não estiver configurada, respondem **503**.

#### `POST /interno/vagas`

Até **500 vagas por envio**. Vaga nova é criada; vaga já conhecida (mesma `fonte` + `id_fonte`) é atualizada,
reativada e marcada como vista agora.

```json
{
  "vagas": [
    {
      "fonte": "gupy",
      "id_fonte": "123456",
      "titulo": "Estágio em Dados",
      "empresa": "Empresa X",
      "cidade": "São Paulo",
      "uf": "SP",
      "modelo": "hibrido",
      "contrato": "estagio",
      "senioridade": "estagio",
      "ferramentas": ["SQL", "Excel"],
      "anos_experiencia": null,
      "publicada_em": "2026-10-25",
      "prazo_inscricao": null,
      "url": "https://...",
      "descricao": "Texto da vaga..."
    }
  ]
}
```

- Os nomes podem vir como no CSV do coletor (`id_fonte`) ou em camelCase (`idFonte`).
- Obrigatórios: `fonte`, `id_fonte`, `titulo`, `url`. Campo sem valor: mande `null` (ou texto vazio).
- `ferramentas` é uma **lista** (no CSV vem separada por `;`, então dê um `split(";")`).
- `modelo`: `presencial`, `hibrido` ou `remoto`. `senioridade`: `estagio`, `trainee`, `junior`, `pleno`, `senior`,
  `especialista`. Outro valor faz **só aquela vaga** ser recusada.
- Datas no formato `AAAA-MM-DD`.

Resposta:

```json
{ "recebidas": 120, "novas": 80, "atualizadas": 38, "recusadas": [ { "indice": 7, "motivo": "url vazia" } ] }
```

#### `GET /interno/perfis?pagina=0&tamanho=100`

Perfis para o match calcular, **sem nome e sem e-mail**. Só aparecem perfis completos de quem aceitou os termos.
Continue pedindo a próxima `pagina` enquanto `temMais` for `true`.

```json
{ "perfis": [ { "usuarioId": "…", "perfil": { "...Profile, com cargosDesejados..." } } ], "pagina": 0, "temMais": false }
```

#### `POST /interno/matches`

Grava o resultado do match (até **1000 por envio**). O mesmo par pessoa + vaga é atualizado, não duplica.

```json
{
  "matches": [
    {
      "usuarioId": "…",
      "fonte": "gupy",
      "id_fonte": "123456",
      "percentual": 82,
      "nota_filtros": 90,
      "nota_afinidade": 75,
      "por_que_combina": "Pede Excel e SQL, e você tem os dois.",
      "o_que_falta": [ { "item": "Power BI", "tipo": "habilidade", "impacto": 18 } ]
    }
  ]
}
```

- A vaga precisa já existir (enviada antes em `/interno/vagas`).
- **Vaga sem match** (ex.: as que vêm de busca no Google): mande `percentual: null` e `o_que_falta` vazio.
  Ela vai para `GET /me/vagas/outras`.
- Regra do time: se `o_que_falta` tiver itens, **percentual + soma dos impacto = 100** (folga de 1 ponto). Senão, só
  aquele match é recusado.
- Nomes também em camelCase (`idFonte`, `porQueCombina`, `oQueFalta`...).
- Resposta: `{ "recebidos": 10, "novos": 8, "atualizados": 2, "recusados": [ { "indice": 3, "motivo": "..." } ] }`

#### `POST /interno/vagas/expirar`

```json
{ "fonte": "gupy", "diasSemVer": 3 }
```

Desativa as vagas dessa fonte que o coletor não encontra há `diasSemVer` dias (padrão 3) e todas as vagas com
prazo de inscrição vencido. Rode depois de cada coleta. Resposta: `{ "naoVistas": 12, "prazoVencido": 4 }`.

### Erros

Todo erro vem neste formato, pronto para mostrar na tela:

```json
{
  "erro": "Confira os campos destacados.",
  "campos": { "senha": "A senha precisa ter entre 8 e 72 caracteres." }
}
```

| Status | Quando acontece                                     |
|--------|-----------------------------------------------------|
| 400    | dados inválidos ou opção que não existe no cadastro |
| 401    | e-mail/senha errados, ou token vencido/inválido     |
| 403    | termos ainda não aceitos (salvar perfil, enviar currículo) |
| 404    | currículo não enviado                               |
| 409    | já existe conta com esse e-mail                     |
| 429    | muitas senhas erradas seguidas (5 em 15 min): login, trocar senha e excluir conta; ou muitos pedidos de "esqueci minha senha" |
| 413    | arquivo maior que 5 MB                              |
| 503    | "Entrar com Google" ainda não configurado           |
