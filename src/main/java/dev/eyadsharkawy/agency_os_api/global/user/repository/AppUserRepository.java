package dev.eyadsharkawy.agency_os_api.global.user.repository;

import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
  Optional<AppUser> findByKeycloakId(String keycloakId);

  boolean existsByKeycloakId(String keycloakId);

  Optional<AppUser> findByEmailIgnoreCase(String email);

  Optional<AppUser> findByUsernameIgnoreCase(String username);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByUsernameIgnoreCase(String username);

  default Optional<AppUser> findByEmail(String email) {
    return findByEmailIgnoreCase(email);
  }

  default Optional<AppUser> findByUsername(String username) {
    return findByUsernameIgnoreCase(username);
  }

  default boolean existsByEmail(String email) {
    return existsByEmailIgnoreCase(email);
  }

  default boolean existsByUsername(String username) {
    return existsByUsernameIgnoreCase(username);
  }
}
