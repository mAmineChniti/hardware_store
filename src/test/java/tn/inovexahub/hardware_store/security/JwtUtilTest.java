package tn.inovexahub.hardware_store.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

  private JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(
        jwtUtil,
        "secret",
        "test-secret-key-for-testing-must-be-at-least-256-bits-long-for-security");
    ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
  }

  @Test
  void generateToken_Success() {
    String token = jwtUtil.generateToken("testuser");

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void extractUsername_Success() {
    String username = "testuser";
    String token = jwtUtil.generateToken(username);

    String extractedUsername = jwtUtil.extractUsername(token);

    assertEquals(username, extractedUsername);
  }

  @Test
  void validateToken_ValidToken_Success() {
    String token = jwtUtil.generateToken("testuser");

    boolean isValid = jwtUtil.validateToken(token);

    assertTrue(isValid);
  }

  @Test
  void validateToken_InvalidToken_Failure() {
    boolean isValid = jwtUtil.validateToken("invalid.token.here");

    assertFalse(isValid);
  }

  @Test
  void validateToken_ExpiredToken_Failure() {
    ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
    String token = jwtUtil.generateToken("testuser");

    boolean isValid = jwtUtil.validateToken(token);

    assertFalse(isValid);
  }
}
