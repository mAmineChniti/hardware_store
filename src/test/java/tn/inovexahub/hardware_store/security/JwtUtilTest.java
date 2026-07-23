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
    ReflectionTestUtils.setField(jwtUtil, "accessExpiration", 900000L);
    ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 1800000L);
  }

  @Test
  void generateAccessToken_Success() {
    String token = jwtUtil.generateAccessToken("testuser");

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void generateRefreshToken_Success() {
    String token = jwtUtil.generateRefreshToken("testuser");

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void extractUsername_Success() {
    String username = "testuser";
    String token = jwtUtil.generateAccessToken(username);

    String extractedUsername = jwtUtil.extractUsername(token);

    assertEquals(username, extractedUsername);
  }

  @Test
  void validateAccessToken_ValidToken_Success() {
    String token = jwtUtil.generateAccessToken("testuser");

    boolean isValid = jwtUtil.validateAccessToken(token);

    assertTrue(isValid);
  }

  @Test
  void validateAccessToken_RefreshTokenAsAccessToken_Failure() {
    String refreshToken = jwtUtil.generateRefreshToken("testuser");

    boolean isValid = jwtUtil.validateAccessToken(refreshToken);

    assertFalse(isValid);
  }

  @Test
  void validateAccessToken_InvalidToken_Failure() {
    boolean isValid = jwtUtil.validateAccessToken("invalid.token.here");

    assertFalse(isValid);
  }

  @Test
  void validateAccessToken_ExpiredToken_Failure() {
    ReflectionTestUtils.setField(jwtUtil, "accessExpiration", -1000L);
    String token = jwtUtil.generateAccessToken("testuser");

    boolean isValid = jwtUtil.validateAccessToken(token);

    assertFalse(isValid);
  }

  @Test
  void validateRefreshToken_ValidToken_Success() {
    String token = jwtUtil.generateRefreshToken("testuser");

    boolean isValid = jwtUtil.validateRefreshToken(token);

    assertTrue(isValid);
  }

  @Test
  void validateRefreshToken_AccessTokenAsRefreshToken_Failure() {
    String accessToken = jwtUtil.generateAccessToken("testuser");

    boolean isValid = jwtUtil.validateRefreshToken(accessToken);

    assertFalse(isValid);
  }

  @Test
  void validateRefreshToken_InvalidToken_Failure() {
    boolean isValid = jwtUtil.validateRefreshToken("invalid.token.here");

    assertFalse(isValid);
  }

  @Test
  void validateRefreshToken_ExpiredToken_Failure() {
    ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", -1000L);
    String token = jwtUtil.generateRefreshToken("testuser");

    boolean isValid = jwtUtil.validateRefreshToken(token);

    assertFalse(isValid);
  }

  @Test
  void getAccessExpirationMs_ReturnsConfiguredValue() {
    assertEquals(900000L, jwtUtil.getAccessExpirationMs());
  }

  @Test
  void getRefreshExpirationMs_ReturnsConfiguredValue() {
    assertEquals(1800000L, jwtUtil.getRefreshExpirationMs());
  }
}
