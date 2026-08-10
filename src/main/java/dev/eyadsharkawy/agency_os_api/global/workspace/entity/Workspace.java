package dev.eyadsharkawy.agency_os_api.global.workspace.entity;

import dev.eyadsharkawy.agency_os_api.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "workspaces", schema = "public")
@SQLDelete(sql = "UPDATE public.workspaces SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
public class Workspace extends BaseEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tenant_id", nullable = false, unique = true, updatable = false)
    private String tenantId;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserWorkspace> userWorkspaces = new HashSet<>();
}
