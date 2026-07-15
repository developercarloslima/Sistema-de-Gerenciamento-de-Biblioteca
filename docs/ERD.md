# Modelo de dados

```mermaid
erDiagram
    APP_USERS ||--o| MEMBERS : possui
    APP_USERS ||--o{ NOTIFICATIONS : recebe
    BOOKS ||--o{ BOOK_COPIES : possui
    MEMBERS ||--o{ LOANS : realiza
    BOOK_COPIES ||--o{ LOANS : participa
    MEMBERS ||--o{ RESERVATIONS : solicita
    BOOKS ||--o{ RESERVATIONS : recebe
    BOOK_COPIES o|--o{ RESERVATIONS : separa
    MEMBERS ||--o{ FINES : recebe
    LOANS ||--o| FINES : gera
```

## Estados importantes

- Exemplar: `AVAILABLE`, `LOANED`, `RESERVED`, `LOST`, `DAMAGED`, `MAINTENANCE`.
- Empréstimo: `ACTIVE`, `RETURNED`, `OVERDUE`, `LOST`.
- Reserva: `WAITING`, `READY`, `FULFILLED`, `CANCELLED`, `EXPIRED`.
- Multa: `PENDING`, `PAID`, `CANCELLED`.
