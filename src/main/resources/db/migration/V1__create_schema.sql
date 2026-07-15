CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE members (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id),
    registration_number VARCHAR(30) NOT NULL UNIQUE,
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    maximum_loans INTEGER NOT NULL DEFAULT 3 CHECK (maximum_loans > 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE books (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    author VARCHAR(160) NOT NULL,
    publisher VARCHAR(120),
    publication_year INTEGER,
    category VARCHAR(80) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE book_copies (
    id UUID PRIMARY KEY,
    book_id UUID NOT NULL REFERENCES books(id),
    inventory_code VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    acquisition_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE loans (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL REFERENCES members(id),
    book_copy_id UUID NOT NULL REFERENCES book_copies(id),
    loan_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    renewal_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_active_loan_copy ON loans(book_copy_id)
WHERE status IN ('ACTIVE', 'OVERDUE');

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL REFERENCES members(id),
    book_id UUID NOT NULL REFERENCES books(id),
    book_copy_id UUID REFERENCES book_copies(id),
    reservation_date TIMESTAMPTZ NOT NULL,
    expiration_date TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_active_reservation_member_book ON reservations(member_id, book_id)
WHERE status IN ('WAITING', 'READY');
CREATE UNIQUE INDEX uq_ready_reservation_copy ON reservations(book_copy_id)
WHERE status = 'READY' AND book_copy_id IS NOT NULL;

CREATE TABLE fines (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL REFERENCES members(id),
    loan_id UUID NOT NULL UNIQUE REFERENCES loans(id),
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(40) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    reference_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_book_copies_book_status ON book_copies(book_id, status);
CREATE INDEX idx_loans_member_status ON loans(member_id, status);
CREATE INDEX idx_loans_due_date ON loans(due_date, status);
CREATE INDEX idx_reservations_book_status ON reservations(book_id, status, created_at);
CREATE INDEX idx_fines_member_status ON fines(member_id, status);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE UNIQUE INDEX uq_notification_reference ON notifications(user_id, type, reference_id)
WHERE reference_id IS NOT NULL;
