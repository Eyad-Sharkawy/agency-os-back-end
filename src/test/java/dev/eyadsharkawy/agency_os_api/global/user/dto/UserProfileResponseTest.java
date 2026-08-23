package dev.eyadsharkawy.agency_os_api.global.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserProfileResponseTest {

  @Test
  void fromEntity_ShouldMapAllFieldsCorrectly() {
    UUID userId = UUID.randomUUID();
    AppUser user = new AppUser();
    user.setId(userId);
    user.setKeycloakId("kc-123");
    user.setUsername("jdoe");
    user.setEmail("jdoe@example.com");
    user.setFirstName("John");
    user.setLastName("Doe");

    UserProfileResponse response = UserProfileResponse.fromEntity(user);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(userId);
    assertThat(response.keycloakId()).isEqualTo("kc-123");
    assertThat(response.username()).isEqualTo("jdoe");
    assertThat(response.email()).isEqualTo("jdoe@example.com");
    assertThat(response.firstName()).isEqualTo("John");
    assertThat(response.lastName()).isEqualTo("Doe");
  }
}
