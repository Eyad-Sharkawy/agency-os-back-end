package dev.eyadsharkawy.agency_os_api.global.user.entity;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspace;
import dev.eyadsharkawy.agency_os_api.shared.entity.BaseEntity;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

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

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<UserWorkspace> userWorkspaces = new HashSet<>();
}
