package dev.eyadsharkawy.agency_os_api.global.user.service;

import dev.eyadsharkawy.agency_os_api.global.user.dto.UserProfileResponse;
import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

  private final AppUserRepository userRepository;

  @Transactional
  public AppUser getOrSyncUser(Jwt jwt) {
    String keycloakId = jwt.getSubject();
    String username = extractUsername(jwt);
    String email = extractEmail(jwt, username);
    String firstName = extractFirstName(jwt, username);
    String lastName = extractLastName(jwt);

    return userRepository
        .findByKeycloakId(keycloakId)
        .map(
            existingUser -> {
              boolean updated = false;

              if (email != null && !email.equals(existingUser.getEmail())) {
                existingUser.setEmail(email);
                updated = true;
              }
              if (firstName != null && !firstName.equals(existingUser.getFirstName())) {
                existingUser.setFirstName(firstName);
                updated = true;
              }
              if (lastName != null && !lastName.equals(existingUser.getLastName())) {
                existingUser.setLastName(lastName);
                updated = true;
              }

              if (updated) {
                log.debug("Updating profile details for user [{}]", username);
                return userRepository.save(existingUser);
              }
              return existingUser;
            })
        .orElseGet(
            () -> {
              log.info("Registering new Keycloak user in backend: [{}] ({})", username, keycloakId);
              AppUser newUser = new AppUser();
              newUser.setKeycloakId(keycloakId);
              newUser.setUsername(username);
              newUser.setEmail(email);
              newUser.setFirstName(firstName);
              newUser.setLastName(lastName);
              return userRepository.save(newUser);
            });
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getCurrentUserProfile(Jwt jwt) {
    AppUser user = getOrSyncUser(jwt);
    return new UserProfileResponse(
        user.getId(),
        user.getKeycloakId(),
        user.getUsername(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName());
  }

  private String extractUsername(Jwt jwt) {
    String username = jwt.getClaimAsString("preferred_username");
    if (username == null || username.isBlank()) {
      username = jwt.getSubject();
    }
    return username;
  }

  private String extractEmail(Jwt jwt, String fallbackUsername) {
    String email = jwt.getClaimAsString("email");
    if (email == null || email.isBlank()) {
      email = fallbackUsername + "@agency.com";
    }
    return email;
  }

  private String extractFirstName(Jwt jwt, String fallbackUsername) {
    String firstName = jwt.getClaimAsString("given_name");
    if (firstName == null || firstName.isBlank()) {
      firstName = fallbackUsername;
    }
    return firstName;
  }

  private String extractLastName(Jwt jwt) {
    String lastName = jwt.getClaimAsString("family_name");
    if (lastName == null) {
      lastName = "";
    }
    return lastName;
  }
}
