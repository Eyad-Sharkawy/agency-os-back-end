-- V1__init_tenant.sql
CREATE TABLE clients
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255),
    status     VARCHAR(20)  NOT NULL DEFAULT 'PROSPECT'
        CHECK (status IN ('PROSPECT', 'ACTIVE', 'INACTIVE')),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE projects
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    client_id  UUID         NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    budget     NUMERIC(12, 2),
    status     VARCHAR(20)  NOT NULL DEFAULT 'PLANNING'
        CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'DELIVERED')),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE tasks
(
    id                UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    project_id        UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    start_date        TIMESTAMPTZ,
    due_date          TIMESTAMPTZ,
    estimated_minutes INT,
    priority          VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status            VARCHAR(20)  NOT NULL DEFAULT 'TODO'
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'REVIEW', 'DONE')),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE task_assignees
(
    task_id UUID REFERENCES tasks (id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL, -- Keycloak ID
    PRIMARY KEY (task_id, user_id)
);

CREATE TABLE time_entries
(
    id               UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    task_id          UUID         NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    user_id          VARCHAR(255) NOT NULL, -- Keycloak ID, no FK
    duration_minutes INT          NOT NULL CHECK (duration_minutes > 0),
    is_billable      BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE active_timers
(
    user_id    VARCHAR(255) PRIMARY KEY,
    task_id    UUID        NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE invoices
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    client_id    UUID           NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status       VARCHAR(20)    NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'OVERDUE')),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_client_id ON projects (client_id);
CREATE INDEX idx_tasks_project_id ON tasks (project_id);
CREATE INDEX idx_task_assignees_user_id ON task_assignees (user_id);
CREATE INDEX idx_time_entries_task_id ON time_entries (task_id);
CREATE INDEX idx_time_entries_user_id ON time_entries (user_id);
CREATE INDEX idx_invoices_client_id ON invoices (client_id);
