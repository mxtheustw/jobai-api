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

**Gerar um `JWT_SECRET`** no PowerShell:

```
-join ((1..48) | ForEach-Object { [char](Get-Random -Minimum 65 -Maximum 91) })
```

**No Windows:** menu Iniciar → pesquise "variáveis de ambiente" → **Editar as variáveis de ambiente para sua conta** → **Novo...**
Depois de criar, **feche e abra de novo** o IntelliJ/terminal.

> ⚠️ Nunca escreva senhas ou chaves direto no `application.properties`. Use sempre as variáveis.

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

## Estrutura do banco

| Tabela          | O que guarda                                                                 |
|-----------------|------------------------------------------------------------------------------|
| `usuarios`      | dados de login (nome, e-mail, senha criptografada)                           |
| `sessoes`       | logins ativos em cada dispositivo (web, Android, iOS)                        |
| `tokens_email`  | confirmação de e-mail e recuperação de senha                                 |
| `perfis`        | tudo que o cadastro do site pergunta (momento, áreas, níveis, contratos, modalidades, turnos, escolaridade, experiências, habilidades, idiomas, cidade/UF) + a referência do currículo |
| `planos`        | planos disponíveis (Grátis, Pro, Premium)                                    |
| `assinaturas`   | assinatura de cada usuário e status do pagamento                             |
| `vagas_usuario` | vagas de cada usuário + resultado do match (porcentagem, por que combina, o que falta) |

### Regra importante para alterar o banco

As tabelas são criadas pelos arquivos em `src/main/resources/db/migration`.

- **Nunca edite** um arquivo de migração que já foi enviado (ex.: `V1__schema_inicial.sql`).
- Para mudar algo, **crie um arquivo novo** com o próximo número: `V4__descricao_da_mudanca.sql`.

## Endpoints (para o front)

Endereço base: `http://localhost:8080`. Tudo é JSON.

| Método | Rota             | Precisa estar logado? | O que faz                                   |
|--------|------------------|-----------------------|---------------------------------------------|
| POST   | `/auth/cadastro` | não                   | cria a conta + perfil e já devolve logado   |
| POST   | `/auth/login`    | não                   | entra com e-mail e senha                    |
| POST   | `/auth/google`   | não                   | entra (ou cria conta) com Google            |
| POST   | `/auth/refresh`  | não                   | troca o refreshToken por tokens novos       |
| POST   | `/auth/logout`   | não                   | encerra a sessão daquele dispositivo        |
| GET    | `/me`            | sim                   | dados da conta + perfil                     |
| PUT    | `/me/perfil`     | sim                   | atualiza o perfil                           |
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
    "idiomas": [{ "idioma": "Inglês", "nivel": "basico" }]
  },
  "senha": "minhasenha123",
  "dispositivo": "WEB"
}
```

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
  "perfil": { "...mesmo formato do Profile..." },
  "curriculo": { "nome": "curriculo-ana.pdf", "tamanhoBytes": 183204, "enviadoEm": "2026-10-20T23:10:00Z" }
}
```

`curriculo` vem `null` enquanto a pessoa não enviar um.

### `PUT /me/perfil`

Envie o `Profile` completo (mesmo formato do cadastro, sem a senha). O e-mail não muda por aqui.

### Currículo: `POST /me/curriculo`

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
| 404    | currículo não enviado                               |
| 409    | já existe conta com esse e-mail                     |
| 413    | arquivo maior que 5 MB                              |
| 503    | "Entrar com Google" ainda não configurado           |
