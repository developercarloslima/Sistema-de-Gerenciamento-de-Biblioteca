# Correção do erro `lower(bytea)`

O PostgreSQL recebia filtros opcionais de texto como `null`. Em conjunto com Hibernate 6, o parâmetro nulo podia ser associado como `bytea`, causando:

```text
ERROR: function lower(bytea) does not exist
```

A correção aplicada:

- envia filtros ausentes como string vazia (`""`), não `null`;
- usa `:query = ''` e `:category = ''` para desativar filtros opcionais;
- aplica a mesma correção ao catálogo digital.

Arquivos alterados:

- `src/main/java/com/carlos/library/repository/BookRepository.java`
- `src/main/java/com/carlos/library/service/BookService.java`
- `src/main/java/com/carlos/library/service/DigitalBookService.java`
