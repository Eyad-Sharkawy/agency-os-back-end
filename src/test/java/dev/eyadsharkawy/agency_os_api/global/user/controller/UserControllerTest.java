package dev.eyadsharkawy.agency_os_api.global.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.eyadsharkawy.agency_os_api.core.config.JacksonConfig;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantSecurityFilter;
import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.service.UserSyncService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(JacksonConfig.class)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserSyncService userSyncService;
  @MockitoBean private TenantSecurityFilter tenantSecurityFilter;

  @BeforeEach
  void setUp() throws Exception {
    Mockito.doAnswer(
            invocation -> {
              jakarta.servlet.ServletRequest request = invocation.getArgument(0);
              jakarta.servlet.ServletResponse response = invocation.getArgument(1);
              jakarta.servlet.FilterChain chain = invocation.getArgument(2);
              chain.doFilter(request, response);
              return null;
            })
        .when(tenantSecurityFilter)
        .doFilter(any(), any(), any());
  }

  @Test
  void getCurrentUser_WhenAuthenticated_ShouldReturnUserProfile() throws Exception {
    UUID userId = UUID.randomUUID();
    AppUser mockUser = new AppUser();
    mockUser.setId(userId);
    mockUser.setKeycloakId("kc-123");
    mockUser.setUsername("jdoe");
    mockUser.setEmail("jdoe@example.com");
    mockUser.setFirstName("John");
    mockUser.setLastName("Doe");

    when(userSyncService.getOrSyncUser(any())).thenReturn(mockUser);

    mockMvc
        .perform(
            get("/api/v1/users/me")
                .with(jwt().jwt(builder -> builder.subject("kc-123")))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.keycloakId").value("kc-123"))
        .andExpect(jsonPath("$.username").value("jdoe"))
        .andExpect(jsonPath("$.email").value("jdoe@example.com"))
        .andExpect(jsonPath("$.firstName").value("John"))
        .andExpect(jsonPath("$.lastName").value("Doe"));
  }

  @Test
  void getCurrentUser_WhenUnauthenticated_ShouldReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
  }
}
