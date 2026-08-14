package tn.inovexahub.hardware_store.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

  private static final Long USER_ID = 1L;

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
    String token = jwtUtil.generateAccessToken(USER_ID);

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void generateRefreshToken_Success() {
    String token = jwtUtil.generateRefreshToken(USER_ID);

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void extractUserId_Success() {
    String token = jwtUtil.generateAccessToken(USER_ID);

    Long extractedUserId = jwtUtil.extractUserId(token);

    assertEquals(USER_ID, extractedUserId);
  }

  @Test
  void validateAccessToken_ValidToken_Success() {
    String token = jwtUtil.generateAccessToken(USER_ID);

    boolean isValid = jwtUtil.validateAccessToken(token);

    assertTrue(isValid);
  }

  @Test
  void validateAccessToken_RefreshTokenAsAccessToken_Failure() {
    String refreshToken = jwtUtil.generateRefreshToken(USER_ID);

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
    String token = jwtUtil.generateAccessToken(USER_ID);

    boolean isValid = jwtUtil.validateAccessToken(token);

    assertFalse(isValid);
  }

  @Test
  void validateRefreshToken_ValidToken_Success() {
    String token = jwtUtil.generateRefreshToken(USER_ID);

    boolean isValid = jwtUtil.validateRefreshToken(token);

    assertTrue(isValid);
  }

  @Test
  void validateRefreshToken_AccessTokenAsRefreshToken_Failure() {
    String accessToken = jwtUtil.generateAccessToken(USER_ID);

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
    String token = jwtUtil.generateRefreshToken(USER_ID);

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

  @Test
  void validateAccessTokenAndGetUserId_ValidToken_ReturnsUserId() {
    String token = jwtUtil.generateAccessToken(USER_ID);

    Long userId = jwtUtil.validateAccessTokenAndGetUserId(token);

    assertEquals(USER_ID, userId);
  }

  @Test
  void validateAccessTokenAndGetUserId_RefreshTokenType_ReturnsNull() {
    String refreshToken = jwtUtil.generateRefreshToken(USER_ID);

    Long userId = jwtUtil.validateAccessTokenAndGetUserId(refreshToken);

    assertEquals(null, userId);
  }

  @Test
  void validateAccessTokenAndGetUserId_ExpiredToken_ReturnsNull() {
    ReflectionTestUtils.setField(jwtUtil, "accessExpiration", -1000L);
    String token = jwtUtil.generateAccessToken(USER_ID);

    Long userId = jwtUtil.validateAccessTokenAndGetUserId(token);

    assertEquals(null, userId);
  }

  @Test
  void validateAccessTokenAndGetUserId_InvalidToken_ReturnsNull() {
    Long userId = jwtUtil.validateAccessTokenAndGetUserId("invalid.token.here");

    assertEquals(null, userId);
  }
}
