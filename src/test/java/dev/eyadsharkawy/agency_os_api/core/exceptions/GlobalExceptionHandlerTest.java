package dev.eyadsharkawy.agency_os_api.core.exceptions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("Should map ResourceNotFoundException to 404 Not Found ProblemDetail")
  void testResourceNotFound() throws Exception {
    mockMvc
        .perform(get("/test/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Resource not found message"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  @DisplayName("Should map ResourceAlreadyExistsException to 409 Conflict ProblemDetail")
  void testResourceAlreadyExists() throws Exception {
    mockMvc
        .perform(get("/test/already-exists"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.detail").value("Resource already exists message"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  @DisplayName("Should map IllegalArgumentException to 400 Bad Request ProblemDetail")
  void testIllegalArgument() throws Exception {
    mockMvc
        .perform(get("/test/illegal-argument"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Illegal argument message"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  @DisplayName("Should map AccessDeniedException to 403 Forbidden ProblemDetail")
  void testAccessDenied() throws Exception {
    mockMvc
        .perform(get("/test/access-denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.detail").value("Access denied message"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  @DisplayName("Should map generic Exception to 500 Internal Server Error ProblemDetail")
  void testGenericException() throws Exception {
    mockMvc
        .perform(get("/test/generic-exception"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(
            jsonPath("$.detail")
                .value("An unexpected server error occurred. Please try again later."))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @RestController
  static class TestController {

    @GetMapping("/test/not-found")
    public void throwNotFound() {
      throw new ResourceNotFoundException("Resource not found message");
    }

    @GetMapping("/test/already-exists")
    public void throwAlreadyExists() {
      throw new ResourceAlreadyExistsException("Resource already exists message");
    }

    @GetMapping("/test/illegal-argument")
    public void throwIllegalArgument() {
      throw new IllegalArgumentException("Illegal argument message");
    }

    @GetMapping("/test/access-denied")
    public void throwAccessDenied() throws AccessDeniedException {
      throw new AccessDeniedException("Access denied message");
    }

    @GetMapping("/test/generic-exception")
    public void throwGeneric() throws Exception {
      throw new RuntimeException("Generic crash");
    }
  }
}
