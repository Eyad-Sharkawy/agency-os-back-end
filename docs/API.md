# Agency OS — Complete REST API Specification

All endpoints are prefixed with `/api/v1`.

- **Base URL (Local)**: `http://localhost:8080/api/v1`
- **Authentication**: `Authorization: Bearer <Keycloak_JWT>` on every request.
- **Tenant Scoping**: `X-Tenant-ID: <tenantId>` on all tenant-level endpoints.

---

## Table of Contents

0. [Users (`/users`)](#0-users)
1. [Workspaces (`/workspaces`)](#1-workspaces)
2. [Workspace Invitations (`/workspaces/invitations`)](#2-workspace-invitations)
3. [Clients (`/clients`)](#3-clients)
4. [Projects (`/projects`)](#4-projects)
5. [Tasks (`/tasks`)](#5-tasks)
6. [Time Tracking (`/time-entries`)](#6-time-tracking)
7. [Invoices (`/invoices`)](#7-invoices)
8. [Real-Time WebSocket STOMP Broker](#8-real-time-websocket-stomp-broker)

---

## 0. Users

Synchronizes and retrieves Keycloak authenticated user profile data.

### `GET /api/v1/users/me`
Retrieves the synchronized user profile for the authenticated Keycloak user account.

**Permissions**: Authenticated User (`Bearer <JWT>`)

**Response (`200 OK` - `UserProfileResponse`)**:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "keycloakId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "username": "john_doe",
  "email": "john.doe@agency.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

---

## 1. Workspaces

Manages workspace organizations, member directories, role assignments, and ownership transitions.

### `POST /api/v1/workspaces`
Creates a new isolated workspace organization and assigns the caller as `OWNER`. Triggers asynchronous schema creation in PostgreSQL.

**Permissions**: Authenticated User

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

**Permissions**: Authenticated User

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

**Request Body (`WorkspaceRequest`)**:
```json
{
  "name": "Acme Digital Group",
  "contactEmail": "contact@acmedigital.com"
}
```

**Response (`200 OK` - `WorkspaceResponse`)**:
```json
{
  "id": "c1f7b0f6-59b4-4b51-9bf6-2f0808cf7e85",
  "name": "Acme Digital Group",
  "tenantId": "tenant_acme_agency_9e3d21",
  "contactEmail": "contact@acmedigital.com",
  "isActive": true,
  "createdAt": "2026-08-14T07:00:00Z",
  "updatedAt": "2026-08-14T07:15:00Z"
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
**Permissions**: `OWNER`, `ADMIN` (Admins cannot modify Owners or other Admins).

**Request Body (`WorkspaceMemberUpdateRequest`)**:
```json
{
  "role": "ADMIN"
}
```

**Response**: `200 OK`

---

### `DELETE /api/v1/workspaces/{tenantId}/members/{userId}`
Removes a member from the workspace.  
**Permissions**: `OWNER`, `ADMIN`

**Response**: `204 No Content`

---

### `POST /api/v1/workspaces/{tenantId}/transfer-ownership`
Transfers the `OWNER` role to another workspace member and demotes the current caller to `ADMIN`.  
**Permissions**: `OWNER`

**Request Body (`WorkspaceOwnershipTransferRequest`)**:
```json
{
  "newOwnerId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99"
}
```

**Response**: `200 OK`

---

## 2. Workspace Invitations

Handles onboarding new users to workspaces by username or email.

### `POST /api/v1/workspaces/{tenantId}/invitations`
Sends an invitation to a user.  
**Permissions**: `OWNER`, `ADMIN`

**Request Body (`WorkspaceInvitationRequest`)**:
```json
{
  "username": "alexjones@example.com",
  "role": "MEMBER",
  "clientId": null
}
```

**Response (`201 Created` - `WorkspaceInvitationResponse`)**:
```json
{
  "id": "e3b0c442-98fc-1c14-9afbf4c8996fb924",
  "workspaceId": "c1f7b0f6-59b4-4b51-9bf6-2f0808cf7e85",
  "workspaceName": "Acme Agency",
  "invitedByUsername": "johndoe",
  "role": "MEMBER",
  "status": "PENDING"
}
```

---

### `GET /api/v1/workspaces/invitations`
Retrieves all pending invitations for the logged-in user.  
**Permissions**: Authenticated User

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
**Permissions**: Invitee

**Response**: `200 OK`

---

### `POST /api/v1/workspaces/invitations/{id}/decline`
Declines a pending invitation.  
**Permissions**: Invitee

**Response**: `200 OK`

---

## 3. Clients

Manages external client company accounts within the active workspace.

### `POST /api/v1/clients`
Creates a new client company.  
**Permissions**: `OWNER`, `ADMIN`  
**Headers**: `X-Tenant-ID: <tenantId>`

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
Lists all client companies in the workspace. `CLIENT` users only see their own company.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`, `CLIENT`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `GET /api/v1/clients/{id}`
Retrieves client details by ID.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`, `CLIENT`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `PUT /api/v1/clients/{id}`
Updates client company information.  
**Permissions**: `OWNER`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `DELETE /api/v1/clients/{id}`
Soft-deletes the client and cascades soft-delete to its associated projects.  
**Permissions**: `OWNER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Response**: `204 No Content`

---

## 4. Projects

Manages project deliverables, budgets, and billing rates.

### `POST /api/v1/projects`
Creates a new project. Only `OWNER` can assign a `clientId`.  
**Permissions**: `OWNER`, `ADMIN`  
**Headers**: `X-Tenant-ID: <tenantId>`

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

**Response (`201 Created` - `ProjectResponse`)**:
```json
{
  "id": "7a9e3e7f-4567-4890-a123-abcdef123456",
  "name": "E-Commerce Redesign",
  "description": "Full redesign and replatforming to Next.js and Headless Shopify",
  "budget": 25000.00,
  "billingRate": 150.00,
  "status": "IN_PROGRESS",
  "clientId": "8f3b2d1e-4c5a-6b7d-8e9f-0a1b2c3d4e5f",
  "createdAt": "2026-08-14T07:00:00Z",
  "updatedAt": "2026-08-14T07:00:00Z"
}
```

---

### `GET /api/v1/projects`
Lists projects with role-based scoping:
- `OWNER` / `ADMIN`: Sees all projects.
- `MEMBER`: Sees only projects with tasks assigned to them.
- `CLIENT`: Sees only projects contracted by their client company.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`, `CLIENT`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `GET /api/v1/projects/{id}`
Retrieves a project by ID with access verification.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`, `CLIENT`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `GET /api/v1/projects/client/{clientId}`
Lists all projects associated with a given client.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `PUT /api/v1/projects/{id}`
Updates project details. Changing `clientId` requires `OWNER` role.  
**Permissions**: `OWNER`, `ADMIN`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `DELETE /api/v1/projects/{id}`
Soft-deletes a project.  
**Permissions**: `OWNER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Response**: `204 No Content`

---

## 5. Tasks

Task backlog management, assignments, and workflow statuses.

### `POST /api/v1/tasks`
Creates a task under a project.  
**Permissions**: `OWNER`, `ADMIN`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Request Body (`TaskRequest`)**:
```json
{
  "title": "Design Checkout Flow",
  "description": "Create high-fidelity wireframes in Figma",
  "startDate": "2026-08-15T09:00:00Z",
  "dueDate": "2026-08-25T18:00:00Z",
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
  "startDate": "2026-08-15T09:00:00Z",
  "dueDate": "2026-08-25T18:00:00Z",
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
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `GET /api/v1/tasks/{id}`
Retrieves a specific task by ID. Verifies that `MEMBER` users are assigned to this task.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`, `CLIENT`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `GET /api/v1/tasks/project/{projectId}`
Lists all tasks for a specific project. `MEMBER` users only receive tasks they are assigned to.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`, `CLIENT`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `GET /api/v1/tasks/assignee/{assigneeId}`
Lists all tasks assigned to a specific Keycloak user ID. `MEMBER` users can only query their own ID.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `PATCH /api/v1/tasks/{id}/status`
Quick status update for task boards (Kanban drag-and-drop).  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER` (`MEMBER` users can only update status for tasks assigned to them).  
**Headers**: `X-Tenant-ID: <tenantId>`

**Request Body (`TaskStatusUpdateRequest`)**:
```json
{
  "status": "IN_PROGRESS"
}
```

**Response (`200 OK` - `TaskResponse`)**:
```json
{
  "id": "3c2a1b0f-9876-5432-10fe-dcba09876543",
  "title": "Design Checkout Flow",
  "description": "Create high-fidelity wireframes in Figma",
  "status": "IN_PROGRESS",
  "projectId": "7a9e3e7f-4567-4890-a123-abcdef123456",
  "assigneeIds": ["5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99"],
  "totalLoggedMinutes": 0,
  "isOverBudget": false,
  "createdAt": "2026-08-14T07:00:00Z",
  "updatedAt": "2026-08-14T07:20:00Z"
}
```

---

### `PUT /api/v1/tasks/{id}`
Full task update (title, description, dates, priority, status, project, assignees).  
**Permissions**: `OWNER`, `ADMIN`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `DELETE /api/v1/tasks/{id}`
Deletes a task.  
**Permissions**: `OWNER`, `ADMIN`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Response**: `204 No Content`

---

## 6. Time Tracking

Stopwatch and manual time logging. `CLIENT` role users are completely locked out. Users must be assigned to the target task in order to log time or start a stopwatch timer.

### `POST /api/v1/time-entries`
Manually records a time entry for an assigned task. Broadcasts to `/topic/{tenantId}/time-entries`.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Request Body (`TimeEntryRequest`)**:
```json
{
  "taskId": "3c2a1b0f-9876-5432-10fe-dcba09876543",
  "durationMinutes": 120,
  "isBillable": true,
  "userId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99"
}
```

**Response (`201 Created` - `TimeEntryResponse`)**:
```json
{
  "id": "bb2a1b0f-1111-2222-3333-dcba09876543",
  "taskId": "3c2a1b0f-9876-5432-10fe-dcba09876543",
  "userId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99",
  "durationMinutes": 120,
  "isBillable": true,
  "invoiceId": null,
  "createdAt": "2026-08-14T07:00:00Z"
}
```

---

### `GET /api/v1/time-entries`
Retrieves all logged time entries in the current workspace, optionally filtered by `taskId` or `userId`.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Query Parameters**:
- `taskId` (UUID, optional): Filter by specific task ID.
- `userId` (String, optional): Filter by specific Keycloak user ID.

---

### `POST /api/v1/time-entries/start/{taskId}`
Starts a live stopwatch timer for the current user. Ensures only 1 timer per user is active at any time.  
Broadcasts to `/topic/{tenantId}/timers/start`.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Response (`201 Created` - `ActiveTimerResponse`)**:
```json
{
  "userId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99",
  "taskId": "3c2a1b0f-9876-5432-10fe-dcba09876543",
  "startTime": "2026-08-14T07:15:00Z",
  "accumulatedSeconds": 0,
  "isPaused": false,
  "lastPausedAt": null
}
```

---

### `POST /api/v1/time-entries/pause`
Pauses the currently running stopwatch timer and saves accumulated seconds.  
Broadcasts to `/topic/{tenantId}/timers/pause`.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Response (`200 OK` - `ActiveTimerResponse`)**:
```json
{
  "userId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99",
  "taskId": "3c2a1b0f-9876-5432-10fe-dcba09876543",
  "startTime": "2026-08-14T07:15:00Z",
  "accumulatedSeconds": 1800,
  "isPaused": true,
  "lastPausedAt": "2026-08-14T07:45:00Z"
}
```

---

### `POST /api/v1/time-entries/resume`
Resumes a paused stopwatch timer.  
Broadcasts to `/topic/{tenantId}/timers/resume`.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Response (`200 OK` - `ActiveTimerResponse`)**:
```json
{
  "userId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99",
  "taskId": "3c2a1b0f-9876-5432-10fe-dcba09876543",
  "startTime": "2026-08-14T07:15:00Z",
  "accumulatedSeconds": 1800,
  "isPaused": false,
  "lastPausedAt": null
}
```

---

### `POST /api/v1/time-entries/stop`
Stops the caller's active stopwatch, computes duration, and saves a new `TimeEntry`.  
Broadcasts to `/topic/{tenantId}/timers/stop`.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Query Parameters**:
- `isBillable` (boolean, default: `true`)
- `durationMinutes` (integer, optional override)

**Response (`200 OK` - `TimeEntryResponse`)**:
```json
{
  "id": "bb2a1b0f-1111-2222-3333-dcba09876543",
  "taskId": "3c2a1b0f-9876-5432-10fe-dcba09876543",
  "userId": "5a2deb4d-1b2c-3def-8bad-1c0d7b3dcb99",
  "durationMinutes": 45,
  "isBillable": true,
  "invoiceId": null,
  "createdAt": "2026-08-14T08:00:00Z"
}
```

---

### `GET /api/v1/time-entries/active`
Returns the active running timer for the authenticated user, or `204 No Content` if none.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `GET /api/v1/time-entries/task/{taskId}`
Returns all time entries logged against a task.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `GET /api/v1/time-entries/user/{userId}`
Returns all time entries logged by a specific user.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `DELETE /api/v1/time-entries/{id}`
Deletes a recorded time entry.  
**Permissions**: `OWNER`, `ADMIN`, `MEMBER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Response**: `204 No Content`

---

## 7. Invoices

Billing aggregation and PDF document generation.

### `POST /api/v1/invoices`
Consolidates unbilled billable time entries for a client and generates an invoice.  
**Permissions**: `OWNER`  
**Headers**: `X-Tenant-ID: <tenantId>`

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
**Permissions**: `OWNER`, `ADMIN`, `CLIENT`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `GET /api/v1/invoices/{id}`
Retrieves invoice metadata and details.  
**Permissions**: `OWNER`, `ADMIN`, `CLIENT` (scoped)  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `GET /api/v1/invoices/client/{clientId}`
Lists all invoices for a specific client company.  
**Permissions**: `OWNER`, `ADMIN`  
**Headers**: `X-Tenant-ID: <tenantId>`

---

### `PUT /api/v1/invoices/{id}`
Updates invoice status (`DRAFT`, `SENT`, `PAID`, `OVERDUE`).  
**Permissions**: `OWNER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Request Body (`InvoiceRequest`)**:
```json
{
  "clientId": "8f3b2d1e-4c5a-6b7d-8e9f-0a1b2c3d4e5f",
  "status": "SENT"
}
```

---

### `DELETE /api/v1/invoices/{id}`
Deletes an invoice and releases linked time entries back to unbilled state.  
**Permissions**: `OWNER`  
**Headers**: `X-Tenant-ID: <tenantId>`

**Response**: `204 No Content`

---

### `GET /api/v1/invoices/{id}/pdf`
Generates and streams a vector PDF invoice rendered via Apache PDFBox.  
**Permissions**: `OWNER`, `ADMIN`, `CLIENT` (scoped)  
**Headers**: `X-Tenant-ID: <tenantId>`

**Response**: `200 OK` (`Content-Type: application/pdf`, `Content-Disposition: inline; filename="invoice-{id}.pdf"`)

---

## 8. Real-Time WebSocket STOMP Broker

Agency OS exposes a STOMP over WebSocket endpoint at `/ws-timer` with SockJS fallback.

### Connection & Authentication
- **Endpoint**: `http://localhost:8080/ws-timer`
- **Native Header**: `Authorization: Bearer <Keycloak_JWT>` on `CONNECT` frame.

### Topic Subscription Catalog

| Topic Destination | Authorization Scoping | Emitted Event Payload | Description |
|---|---|---|---|
| `/topic/{tenantId}/time-entries` | Member of `{tenantId}` | `TimeEntryResponse` | Broadcast when a manual time entry is saved |
| `/topic/{tenantId}/timers/start` | Member of `{tenantId}` | `ActiveTimerResponse` | Broadcast when a user starts a live stopwatch |
| `/topic/{tenantId}/timers/pause` | Member of `{tenantId}` | `ActiveTimerResponse` | Broadcast when a user pauses their stopwatch |
| `/topic/{tenantId}/timers/resume` | Member of `{tenantId}` | `ActiveTimerResponse` | Broadcast when a user resumes their stopwatch |
| `/topic/{tenantId}/timers/stop` | Member of `{tenantId}` | `TimeEntryResponse` | Broadcast when a user stops their stopwatch |
