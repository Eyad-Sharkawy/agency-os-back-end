package dev.eyadsharkawy.agency_os_api.global.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.repository.AppUserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class UserSyncServiceTest {

  @Mock private AppUserRepository userRepository;

  @InjectMocks private UserSyncService userSyncService;

  private Jwt mockJwt;
  private AppUser existingUser;

  @BeforeEach
  void setUp() {
    mockJwt =
        new Jwt(
            "mock-token-value",
            null,
            null,
            Map.of("alg", "none"),
            Map.of(
                "sub", "kc-user-123",
                "preferred_username", "jdoe",
                "email", "jdoe@example.com",
                "given_name", "John",
                "family_name", "Doe"));

    existingUser = new AppUser();
    existingUser.setId(UUID.randomUUID());
    existingUser.setKeycloakId("kc-user-123");
    existingUser.setUsername("jdoe");
    existingUser.setEmail("jdoe@example.com");
    existingUser.setFirstName("John");
    existingUser.setLastName("Doe");
  }

  @Test
  void getOrSyncUser_WhenUserDoesNotExist_ShouldCreateAndSaveNewUser() {
    when(userRepository.findByKeycloakId("kc-user-123")).thenReturn(Optional.empty());
    when(userRepository.save(any(AppUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AppUser result = userSyncService.getOrSyncUser(mockJwt);

    assertThat(result).isNotNull();
    assertThat(result.getKeycloakId()).isEqualTo("kc-user-123");
    assertThat(result.getUsername()).isEqualTo("jdoe");
    assertThat(result.getEmail()).isEqualTo("jdoe@example.com");
    assertThat(result.getFirstName()).isEqualTo("John");
    assertThat(result.getLastName()).isEqualTo("Doe");

    verify(userRepository, times(1)).save(any(AppUser.class));
  }

  @Test
  void getOrSyncUser_WhenUserExistsWithoutChanges_ShouldReturnExistingUserWithoutSave() {
    when(userRepository.findByKeycloakId("kc-user-123")).thenReturn(Optional.of(existingUser));

    AppUser result = userSyncService.getOrSyncUser(mockJwt);

    assertThat(result).isSameAs(existingUser);
    verify(userRepository, never()).save(any(AppUser.class));
  }

  @Test
  void getOrSyncUser_WhenUserExistsWithUpdatedProfile_ShouldUpdateAndSaveUser() {
    existingUser.setEmail("old@example.com");
    existingUser.setFirstName("OldName");

    when(userRepository.findByKeycloakId("kc-user-123")).thenReturn(Optional.of(existingUser));
    when(userRepository.save(existingUser)).thenReturn(existingUser);

    AppUser result = userSyncService.getOrSyncUser(mockJwt);

    assertThat(result.getEmail()).isEqualTo("jdoe@example.com");
    assertThat(result.getFirstName()).isEqualTo("John");
    verify(userRepository, times(1)).save(existingUser);
  }
}
