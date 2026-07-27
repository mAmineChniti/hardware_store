package tn.inovexahub.hardware_store.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tn.inovexahub.hardware_store.dto.ErrorResponse;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public JsonAccessDeniedHandler() {
    this.objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Access Denied",
            "You do not have permission to access this resource",
            LocalDateTime.now(),
            request.getRequestURI(),
            null);

    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getWriter(), errorResponse);
  }
}
