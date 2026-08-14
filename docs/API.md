# Agency OS — Complete REST API Specification

All endpoints are prefixed with `/api/v1`.

- **Base URL (Local)**: `http://localhost:8080/api/v1`
- **Authentication**: `Authorization: Bearer <Keycloak_JWT>` on every request.
- **Tenant Scoping**: `X-Tenant-ID: <tenantId>` on all tenant-level endpoints.

---

## Table of Contents

1. [Workspaces (`/workspaces`)](#1-workspaces)
2. [Workspace Invitations (`/workspaces/invitations`)](#2-workspace-invitations)
3. [Clients (`/clients`)](#3-clients)
4. [Projects (`/projects`)](#4-projects)
5. [Tasks (`/tasks`)](#5-tasks)
6. [Time Tracking (`/time-entries`)](#6-time-tracking)
7. [Invoices (`/invoices`)](#7-invoices)

---

## 1. Workspaces

Manages workspace organizations, member directories, and ownership transitions.

### `POST /api/v1/workspaces`
Creates a new isolated workspace organization and assigns the caller as `OWNER`. Triggers asynchronous schema creation in PostgreSQL.

**Request Body (`WorkspaceRequest`)**:
```json
{
  "name": "Acme Agency",
  "contactEmail": "admin@acme.com"
}
```

**Response (`201 Created` - `WorkspaceResponse`)**:
```json
{
  "id": "c1f7b0f6-59b4-4b51-9bf6-2f0808cf7e85",
  "name": "Acme Agency",
  "tenantId": "tenant_acme_agency_9e3d21",
  "contactEmail": "admin@acme.com",
  "isActive": true,
  "createdAt": "2026-08-14T07:00:00Z",
  "updatedAt": "2026-08-14T07:00:00Z"
}
```

---

### `GET /api/v1/workspaces`
Lists all workspaces where the current authenticated user has active membership.

**Response (`200 OK` - `List<WorkspaceResponse>`)**:
```json
[
  {
    "id": "c1f7b0f6-59b4-4b51-9bf6-2f0808cf7e85",
    "name": "Acme Agency",
    "tenantId": "tenant_acme_agency_9e3d21",
    "contactEmail": "admin@acme.com",
    "isActive": true,
    "createdAt": "2026-08-14T07:00:00Z",
    "updatedAt": "2026-08-14T07:00:00Z"
  }
]
```

---

### `PUT /api/v1/workspaces/{tenantId}`
Updates workspace name or contact email.  
**Permissions**: `OWNER`

**Request Body**:
```json
{
  "name": "Acme Digital Group",
  "contactEmail": "contact@acmedigital.com"
}
```

---

### `DELETE /api/v1/workspaces/{tenantId}`
Soft-deletes the workspace.  
**Permissions**: `OWNER`

**Response**: `204 No Content`

---

### `GET /api/v1/workspaces/{tenantId}/members`
Lists all members in the specified workspace with their assigned roles.  
**Permissions**: `OWNER`, `ADMIN`

**Response (`200 OK` - `List<WorkspaceMemberResponse>`)**:
```json
[
  {
    "userId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "username": "johndoe",
    "email": "john@acme.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "OWNER"
  },
  {
    "userId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99",
    "username": "sarahsmith",
    "email": "sarah@acme.com",
    "firstName": "Sarah",
    "lastName": "Smith",
    "role": "MEMBER"
  }
]
```

---

### `PUT /api/v1/workspaces/{tenantId}/members/{userId}`
Updates a member's role in the workspace.  
**Permissions**: `OWNER`, `ADMIN`

**Request Body (`WorkspaceMemberUpdateRequest`)**:
```json
{
  "role": "ADMIN"
}
```

---

### `DELETE /api/v1/workspaces/{tenantId}/members/{userId}`
Removes a member from the workspace.  
**Permissions**: `OWNER`, `ADMIN`

**Response**: `204 No Content`

---

### `POST /api/v1/workspaces/{tenantId}/transfer-ownership`
Transfers the `OWNER` role to another workspace member.  
**Permissions**: `OWNER`

**Request Body (`WorkspaceOwnershipTransferRequest`)**:
```json
{
  "newOwnerUserId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99"
}
```

---

## 2. Workspace Invitations

Handles onboarding new users to workspaces.

### `POST /api/v1/workspaces/{tenantId}/invitations`
Sends an invitation to a user.  
**Permissions**: `OWNER`, `ADMIN`

**Request Body (`WorkspaceInvitationRequest`)**:
```json
{
  "username": "alexjones",
  "role": "MEMBER",
  "clientId": null
}
```

---

### `GET /api/v1/workspaces/invitations`
Retrieves all pending invitations for the logged-in user.

**Response (`200 OK` - `List<WorkspaceInvitationResponse>`)**:
```json
[
  {
    "id": "e3b0c442-98fc-1c14-9afbf4c8996fb924",
    "workspaceId": "c1f7b0f6-59b4-4b51-9bf6-2f0808cf7e85",
    "workspaceName": "Acme Agency",
    "invitedByUsername": "johndoe",
    "role": "MEMBER",
    "status": "PENDING"
  }
]
```

---

### `POST /api/v1/workspaces/invitations/{id}/accept`
Accepts a pending invitation and joins the workspace.

**Response**: `200 OK`

---

### `POST /api/v1/workspaces/invitations/{id}/decline`
Declines a pending invitation.

**Response**: `200 OK`

---

## 3. Clients

Manages external client company accounts.

### `POST /api/v1/clients`
Creates a new client company.  
**Permissions**: `OWNER`, `ADMIN`

**Request Body (`ClientRequest`)**:
```json
{
  "name": "Wayne Enterprises",
  "email": "billing@wayne.com",
  "status": "ACTIVE"
}
```

**Response (`201 Created` - `ClientResponse`)**:
```json
{
  "id": "8f3b2d1e-4c5a-6b7d-8e9f-0a1b2c3d4e5f",
  "name": "Wayne Enterprises",
  "email": "billing@wayne.com",
  "status": "ACTIVE",
  "createdAt": "2026-08-14T07:00:00Z",
  "updatedAt": "2026-08-14T07:00:00Z"
}
```

---

### `GET /api/v1/clients`
Lists all client companies in the workspace.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`

---

### `GET /api/v1/clients/{id}`
Retrieves client details by ID.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`

---

### `PUT /api/v1/clients/{id}`
Updates client company information.  
**Permissions**: `OWNER`

---

### `DELETE /api/v1/clients/{id}`
Soft-deletes the client and cascades soft-delete to its associated projects.  
**Permissions**: `OWNER`

**Response**: `204 No Content`

---

## 4. Projects

Manages project deliverables, budgets, and billing rates.

### `POST /api/v1/projects`
Creates a new project.  
**Permissions**: `OWNER`, `ADMIN` (Only `OWNER` can set `clientId`).

**Request Body (`ProjectRequest`)**:
```json
{
  "name": "E-Commerce Redesign",
  "description": "Full redesign and replatforming to Next.js and Headless Shopify",
  "budget": 25000.00,
  "billingRate": 150.00,
  "status": "IN_PROGRESS",
  "clientId": "8f3b2d1e-4c5a-6b7d-8e9f-0a1b2c3d4e5f"
}
```

---

### `GET /api/v1/projects`
Lists projects with role-based scoping:
- `OWNER` / `ADMIN`: Sees all projects.
- `MEMBER`: Sees only projects with tasks assigned to them.
- `CLIENT`: Sees only projects contracted by their client company.

---

### `GET /api/v1/projects/{id}`
Retrieves a project by ID with access verification.

---

### `GET /api/v1/projects/client/{clientId}`
Lists all projects associated with a given client.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`

---

### `PUT /api/v1/projects/{id}`
Updates project details. Changing `clientId` requires `OWNER` role.  
**Permissions**: `OWNER`, `ADMIN`

---

### `DELETE /api/v1/projects/{id}`
Soft-deletes a project.  
**Permissions**: `OWNER`

---

## 5. Tasks

Task board management, assignments, and workflow statuses.

### `POST /api/v1/tasks`
Creates a task under a project.  
**Permissions**: `OWNER`, `ADMIN`

**Request Body (`TaskRequest`)**:
```json
{
  "title": "Design Checkout Flow",
  "description": "Create high-fidelity wireframes in Figma",
  "startDate": "2026-08-15",
  "dueDate": "2026-08-25",
  "estimatedMinutes": 480,
  "priority": "HIGH",
  "status": "TODO",
  "projectId": "7a9e3e7f-4567-4890-a123-abcdef123456",
  "assigneeIds": ["5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99"]
}
```

**Response (`201 Created` - `TaskResponse`)**:
```json
{
  "id": "3c2a1b0f-9876-5432-10fe-dcba09876543",
  "title": "Design Checkout Flow",
  "description": "Create high-fidelity wireframes in Figma",
  "startDate": "2026-08-15",
  "dueDate": "2026-08-25",
  "estimatedMinutes": 480,
  "priority": "HIGH",
  "status": "TODO",
  "projectId": "7a9e3e7f-4567-4890-a123-abcdef123456",
  "assigneeIds": ["5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99"],
  "totalLoggedMinutes": 0,
  "isOverBudget": false,
  "createdAt": "2026-08-14T07:00:00Z",
  "updatedAt": "2026-08-14T07:00:00Z"
}
```

---

### `GET /api/v1/tasks`
Lists tasks (filtered to assigned tasks for `MEMBER`).  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`, `CLIENT`

---

### `GET /api/v1/tasks/{id}`
Retrieves a specific task by ID. Verifies that `MEMBER` users are assigned to this task before returning.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`, `CLIENT`

---

### `GET /api/v1/tasks/project/{projectId}`
Lists all tasks for a specific project. `MEMBER` users only receive tasks they are assigned to.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`, `CLIENT`

---

### `GET /api/v1/tasks/assignee/{assigneeId}`
Lists all tasks assigned to a specific Keycloak user ID. `MEMBER` users can only query their own ID.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`

---

### `PATCH /api/v1/tasks/{id}/status`
Quick status update for task boards (Kanban drag-and-drop).  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER` (`MEMBER` users can only update status for tasks assigned to them).

**Request Body (`TaskStatusUpdateRequest`)**:
```json
{
  "status": "IN_PROGRESS"
}
```

---

### `PUT /api/v1/tasks/{id}`
Full task update (title, description, dates, priority, status, project, assignees).  
**Permissions**: `OWNER`, `ADMIN`

---

### `DELETE /api/v1/tasks/{id}`
Deletes a task.  
**Permissions**: `OWNER`, `ADMIN`

---

## 6. Time Tracking

Stopwatch and manual time logging. `CLIENT` role users are completely locked out. Users must be assigned to the target task in order to log time or start a stopwatch timer.

### `POST /api/v1/time-entries`
Manually records a time entry for an assigned task. `OWNER` and `ADMIN` can optionally provide `userId` to log time on behalf of an assigned team member. Broadcasts to `/topic/{tenantId}/time-entries`.

**Request Body (`TimeEntryRequest`)**:
```json
{
  "taskId": "3c2a1b0f-9876-5432-10fe-dcba09876543",
  "durationMinutes": 120,
  "isBillable": true,
  "userId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99"
}
```
> Note: `userId` is optional. If omitted, defaults to the authenticated user. `MEMBER` role can only log time for themselves. In all cases, the target user must be assigned to the task.

---

### `POST /api/v1/time-entries/start/{taskId}`
Starts a live stopwatch timer for the current user. Ensures only 1 timer per user is active at any time.  
Broadcasts to `/topic/{tenantId}/timers/start`.

**Response (`200 OK` - `ActiveTimerResponse`)**:
```json
{
  "userId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99",
  "taskId": "3c2a1b0f-9876-5432-10fe-dcba09876543",
  "startTime": "2026-08-14T07:15:00Z"
}
```

---

### `POST /api/v1/time-entries/stop`
Stops the caller's active stopwatch, computes duration, and saves a new `TimeEntry`.  
Broadcasts to `/topic/{tenantId}/timers/stop`.

---

### `GET /api/v1/time-entries/active`
Returns the active running timer for the authenticated user, or `204 No Content` if none.

---

### `GET /api/v1/time-entries/task/{taskId}`
Returns all time entries logged against a task.

---

### `DELETE /api/v1/time-entries/{id}`
Deletes a recorded time entry.

---

## 7. Invoices

Billing aggregation and PDF document generation.

### `POST /api/v1/invoices`
Consolidates unbilled billable time entries for a client and generates an invoice.  
**Permissions**: `OWNER`

**Request Body (`InvoiceRequest`)**:
```json
{
  "clientId": "8f3b2d1e-4c5a-6b7d-8e9f-0a1b2c3d4e5f"
}
```

**Response (`201 Created` - `InvoiceResponse`)**:
```json
{
  "id": "11223344-5566-7788-99aa-bbccddeeff00",
  "clientId": "8f3b2d1e-4c5a-6b7d-8e9f-0a1b2c3d4e5f",
  "totalAmount": 3750.00,
  "status": "DRAFT",
  "createdAt": "2026-08-14T07:00:00Z",
  "updatedAt": "2026-08-14T07:00:00Z"
}
```

---

### `GET /api/v1/invoices`
Lists invoices:
- `OWNER`, `ADMIN`: All invoices.
- `CLIENT`: Invoices for their company only.
- `MEMBER`: `403 Forbidden`.

---

### `GET /api/v1/invoices/{id}/pdf`
Generates and streams a vector PDF invoice rendered via Apache PDFBox.  
**Permissions**: `OWNER`, `ADMIN`, `CLIENT` (scoped)

**Response**: `200 OK` (`Content-Type: application/pdf`)
