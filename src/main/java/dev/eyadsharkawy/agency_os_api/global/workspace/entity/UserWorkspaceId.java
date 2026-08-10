package dev.eyadsharkawy.agency_os_api.global.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Setter @Getter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class UserWorkspaceId implements Serializable {
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "workspace_id")
    private UUID workspaceId;
}
