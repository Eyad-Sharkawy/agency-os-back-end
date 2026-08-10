-- src/main/resources/db/migration/global/V2__add_workspace_role.sql
ALTER TABLE user_workspaces
    ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'MEMBER'
        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'CLIENT'));
