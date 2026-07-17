-- Garante, no banco de dados, que um exemplar não tenha dois empréstimos
-- simultâneos. A migration é necessária mesmo que o índice já conste na V1,
-- pois bancos que receberam uma versão anterior da V1 não são recriados pelo Flyway.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM loans
        WHERE status IN ('ACTIVE', 'OVERDUE')
        GROUP BY book_copy_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Existem exemplares com mais de um empréstimo ativo. Corrija os dados antes de aplicar a restrição uq_active_loan_copy.';
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_loan_copy
    ON loans (book_copy_id)
    WHERE status IN ('ACTIVE', 'OVERDUE');
