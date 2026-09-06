-- =========================================================================
-- AGENCY OS — FULL SHOWCASE & DEMO SEED SCRIPT (8 USERS)
-- =========================================================================

-- 1. PUBLIC SCHEMA: Users & Workspace Setup
SET search_path TO public;

GRANT ALL ON SCHEMA public TO PUBLIC;
GRANT ALL ON ALL TABLES IN SCHEMA public TO PUBLIC;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO PUBLIC;

-- Clean existing demo data if re-running
DELETE
FROM public.workspaces
WHERE tenant_id = 'tenant_apex_digital_demo';
DELETE
FROM public.app_users
WHERE username IN (
                   'alex_owner', 'marcus_admin', 'rachel_admin',
                   'sarah_member', 'liam_member', 'james_member',
                   'david_client', 'elena_client'
    );

-- Insert 8 App Users with mapped Keycloak sub UUIDs
INSERT INTO public.app_users (id, keycloak_id, username, email, first_name, last_name, created_at, updated_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'fc8c99b7-3f29-43f3-917a-3f24f0da41c4', 'alex_owner',
        'alex.owner@agency-os.dev', 'Alex', 'Vance', NOW(), NOW()),
       ('22222222-2222-2222-2222-222222222222', '2656c2b1-c49b-41cb-93d6-006468376408', 'marcus_admin',
        'marcus.admin@agency-os.dev', 'Marcus', 'Wright', NOW(), NOW()),
       ('33333333-3333-3333-3333-333333333333', 'd26e9d31-0e5f-4aff-a9b6-1da7b53d013d', 'rachel_admin',
        'rachel.admin@agency-os.dev', 'Rachel', 'Adams', NOW(), NOW()),
       ('44444444-4444-4444-4444-444444444444', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6', 'sarah_member',
        'sarah.member@agency-os.dev', 'Sarah', 'Chen', NOW(), NOW()),
       ('55555555-5555-5555-5555-555555555555', '4c223473-fc1e-4686-813b-dd50a87fa654', 'liam_member',
        'liam.member@agency-os.dev', 'Liam', 'Rodriguez', NOW(), NOW()),
       ('66666666-6666-6666-6666-666666666666', '1319b3a7-11a7-4410-b0e9-86e8bef74e77', 'james_member',
        'james.member@agency-os.dev', 'James', 'Wilson', NOW(), NOW()),
       ('77777777-7777-7777-7777-777777777777', 'd578ed5e-3c18-473e-af41-3d7a4e51c403', 'david_client',
        'david.client@cybernetic.co', 'David', 'Miller', NOW(), NOW()),
       ('88888888-8888-8888-8888-888888888888', 'a0989c21-8c23-494d-a5f7-c5ad7c51e4df', 'elena_client',
        'elena.client@velocitylabs.io', 'Elena', 'Rostova', NOW(), NOW());

-- Insert Showcase Workspace
INSERT INTO public.workspaces (id, name, tenant_id, contact_email, is_active, created_at, updated_at)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Apex Digital Studio (Demo)', 'tenant_apex_digital_demo',
        'contact@apexdigital.demo', true, NOW(), NOW());

-- Map Memberships with RBAC Roles
INSERT INTO public.user_workspaces (user_id, workspace_id, role)
VALUES ('11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'OWNER'),
       ('22222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ADMIN'),
       ('33333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ADMIN'),
       ('44444444-4444-4444-4444-444444444444', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'MEMBER'),
       ('55555555-5555-5555-5555-555555555555', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'MEMBER'),
       ('66666666-6666-6666-6666-666666666666', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'MEMBER'),
       ('77777777-7777-7777-7777-777777777777', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'CLIENT'),
       ('88888888-8888-8888-8888-888888888888', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'CLIENT');


-- =========================================================================
-- 2. TENANT SCHEMA: Provision & Seed Realistic Multi-Tenant Data
-- =========================================================================
CREATE SCHEMA IF NOT EXISTS tenant_apex_digital_demo;
GRANT ALL ON SCHEMA tenant_apex_digital_demo TO PUBLIC;
GRANT ALL ON ALL TABLES IN SCHEMA tenant_apex_digital_demo TO PUBLIC;
GRANT ALL ON ALL SEQUENCES IN SCHEMA tenant_apex_digital_demo TO PUBLIC;
GRANT ALL ON ALL ROUTINES IN SCHEMA tenant_apex_digital_demo TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_apex_digital_demo GRANT ALL ON TABLES TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_apex_digital_demo GRANT ALL ON SEQUENCES TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_apex_digital_demo GRANT ALL ON ROUTINES TO PUBLIC;

SET search_path TO tenant_apex_digital_demo;

-- Schema Tables
CREATE TABLE IF NOT EXISTS clients
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255),
    status     VARCHAR(20)  NOT NULL DEFAULT 'PROSPECT' CHECK (status IN ('PROSPECT', 'ACTIVE', 'INACTIVE')),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS client_users
(
    user_id    VARCHAR(255) PRIMARY KEY,
    client_id  UUID        NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS projects
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    client_id    UUID           NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    name         VARCHAR(100)   NOT NULL,
    description  TEXT,
    budget       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status       VARCHAR(20)    NOT NULL DEFAULT 'PLANNING' CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'ON_HOLD', 'DELIVERED')),
    billing_rate NUMERIC(12, 2) NOT NULL DEFAULT 100.00,
    is_active    BOOLEAN        NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS tasks
(
    id                UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    project_id        UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    start_date        TIMESTAMPTZ,
    due_date          TIMESTAMPTZ,
    estimated_minutes INT,
    priority          VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status            VARCHAR(20)  NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'REVIEW', 'DONE')),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS task_assignees
(
    task_id UUID REFERENCES tasks (id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (task_id, user_id)
);

CREATE TABLE IF NOT EXISTS invoices
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    client_id    UUID           NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status       VARCHAR(20)    NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'OVERDUE')),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS time_entries
(
    id               UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    task_id          UUID         NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    user_id          VARCHAR(255) NOT NULL,
    duration_minutes INT          NOT NULL CHECK (duration_minutes > 0),
    is_billable      BOOLEAN      NOT NULL DEFAULT true,
    invoice_id       UUID         REFERENCES invoices (id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS active_timers
(
    user_id               VARCHAR(255) PRIMARY KEY,
    task_id               UUID        NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    start_time            TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_paused             BOOLEAN     NOT NULL DEFAULT false,
    accumulated_seconds   INT         NOT NULL DEFAULT 0,
    last_resume_timestamp TIMESTAMPTZ
);

-- Clean tenant data if re-running
DELETE
FROM active_timers;
DELETE
FROM time_entries;
DELETE
FROM task_assignees;
DELETE
FROM tasks;
DELETE
FROM projects;
DELETE
FROM client_users;
DELETE
FROM invoices;
DELETE
FROM clients;

-- =========================================================================
-- 3. INSERT DEMO RECORDS
-- =========================================================================

-- Clients
INSERT INTO clients (id, name, email, status)
VALUES ('ca000000-0000-0000-0000-000000000001', 'Cybernetic Co.', 'billing@cybernetic.co', 'ACTIVE'),
       ('ca000000-0000-0000-0000-000000000002', 'Velocity Labs', 'accounts@velocitylabs.io', 'ACTIVE'),
       ('ca000000-0000-0000-0000-000000000003', 'Apex FinTech Solutions', 'finance@apexfintech.com', 'PROSPECT');

-- Map Client Portal Users
INSERT INTO client_users (user_id, client_id)
VALUES ('d578ed5e-3c18-473e-af41-3d7a4e51c403', 'ca000000-0000-0000-0000-000000000001'), -- David -> Cybernetic Co.
       ('a0989c21-8c23-494d-a5f7-c5ad7c51e4df', 'ca000000-0000-0000-0000-000000000002');
-- Elena -> Velocity Labs

-- Projects
INSERT INTO projects (id, client_id, name, description, budget, status, billing_rate)
VALUES ('ba000000-0000-0000-0000-000000000001', 'ca000000-0000-0000-0000-000000000001', 'AI Cloud Migration',
        'Complete infrastructure migration to distributed cloud with automated telemetry.', 45000.00, 'IN_PROGRESS',
        175.00),
       ('ba000000-0000-0000-0000-000000000002', 'ca000000-0000-0000-0000-000000000001', 'Mobile App Redesign',
        'Cross-platform mobile overhaul with biometric security and offline sync.', 28000.00, 'PLANNING', 150.00),
       ('ba000000-0000-0000-0000-000000000003', 'ca000000-0000-0000-0000-000000000002', 'Design System & Brand Portal',
        'Multi-brand Figma token library and accessible Angular component kit.', 18500.00, 'IN_PROGRESS', 160.00),
       ('ba000000-0000-0000-0000-000000000004', 'ca000000-0000-0000-0000-000000000002', 'Telemetry & Metrics Pipeline',
        'Prometheus and Grafana real-time metrics dashboards.', 12000.00, 'DELIVERED', 160.00);

-- Tasks (Featuring Overdue, Due Soon, and Over Budget showcases)
INSERT INTO tasks (id, project_id, title, description, start_date, due_date, estimated_minutes, priority, status)
VALUES ('da000000-0000-0000-0000-000000000001', 'ba000000-0000-0000-0000-000000000001',
        'Multi-Region Kubernetes Deployment', 'Set up Terraform IaC and ArgoCD sync.', NOW() - INTERVAL '3 days',
        NOW() + INTERVAL '4 days', 720, 'URGENT', 'IN_PROGRESS'),
       ('da000000-0000-0000-0000-000000000002', 'ba000000-0000-0000-0000-000000000001',
        'Zero-Trust JWT & OIDC Integration', 'Connect Spring Security resource server with Keycloak JWKS.',
        NOW() - INTERVAL '7 days', NOW() - INTERVAL '1 day', 480, 'HIGH', 'REVIEW'), -- Overdue & Over Budget
       ('da000000-0000-0000-0000-000000000003', 'ba000000-0000-0000-0000-000000000001',
        'PostgreSQL Schema Migration Pipeline', 'Automate tenant schema DDL migrations using Flyway.',
        NOW() - INTERVAL '10 days', NOW() - INTERVAL '6 days', 360, 'MEDIUM', 'DONE'),
       ('da000000-0000-0000-0000-000000000004', 'ba000000-0000-0000-0000-000000000003',
        'Design Token Architecture in Figma', 'Standardize spacing, typography, and dark-theme color tokens.',
        NOW() - INTERVAL '2 days', NOW() + INTERVAL '3 days', 300, 'HIGH', 'IN_PROGRESS'),
       ('da000000-0000-0000-0000-000000000005', 'ba000000-0000-0000-0000-000000000003',
        'Tailwind v4 Semantic Theme Mapping', 'Implement `@theme` utilities in Angular styles.',
        NOW() - INTERVAL '1 day', NOW() + INTERVAL '1 day', 240, 'MEDIUM', 'TODO'),  -- Due Soon
       ('da000000-0000-0000-0000-000000000006', 'ba000000-0000-0000-0000-000000000002',
        'Figma Prototype Review with Stakeholders', 'Validate user flow for biometric authentication.',
        NOW() + INTERVAL '1 day', NOW() + INTERVAL '5 days', 240, 'LOW', 'TODO'),
       ('da000000-0000-0000-0000-000000000007', 'ba000000-0000-0000-0000-000000000004',
        'Prometheus Exporters & AlertManager', 'Configure alert rules for p99 latency spikes and error budgets.',
        NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day', 360, 'HIGH', 'DONE');

-- Task Assignees
INSERT INTO task_assignees (task_id, user_id)
VALUES ('da000000-0000-0000-0000-000000000001', '1319b3a7-11a7-4410-b0e9-86e8bef74e77'), -- James
       ('da000000-0000-0000-0000-000000000001', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6'), -- Sarah
       ('da000000-0000-0000-0000-000000000002', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6'), -- Sarah
       ('da000000-0000-0000-0000-000000000002', '2656c2b1-c49b-41cb-93d6-006468376408'), -- Marcus
       ('da000000-0000-0000-0000-000000000003', 'fc8c99b7-3f29-43f3-917a-3f24f0da41c4'), -- Alex
       ('da000000-0000-0000-0000-000000000004', '4c223473-fc1e-4686-813b-dd50a87fa654'), -- Liam
       ('da000000-0000-0000-0000-000000000004', 'd26e9d31-0e5f-4aff-a9b6-1da7b53d013d'), -- Rachel
       ('da000000-0000-0000-0000-000000000005', '4c223473-fc1e-4686-813b-dd50a87fa654'), -- Liam
       ('da000000-0000-0000-0000-000000000005', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6'), -- Sarah
       ('da000000-0000-0000-0000-000000000006', '2656c2b1-c49b-41cb-93d6-006468376408'), -- Marcus
       ('da000000-0000-0000-0000-000000000007', '1319b3a7-11a7-4410-b0e9-86e8bef74e77');
-- James

-- Invoices
INSERT INTO invoices (id, client_id, total_amount, status, created_at)
VALUES ('ea000000-0000-0000-0000-000000000001', 'ca000000-0000-0000-0000-000000000001', 4200.00, 'PAID',
        NOW() - INTERVAL '14 days'),
       ('ea000000-0000-0000-0000-000000000002', 'ca000000-0000-0000-0000-000000000001', 3150.00, 'SENT',
        NOW() - INTERVAL '2 days'),
       ('ea000000-0000-0000-0000-000000000003', 'ca000000-0000-0000-0000-000000000002', 4800.00, 'PAID',
        NOW() - INTERVAL '7 days'),
       ('ea000000-0000-0000-0000-000000000004', 'ca000000-0000-0000-0000-000000000002', 2560.00, 'DRAFT',
        NOW() - INTERVAL '1 day');

-- Time Entries (Includes Alex, Marcus, Rachel, Sarah, Liam, and James)
INSERT INTO time_entries (id, task_id, user_id, duration_minutes, is_billable, invoice_id, created_at)
VALUES
    -- Alex Vance (Owner)
    ('fa000000-0000-0000-0000-000000000001', 'da000000-0000-0000-0000-000000000003',
     'fc8c99b7-3f29-43f3-917a-3f24f0da41c4', 360, true, 'ea000000-0000-0000-0000-000000000001',
     NOW() - INTERVAL '8 days'),

    -- Marcus Wright (Admin)
    ('fa000000-0000-0000-0000-000000000006', 'da000000-0000-0000-0000-000000000002',
     '2656c2b1-c49b-41cb-93d6-006468376408', 180, true, NULL, NOW() - INTERVAL '2 days'),

    -- Rachel Adams (Admin)
    ('fa000000-0000-0000-0000-000000000009', 'da000000-0000-0000-0000-000000000004',
     'd26e9d31-0e5f-4aff-a9b6-1da7b53d013d', 150, true, 'ea000000-0000-0000-0000-000000000003',
     NOW() - INTERVAL '3 days'),

    -- Sarah Chen (Member)
    ('fa000000-0000-0000-0000-000000000002', 'da000000-0000-0000-0000-000000000002',
     '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6', 480, true, 'ea000000-0000-0000-0000-000000000002',
     NOW() - INTERVAL '3 days'),
    ('fa000000-0000-0000-0000-000000000003', 'da000000-0000-0000-0000-000000000001',
     '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6', 210, true, NULL, NOW() - INTERVAL '1 day'),

    -- Liam Rodriguez (Member)
    ('fa000000-0000-0000-0000-000000000004', 'da000000-0000-0000-0000-000000000004',
     '4c223473-fc1e-4686-813b-dd50a87fa654', 180, true, 'ea000000-0000-0000-0000-000000000003',
     NOW() - INTERVAL '5 days'),
    ('fa000000-0000-0000-0000-000000000005', 'da000000-0000-0000-0000-000000000004',
     '4c223473-fc1e-4686-813b-dd50a87fa654', 120, true, NULL, NOW() - INTERVAL '1 day'),

    -- James Wilson (Member)
    ('fa000000-0000-0000-0000-000000000007', 'da000000-0000-0000-0000-000000000007',
     '1319b3a7-11a7-4410-b0e9-86e8bef74e77', 360, true, 'ea000000-0000-0000-0000-000000000003',
     NOW() - INTERVAL '4 days'),
    ('fa000000-0000-0000-0000-000000000008', 'da000000-0000-0000-0000-000000000001',
     '1319b3a7-11a7-4410-b0e9-86e8bef74e77', 240, true, NULL, NOW() - INTERVAL '1 day');

-- Real-Time Active Stopwatch Timers (Sarah running, Liam paused)
INSERT INTO active_timers (user_id, task_id, start_time, is_paused, accumulated_seconds)
VALUES ('8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6', 'da000000-0000-0000-0000-000000000001',
        NOW() - INTERVAL '1 hour 24 minutes', false, 5040),
       ('4c223473-fc1e-4686-813b-dd50a87fa654', 'da000000-0000-0000-0000-000000000004', NOW() - INTERVAL '45 minutes',
        true, 2700);

-- =========================================================================
-- 4. PERMISSIONS & FLYWAY BASELINE
-- =========================================================================
GRANT ALL ON ALL TABLES IN SCHEMA tenant_apex_digital_demo TO PUBLIC;
GRANT ALL ON ALL SEQUENCES IN SCHEMA tenant_apex_digital_demo TO PUBLIC;

CREATE TABLE IF NOT EXISTS tenant_apex_digital_demo.flyway_schema_history
(
    installed_rank INT           NOT NULL PRIMARY KEY,
    version        VARCHAR(50),
    description    VARCHAR(200)  NOT NULL,
    type           VARCHAR(20)   NOT NULL,
    script         VARCHAR(1000) NOT NULL,
    checksum       INT,
    installed_by   VARCHAR(100)  NOT NULL,
    installed_on   TIMESTAMP     NOT NULL DEFAULT now(),
    execution_time INT           NOT NULL,
    success        BOOLEAN       NOT NULL
);

INSERT INTO tenant_apex_digital_demo.flyway_schema_history
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (1, '1', 'init tenant', 'SQL', 'V1__init_tenant.sql', NULL, 'seed_script', now(), 1, true),
       (2, '2', 'add client users', 'SQL', 'V2__add_client_users.sql', NULL, 'seed_script', now(), 1, true),
       (3, '3', 'add workspace invitations', 'SQL', 'V3__add_workspace_invitations.sql', NULL, 'seed_script', now(), 1,
        true),
       (4, '4', 'add project description', 'SQL', 'V4__add_project_description.sql', NULL, 'seed_script', now(), 1,
        true),
       (5, '5', 'add timer pause support', 'SQL', 'V5__add_timer_pause_support.sql', NULL, 'seed_script', now(), 1,
        true)
ON CONFLICT (installed_rank) DO NOTHING;

GRANT ALL ON ALL TABLES IN SCHEMA tenant_apex_digital_demo TO PUBLIC;

