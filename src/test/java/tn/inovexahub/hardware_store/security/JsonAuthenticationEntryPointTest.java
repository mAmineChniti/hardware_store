package tn.inovexahub.hardware_store.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

class JsonAuthenticationEntryPointTest {

  private JsonAuthenticationEntryPoint entryPoint;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    entryPoint = new JsonAuthenticationEntryPoint();
  }

  @Test
  void commence_sets401StatusAndWritesErrorResponse() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);
    when(request.getRequestURI()).thenReturn("/api/protected");
    AuthenticationException authException =
        new InsufficientAuthenticationException("Full authentication required");

    entryPoint.commence(request, response, authException);

    verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");

    printWriter.flush();
    JsonNode json = objectMapper.readTree(stringWriter.toString());
    assertEquals(401, json.get("status").asInt());
    assertEquals("Authentication Failed", json.get("error").asText());
    assertEquals("Full authentication required", json.get("message").asText());
    assertEquals("/api/protected", json.get("path").asText());
    assertNotNull(json.get("timestamp"));
  }

  @Test
  void commence_withNullMessage_usesFallbackMessage() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);
    when(request.getRequestURI()).thenReturn("/api/protected");
    AuthenticationException authException = mock(AuthenticationException.class);
    when(authException.getMessage()).thenReturn(null);

    entryPoint.commence(request, response, authException);

    printWriter.flush();
    JsonNode json = objectMapper.readTree(stringWriter.toString());
    assertEquals(401, json.get("status").asInt());
    assertEquals("Authentication required", json.get("message").asText());
  }
}
