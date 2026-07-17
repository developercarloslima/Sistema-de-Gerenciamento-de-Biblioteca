# Library Management API

Sistema back-end completo para gerenciamento de biblioteca, desenvolvido com **Java 21, Spring Boot 3.5, Spring Security/JWT, PostgreSQL, Flyway e Docker**.

> Correção incluída: filtros vazios de busca são enviados como string vazia para evitar `lower(bytea)` no PostgreSQL/Hibernate.

## Funcionalidades

- Autenticação JWT com access token e refresh token.
- Perfis `ADMIN`, `LIBRARIAN` e `MEMBER`.
- Cadastro, consulta, edição e desativação de livros.
- Controle individual de exemplares e disponibilidade.
- Cadastro e bloqueio de membros.
- Empréstimos com limite por membro e bloqueio por atraso/multas.
- Devoluções com cálculo automático de multa.
- Renovações com validação de reservas e limite configurável.
- Fila de reservas e prazo automático para retirada.
- Notificações internas de vencimento, atraso, reserva e multa.
- Tarefa agendada diária para processar prazos.
- Swagger/OpenAPI.
- Endpoint de perfil autenticado e administração de funcionários.
- Collection do Postman pronta para importar.
- Pipeline de CI com GitHub Actions.
- Migrations com Flyway.
- Concorrência protegida por lock pessimista e índice único de empréstimo ativo.

## Executar com Docker

Linux/macOS:

```bash
cp .env.example .env
docker compose up --build
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Acesse:

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Credenciais iniciais

Por padrão, a aplicação cria um administrador:

```text
E-mail: admin@library.local
Senha: Admin@123456
```

Altere as variáveis `ADMIN_EMAIL`, `ADMIN_PASSWORD` e `APP_JWT_SECRET` no `.env` antes de usar fora do ambiente local.

## Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@library.local","password":"Admin@123456"}'
```

Use o `accessToken` retornado:

```bash
-H "Authorization: Bearer SEU_TOKEN"
```

## Fluxo de teste sugerido

1. Login como administrador.
2. Cadastre um membro em `POST /api/members`.
3. Cadastre um livro em `POST /api/books`.
4. Cadastre um exemplar em `POST /api/books/{bookId}/copies`.
5. Registre um empréstimo em `POST /api/loans`.
6. Renove ou devolva o empréstimo.
7. Consulte multas e notificações.

## Principais regras de negócio

- Membros bloqueados ou inativos não podem emprestar ou reservar.
- Um membro não pode ultrapassar o limite de empréstimos.
- Empréstimos atrasados impedem novos empréstimos e renovações.
- Multas acima do limite configurado impedem novos empréstimos.
- Um exemplar não pode ter dois empréstimos ativos simultaneamente.
- Livros com fila de reservas não podem ser emprestados fora da ordem.
- Uma reserva pronta expira após o período configurado.
- A devolução atrasada gera uma multa por dia.

## Variáveis principais

| Variável | Padrão | Descrição |
|---|---:|---|
| `DEFAULT_LOAN_DAYS` | 14 | Prazo padrão |
| `MAX_RENEWALS` | 2 | Limite de renovações |
| `RESERVATION_HOLD_HOURS` | 48 | Prazo para retirada |
| `DAILY_FINE` | 2.00 | Multa diária |
| `MAX_UNPAID_FINE` | 20.00 | Limite de multa pendente |
| `DUE_SOON_DAYS` | 2 | Antecedência da notificação |

## Executar sem Docker

Pré-requisitos: Java 21, Maven 3.6+ e PostgreSQL.

```bash
mvn clean test
mvn spring-boot:run
```

## Estrutura

```text
src/main/java/com/carlos/library
├── config
├── controller
├── domain
│   ├── entity
│   └── enums
├── dto
├── exception
├── repository
├── scheduler
└── service
```

## Endpoints principais

- `/api/auth` — login, cadastro e refresh token
- `/api/me` — perfil autenticado
- `/api/staff` — funcionários, somente ADMIN
- `/api/books`
- `/api/members`
- `/api/loans`
- `/api/reservations`
- `/api/fines`
- `/api/notifications`
- `/api/admin/tasks` — execução manual das rotinas agendadas

## Observações de produção

Para produção, use um gerenciador de segredos, HTTPS, rotação de chaves JWT, política de CORS explícita, observabilidade e estratégia de revogação de refresh tokens.

## Postman e documentação

- Importe `postman/Library-Management-API.postman_collection.json`.
- Consulte o diagrama em `docs/ERD.md`.
- A collection salva automaticamente IDs e o token durante o fluxo principal.

## Testes e CI

```bash
mvn clean verify
```

O workflow `.github/workflows/ci.yml` executa os testes em pushes e pull requests.

## Solução de problemas

### Build parado no Maven

O Dockerfile usa logs Maven visíveis e cache local de dependências. Para reconstruir mostrando toda a saída:

```powershell
docker compose down
docker compose --progress plain build --no-cache api
docker compose up
```

Se houver erro de rede ao acessar o Maven Central, reinicie o Docker Desktop e execute o build novamente. Em redes corporativas, proxy ou VPN podem precisar ser configurados no Docker Desktop.

## Biblioteca digital — PDF, capa e progresso de leitura

A API também oferece uma biblioteca digital com arquivos armazenados em um bucket privado do Cloudflare R2.

### Recursos

- Upload direto do navegador para o R2 por URL pré-assinada.
- O segredo do R2 permanece somente no back-end.
- PDF com limite padrão de 50 MB.
- Capas JPEG, PNG ou WebP com limite padrão de 5 MB.
- Validação de tamanho, `Content-Type` e assinatura interna do arquivo.
- Níveis de acesso `PUBLIC`, `MEMBERS_ONLY`, `STAFF_ONLY` e `ADMIN_ONLY`.
- Download opcional; quando desativado, o front-end exibe somente o leitor interno.
- URLs de leitura temporárias.
- Progresso de leitura por usuário e livro.
- Limpeza automática de sessões de upload expiradas.

### Endpoints digitais

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/digital-library/books` | Autenticado | Lista os PDFs acessíveis ao perfil atual. |
| `POST` | `/api/books/{bookId}/assets/upload-url` | `ADMIN` | Cria uma URL temporária para upload. |
| `POST` | `/api/books/{bookId}/assets/uploads/{sessionId}/confirm` | `ADMIN` | Valida e confirma o arquivo enviado. |
| `GET` | `/api/books/{bookId}/assets` | Autenticado | Lista arquivos acessíveis do livro. |
| `GET` | `/api/books/{bookId}/assets/{type}/access` | Autenticado | Gera URL temporária de leitura ou download. |
| `DELETE` | `/api/books/{bookId}/assets/{type}` | `ADMIN` | Remove o PDF ou a capa. |
| `GET` | `/api/books/{bookId}/reading-progress` | Autenticado | Consulta o progresso de leitura. |
| `PUT` | `/api/books/{bookId}/reading-progress` | Autenticado | Atualiza a página atual. |

### Variáveis do Cloudflare R2

```env
R2_ENABLED=true
R2_ENDPOINT=https://SEU_ACCOUNT_ID.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=SEU_ACCESS_KEY_ID
R2_SECRET_ACCESS_KEY=SEU_SECRET_ACCESS_KEY
R2_BUCKET=library-digital
R2_UPLOAD_EXPIRATION_MINUTES=15
R2_ACCESS_EXPIRATION_MINUTES=60
R2_MAX_PDF_BYTES=52428800
R2_MAX_COVER_BYTES=5242880
```

O bucket deve permanecer privado. Configure o CORS do próprio bucket usando `docs/cloudflare-r2-cors.json`. Esse CORS é diferente do `CORS_ALLOWED_ORIGINS` configurado no Spring Boot.

Consulte o guia completo em [`docs/DIGITAL_LIBRARY.md`](docs/DIGITAL_LIBRARY.md).
