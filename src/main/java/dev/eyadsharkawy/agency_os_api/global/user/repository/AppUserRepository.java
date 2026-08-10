package dev.eyadsharkawy.agency_os_api.global.user.repository;

import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
  Optional<AppUser> findByKeycloakId(String keycloakId);

  boolean existsByKeycloakId(String keycloakId);

  Optional<AppUser> findByEmail(String email);

  Optional<AppUser> findByUsername(String email);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);
}
