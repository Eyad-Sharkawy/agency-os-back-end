package dev.eyadsharkawy.agency_os_api.global.workspace.entity;

import dev.eyadsharkawy.agency_os_api.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "workspace_invitations", schema = "public")
@Getter
@Setter
public class WorkspaceInvitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "invited_by_username", nullable = false)
    private String invitedByUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private WorkspaceRole role;

    @Column(name = "client_id")
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;
}
