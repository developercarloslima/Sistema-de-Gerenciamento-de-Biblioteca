CREATE TABLE book_assets (
    id UUID PRIMARY KEY,
    book_id UUID NOT NULL REFERENCES books(id),
    asset_type VARCHAR(20) NOT NULL,
    object_key VARCHAR(500) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    etag VARCHAR(200),
    access_level VARCHAR(30) NOT NULL,
    download_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_by UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_book_asset_type UNIQUE (book_id, asset_type)
);

CREATE TABLE book_upload_sessions (
    id UUID PRIMARY KEY,
    book_id UUID NOT NULL REFERENCES books(id),
    asset_type VARCHAR(20) NOT NULL,
    object_key VARCHAR(500) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    expected_size BIGINT NOT NULL CHECK (expected_size > 0),
    access_level VARCHAR(30) NOT NULL,
    download_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    requested_by UUID NOT NULL REFERENCES app_users(id),
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE reading_progress (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    book_id UUID NOT NULL REFERENCES books(id),
    current_page INTEGER NOT NULL CHECK (current_page > 0),
    total_pages INTEGER NOT NULL CHECK (total_pages > 0),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    first_accessed_at TIMESTAMPTZ NOT NULL,
    last_accessed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_reading_progress_user_book UNIQUE (user_id, book_id)
);

CREATE INDEX idx_book_assets_book ON book_assets(book_id, asset_type);
CREATE INDEX idx_upload_sessions_pending ON book_upload_sessions(book_id, asset_type, status, expires_at);
CREATE INDEX idx_reading_progress_user_updated ON reading_progress(user_id, updated_at DESC);
