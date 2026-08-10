package dev.eyadsharkawy.agency_os_api.global.workspace.entity;

import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_workspaces", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class UserWorkspace {
    @EmbeddedId
    private UserWorkspaceId id = new UserWorkspaceId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("workspaceId")
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private WorkspaceRole role = WorkspaceRole.MEMBER;
}
