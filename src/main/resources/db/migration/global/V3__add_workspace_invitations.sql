-- src/main/resources/db/migration/global/V3__add_workspace_invitations.sql
CREATE TABLE workspace_invitations
(
    id                  UUID PRIMARY KEY,
    workspace_id        UUID         NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    username            VARCHAR(255) NOT NULL,
    invited_by_username VARCHAR(255) NOT NULL,
    role                VARCHAR(50)  NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'CLIENT')),
    client_id           UUID         NULL,
    status              VARCHAR(50)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_workspace_username UNIQUE (workspace_id, username)
);
