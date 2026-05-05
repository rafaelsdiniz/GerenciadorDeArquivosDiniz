# Gerenciador de Arquivos — Diniz Contabilidade

Sistema de gerenciamento de documentos contábeis com controle de prazos, status de obrigações e histórico de auditoria.

**Stack:** Quarkus 3.32 (Java 21) + Hibernate Panache + PostgreSQL + MinIO + JWT • Frontend Angular separado.

---

## Pré-requisitos

- **Java 21**
- **Maven** (ou usar `./mvnw`)
- **Docker** (para subir MinIO; opcionalmente Postgres)
- **PostgreSQL 14+** rodando local em `localhost:5432`
- **Node.js 20+** + **Angular CLI** (para o frontend)

---

## 1) Banco de dados

Crie o banco `diniz_contabilidade`:

```sql
CREATE DATABASE diniz_contabilidade;
```

Credenciais padrão (em `src/main/resources/application.properties`):

- usuário: `postgres`
- senha: `123456`
- url: `jdbc:postgresql://localhost:5432/diniz_contabilidade`

O Hibernate roda em `drop-and-create`, então o schema e o seed (`import.sql`) são recriados a cada start.

## 2) MinIO

```bash
docker compose up -d
```

Sobe o MinIO em:
- API: <http://localhost:9000> (usado pelo backend)
- Console: <http://localhost:9001> (login `minioadmin` / `minioadmin`)

O bucket `arquivos-contabeis` é criado automaticamente pela aplicação no startup.

## 3) Backend (Quarkus)

```bash
./mvnw quarkus:dev
```

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/q/swagger-ui>
- Dev UI: <http://localhost:8080/q/dev/>

Na primeira subida, o `BootstrapSenhasService` rehasha automaticamente as senhas em texto puro do seed para BCrypt.

### Login de teste

Qualquer usuário do `import.sql` com senha `123456`. Sugeridos:

- `rafael@diniz.com.br` (ADMIN)
- `joao@diniz.com.br` (FUNCIONARIO)

## 4) Frontend (Angular)

No projeto Angular separado:

```bash
npm install
npm start
```

App disponível em <http://localhost:4200> (CORS já liberado nessa origem).

---

## Estrutura

```
src/main/java/diniz/contabilidade/arquivos/
├── dto/                  request/response DTOs (records)
├── exception/            ErroPayload e mappers
├── model/
│   ├── entity/           JPA entities (Empresa, Arquivo, Pasta, Usuario, …)
│   ├── enums/            StatusArquivo, Periodicidade, PerfilUsuario, …
│   └── valueObject/      Cnpj, Cpf, Email, Telefone, Endereco
├── repository/           Panache repositories
├── resource/             REST resources (controllers)
└── service/              regras de negócio + scheduler + bootstrap
```

## Funcionalidades principais

- CRUD de empresas com cadastro completo (endereço, dados fiscais, regime tributário)
- CRUD de sócios (com participação societária e flag de administrador)
- Upload/download de arquivos para o MinIO, com lixeira e restauração
- Pastas hierárquicas por empresa
- **Obrigações recorrentes** (DAS, FGTS, INSS, etc.) com periodicidade
- **Obrigações pendentes** geradas automaticamente, com ações de marcar entregue e prorrogar
- Calendário e dashboard
- Logs de auditoria de todas as ações
- Notificação por e-mail (atualmente em modo *mock* — `quarkus.mailer.mock=true`)
- JWT com perfis ADMIN/FUNCIONARIO + senhas BCrypt

## Comandos úteis

```bash
# Build do backend
./mvnw package

# Rodar testes
./mvnw test

# Empacotar como uber-jar
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

## Configurações relevantes

`src/main/resources/application.properties`:

- `quarkus.minio.url` / `quarkus.minio.access-key` / `quarkus.minio.secret-key` — conexão com MinIO
- `minio.bucket` — nome do bucket usado
- `notificacao.dias-aviso` — quantos dias antes do vencimento avisar (padrão `30,15,7,3,1`)
- `quarkus.mailer.mock` — `true` em dev; trocar para SMTP real em produção
