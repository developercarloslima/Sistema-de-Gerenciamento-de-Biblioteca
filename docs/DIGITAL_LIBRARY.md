# Biblioteca digital e Cloudflare R2

A implementação usa o protocolo S3 do Cloudflare R2. O navegador recebe uma URL pré-assinada e envia o arquivo diretamente ao bucket; as chaves de acesso nunca são expostas ao cliente.

## 1. Crie o bucket

Crie um bucket privado, por exemplo:

```text
library-digital
```

Não habilite acesso público.

## 2. Crie as credenciais

Em **R2 → API Tokens**, crie um token com permissão **Object Read & Write**, limitado ao bucket. Copie o Access Key ID e o Secret Access Key quando forem exibidos.

## 3. Configure o CORS do bucket

No bucket, abra **Settings → CORS Policy** e cole o conteúdo de `cloudflare-r2-cors.json`, substituindo a URL do front-end.

O CORS do bucket é obrigatório porque o navegador envia o PDF diretamente ao domínio S3 do R2. Ele não substitui o CORS da API Spring Boot.

## 4. Configure o Render

```text
R2_ENABLED=true
R2_ENDPOINT=https://SEU_ACCOUNT_ID.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=...
R2_SECRET_ACCESS_KEY=...
R2_BUCKET=library-digital
R2_UPLOAD_EXPIRATION_MINUTES=15
R2_ACCESS_EXPIRATION_MINUTES=60
R2_MAX_PDF_BYTES=52428800
R2_MAX_COVER_BYTES=5242880
```

Para buckets com jurisdição específica, use o endpoint correspondente exibido pela Cloudflare.

## 5. Fluxo do upload

1. `POST /api/books/{bookId}/assets/upload-url`.
2. O front-end executa `PUT` diretamente na URL recebida, enviando todos os `requiredHeaders`.
3. `POST /api/books/{bookId}/assets/uploads/{sessionId}/confirm`.
4. O back-end confere tamanho, tipo e assinatura interna.
5. O registro ativo é criado ou substituído em `book_assets`.

## 6. Tipos aceitos

```text
PDF: application/pdf, até 50 MB
Capa: image/jpeg, image/png ou image/webp, até 5 MB
```

## 7. Permissões

```text
PUBLIC        todos os usuários autenticados
MEMBERS_ONLY  membros e equipe
STAFF_ONLY    bibliotecários e administradores
ADMIN_ONLY    somente administradores
```

## 8. Direitos autorais

O sistema exige a confirmação de autorização no upload. Essa confirmação não substitui a verificação jurídica do conteúdo. Use apenas obras em domínio público, próprias ou licenciadas para distribuição.
