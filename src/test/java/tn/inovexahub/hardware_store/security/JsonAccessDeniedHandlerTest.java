package tn.inovexahub.hardware_store.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

class JsonAccessDeniedHandlerTest {

  private JsonAccessDeniedHandler handler;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    handler = new JsonAccessDeniedHandler();
  }

  @Test
  void handle_sets403StatusAndJsonContentType() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);
    when(request.getRequestURI()).thenReturn("/api/test");

    handler.handle(request, response, new AccessDeniedException("denied"));

    verify(response).setStatus(HttpStatus.FORBIDDEN.value());
    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");
  }

  @Test
  void handle_writesErrorResponseWithCorrectFields() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);
    when(request.getRequestURI()).thenReturn("/api/admin");

    handler.handle(request, response, new AccessDeniedException("Access Denied"));

    printWriter.flush();
    JsonNode json = objectMapper.readTree(stringWriter.toString());
    assertEquals(403, json.get("status").asInt());
    assertEquals("Access Denied", json.get("error").asText());
    assertEquals(
        "You do not have permission to access this resource", json.get("message").asText());
    assertEquals("/api/admin", json.get("path").asText());
    assertNotNull(json.get("timestamp"));
  }
}
