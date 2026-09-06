CREATE TABLE documents (
                           id UUID PRIMARY KEY,

                           trip_id UUID NOT NULL,
                           uploaded_by UUID NOT NULL,

                           name VARCHAR(255) NOT NULL,
                           original_file_name VARCHAR(255) NOT NULL,

                           mime_type VARCHAR(100) NOT NULL,
                           file_size BIGINT NOT NULL,

                           storage_key VARCHAR(500) NOT NULL UNIQUE,

                           category VARCHAR(30) NOT NULL,
                           source VARCHAR(20) NOT NULL,
                           status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                           description TEXT,

                           document_number VARCHAR(100),
                           issued_by VARCHAR(255),
                           issued_date DATE,
                           expiry_date DATE,

                           is_favorite BOOLEAN NOT NULL DEFAULT FALSE,

                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_documents_trip
                               FOREIGN KEY (trip_id)
                                   REFERENCES trips(id),

                           CONSTRAINT fk_documents_uploaded_by
                               FOREIGN KEY (uploaded_by)
                                   REFERENCES users(id),

                           CONSTRAINT chk_documents_file_size
                               CHECK (file_size > 0),

                           CONSTRAINT chk_documents_file_size_limit
                               CHECK (file_size <= 20971520)
);

CREATE INDEX idx_documents_trip_id
    ON documents(trip_id);

CREATE INDEX idx_documents_uploaded_by
    ON documents(uploaded_by);

CREATE INDEX idx_documents_trip_status
    ON documents(trip_id, status);

CREATE INDEX idx_documents_trip_category
    ON documents(trip_id, category);

CREATE INDEX idx_documents_trip_favorite
    ON documents(trip_id, is_favorite);

CREATE INDEX idx_documents_expiry_date
    ON documents(expiry_date);

CREATE INDEX idx_documents_created_at
    ON documents(created_at);