# Backblaze B2

Variáveis de produção:

```env
R2_ENABLED=true
R2_ENDPOINT=https://s3.us-east-005.backblazeb2.com
R2_REGION=us-east-005
R2_ACCESS_KEY_ID=SEU_NOVO_KEY_ID
R2_SECRET_ACCESS_KEY=SUA_NOVA_APPLICATION_KEY
R2_BUCKET=library-digital
R2_UPLOAD_EXPIRATION_MINUTES=15
R2_ACCESS_EXPIRATION_MINUTES=60
R2_MAX_PDF_BYTES=52428800
R2_MAX_COVER_BYTES=5242880
```

O nome `R2_` foi mantido por compatibilidade com o código existente, mas os valores são do Backblaze B2.
Nunca envie credenciais ao GitHub.
