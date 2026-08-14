package dev.eyadsharkawy.agency_os_api;

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "spring.flyway.clean-disabled=false",
      "spring.flyway.ignore-migration-patterns=*:*"
    })
@Import(BaseIntegrationTest.TestSecurityConfig.class)
public abstract class BaseIntegrationTest {

  protected static PostgreSQLContainer<?> postgres;

  static {
    if (DockerClientFactory.instance().isDockerAvailable()) {
      postgres =
          new PostgreSQLContainer<>("postgres:16-alpine")
              .withDatabaseName("agency_os_test")
              .withUsername("test_user")
              .withPassword("test_pass");
      postgres.start();
    }
  }

  @BeforeAll
  static void setUpAll() {
    Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available, skipping Testcontainers integration tests");
  }

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    if (postgres != null && postgres.isRunning()) {
      registry.add("spring.datasource.url", postgres::getJdbcUrl);
      registry.add("spring.datasource.username", postgres::getUsername);
      registry.add("spring.datasource.password", postgres::getPassword);
      registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
  }

  @TestConfiguration
  public static class TestSecurityConfig {
    @Bean
    public JwtDecoder jwtDecoder() {
      return mock(JwtDecoder.class);
    }
  }
}
