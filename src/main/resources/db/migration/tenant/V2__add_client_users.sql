-- src/main/resources/db/migration/tenant/V2__add_client_users.sql
CREATE TABLE client_users
(
    user_id    VARCHAR(255) PRIMARY KEY,
    client_id  UUID        NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
