# API de Contas — JobAI

Serviço responsável pelas **contas dos usuários** do JobAI: cadastro, login, perfil profissional e assinaturas.
É usado pelo site e, no futuro, pelos apps Android e iOS.

**Responsável:** Matheus

## Tecnologias

- Java 21
- Spring Boot 4
- PostgreSQL 18
- Flyway (cria as tabelas automaticamente)
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

| Variável      | Obrigatória | Valor padrão                              | Para que serve                    |
|---------------|-------------|-------------------------------------------|-----------------------------------|
| `DB_PASSWORD` | sim         | —                                         | senha do seu PostgreSQL           |
| `JAVA_HOME`   | sim         | —                                         | caminho do JDK 21                 |
| `DB_URL`      | não         | `jdbc:postgresql://localhost:5432/contas` | endereço do banco                 |
| `DB_USER`     | não         | `postgres`                                | usuário do banco                  |

**No Windows:** menu Iniciar → pesquise "variáveis de ambiente" → **Editar as variáveis de ambiente para sua conta** → **Novo...**
Depois de criar, **feche e abra de novo** o IntelliJ/terminal.

> ⚠️ Nunca escreva senhas direto no `application.properties`. Use sempre as variáveis.

### 4. Rodar a API

Dentro da pasta `api-contas`:

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
| `perfis`        | tudo que o cadastro do site pergunta: momento, áreas, níveis, contratos, modalidades, turnos, escolaridade, experiências, habilidades, idiomas, cidade/UF |
| `planos`        | planos disponíveis (Grátis, Pro, Premium)                                    |
| `assinaturas`   | assinatura de cada usuário e status do pagamento                             |
| `vagas_usuario` | vagas recomendadas, salvas ou candidatadas por usuário                       |

### Regra importante para alterar o banco

As tabelas são criadas pelos arquivos em `src/main/resources/db/migration`.

- **Nunca edite** um arquivo de migração que já foi enviado (ex.: `V1__schema_inicial.sql`).
- Para mudar algo, **crie um arquivo novo** com o próximo número: `V2__descricao_da_mudanca.sql`.

## Endpoints

🚧 **Em construção.** Planejados (podem mudar):

| Método | Rota              | Descrição                         |
|--------|-------------------|-----------------------------------|
| POST   | `/auth/cadastro`  | cria uma conta                    |
| POST   | `/auth/login`     | faz login e devolve os tokens     |
| POST   | `/auth/refresh`   | renova o token de acesso          |
| POST   | `/auth/logout`    | encerra a sessão                  |
| GET    | `/me`             | dados do usuário logado           |
| PUT    | `/me/perfil`      | atualiza o perfil profissional    |