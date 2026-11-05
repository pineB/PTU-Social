CREATE TABLE certificate_request_documents (
    request_id uuid PRIMARY KEY REFERENCES certificate_requests (id) ON DELETE CASCADE,
    document_name varchar(255) NOT NULL,
    document_data bytea NOT NULL
);
