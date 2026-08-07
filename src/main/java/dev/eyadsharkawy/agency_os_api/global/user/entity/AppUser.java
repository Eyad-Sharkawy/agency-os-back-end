package dev.eyadsharkawy.agency_os_api.global.user.entity;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "app_users", schema = "public")
public class AppUser extends BaseEntity {

    @Column(name = "keycloak_id", nullable = false, unique = true, updatable = false)
    private String keycloakId;

    @Column(name = "username", nullable = false, unique = true, updatable = false)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_workspaces",
            schema = "public",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "workspace_id")
    )
    private Set<Workspace> workspaces = new HashSet<>();
}
