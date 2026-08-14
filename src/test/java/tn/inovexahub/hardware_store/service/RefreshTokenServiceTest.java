package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.RefreshToken;
import tn.inovexahub.hardware_store.repository.RefreshTokenRepository;
import tn.inovexahub.hardware_store.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  private static final Long USER_ID = 1L;

  private JwtUtil jwtUtil;
  private RefreshTokenService refreshTokenService;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(
        jwtUtil,
        "secret",
        "test-secret-key-for-testing-must-be-at-least-256-bits-long-for-security");
    ReflectionTestUtils.setField(jwtUtil, "accessExpiration", 900000L);
    ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 1800000L);

    refreshTokenService = new RefreshTokenService(jwtUtil, refreshTokenRepository);
  }

  @Test
  void generateRefreshToken_ReturnsSignedJwt() {
    String token = refreshTokenService.generateRefreshToken(USER_ID);

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void generateRefreshToken_PersistsTokenRecord() {
    refreshTokenService.generateRefreshToken(USER_ID);

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());

    RefreshToken persisted = captor.getValue();
    assertEquals(USER_ID, persisted.getUserId());
    assertFalse(persisted.isRevoked());
    assertNotNull(persisted.getTokenHash());
    assertEquals(64, persisted.getTokenHash().length());
    assertNotNull(persisted.getExpiresAt());
  }

  @Test
  void extractUserId_ValidRefreshToken_ReturnsUserId() {
    String token = refreshTokenService.generateRefreshToken(USER_ID);

    Long userId = refreshTokenService.extractUserId(token);

    assertEquals(USER_ID, userId);
  }

  @Test
  void extractUserId_AccessTokenUsedAsRefreshToken_ThrowsException() {
    String accessToken = jwtUtil.generateAccessToken(USER_ID);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> refreshTokenService.extractUserId(accessToken));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void extractUserId_InvalidToken_ThrowsException() {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> refreshTokenService.extractUserId("invalid.token.here"));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void extractUserId_ExpiredToken_ThrowsException() {
    ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", -1000L);
    String token = refreshTokenService.generateRefreshToken(USER_ID);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> refreshTokenService.extractUserId(token));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void extractUserId_LegacyUsernameSubject_ThrowsUnauthorized() {
    String legacyToken =
        Jwts.builder()
            .subject("legacy@example.com")
            .claim(JwtUtil.TOKEN_TYPE_CLAIM, JwtUtil.REFRESH_TOKEN_TYPE)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(
                Keys.hmacShaKeyFor(
                    "test-secret-key-for-testing-must-be-at-least-256-bits-long-for-security"
                        .getBytes(StandardCharsets.UTF_8)))
            .compact();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> refreshTokenService.extractUserId(legacyToken));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void validateRefreshTokenForUser_MatchingUser_Success() {
    String token = refreshTokenService.generateRefreshToken(USER_ID);

    refreshTokenService.validateRefreshTokenForUser(token, USER_ID);
    // No exception thrown means success.
  }

  @Test
  void validateRefreshTokenForUser_MismatchedUser_ThrowsException() {
    String token = refreshTokenService.generateRefreshToken(USER_ID);

    assertThrows(
        ResponseStatusException.class,
        () -> refreshTokenService.validateRefreshTokenForUser(token, 2L));
  }

  @Test
  void rotate_ActiveToken_RevokesOldAndIssuesNewToken() {
    String oldToken = refreshTokenService.generateRefreshToken(USER_ID);
    when(refreshTokenRepository.revokeIfActive(anyString())).thenReturn(1);

    String newToken = refreshTokenService.rotate(oldToken, USER_ID);

    assertNotNull(newToken);
    assertNotEquals(oldToken, newToken);
    assertEquals(USER_ID, refreshTokenService.extractUserId(newToken));
    // Once for the initial token generated above, once more for the rotated token.
    verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
  }

  @Test
  void rotate_AlreadyUsedToken_ThrowsUnauthorized() {
    String oldToken = refreshTokenService.generateRefreshToken(USER_ID);
    when(refreshTokenRepository.revokeIfActive(anyString())).thenReturn(0);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> refreshTokenService.rotate(oldToken, USER_ID));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void revoke_ActiveToken_RevokesSuccessfully() {
    String token = refreshTokenService.generateRefreshToken(USER_ID);
    when(refreshTokenRepository.revokeIfActive(anyString())).thenReturn(1);

    refreshTokenService.revoke(token);

    verify(refreshTokenRepository).revokeIfActive(anyString());
  }

  @Test
  void revoke_AlreadyRevokedToken_ThrowsUnauthorized() {
    String token = refreshTokenService.generateRefreshToken(USER_ID);
    when(refreshTokenRepository.revokeIfActive(anyString())).thenReturn(0);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> refreshTokenService.revoke(token));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void revoke_MalformedToken_ThrowsUnauthorizedWithoutTouchingRepository() {
    assertThrows(
        ResponseStatusException.class, () -> refreshTokenService.revoke("not-a-real-token"));

    verify(refreshTokenRepository, never()).revokeIfActive(anyString());
  }

  @Test
  void revokeAllForUser_DelegatesToRepository() {
    refreshTokenService.revokeAllForUser(USER_ID);

    verify(refreshTokenRepository).revokeAllActiveForUser(USER_ID);
  }
}
