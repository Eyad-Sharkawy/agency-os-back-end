-- src/main/resources/db/migration/global/V1__init_global.sql

CREATE TABLE workspaces
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(255)             NOT NULL,
    tenant_id  VARCHAR(255)             NOT NULL UNIQUE,
    is_active  BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE app_users
(
    id          UUID PRIMARY KEY,
    keycloak_id VARCHAR(255)             NOT NULL UNIQUE,
    username    VARCHAR(255)             NOT NULL UNIQUE,
    email       VARCHAR(255)             NOT NULL UNIQUE,
    first_name  VARCHAR(255)             NOT NULL,
    last_name   VARCHAR(255)             NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE user_workspaces
(
    user_id      UUID NOT NULL,
    workspace_id UUID NOT NULL,
    PRIMARY KEY (user_id, workspace_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE
);
