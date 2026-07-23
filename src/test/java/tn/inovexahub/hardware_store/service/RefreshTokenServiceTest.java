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

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.RefreshToken;
import tn.inovexahub.hardware_store.repository.RefreshTokenRepository;
import tn.inovexahub.hardware_store.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  private JwtUtil jwtUtil;
  private RefreshTokenService refreshTokenService;

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private UserDetails unrelatedUserDetails;

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
    String token = refreshTokenService.generateRefreshToken("testuser");

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void generateRefreshToken_PersistsTokenRecord() {
    refreshTokenService.generateRefreshToken("testuser");

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());

    RefreshToken persisted = captor.getValue();
    assertEquals("testuser", persisted.getUsername());
    assertFalse(persisted.isRevoked());
    assertNotNull(persisted.getTokenHash());
    assertEquals(64, persisted.getTokenHash().length());
    assertNotNull(persisted.getExpiresAt());
  }

  @Test
  void extractUsername_ValidRefreshToken_ReturnsUsername() {
    String token = refreshTokenService.generateRefreshToken("testuser");

    String username = refreshTokenService.extractUsername(token);

    assertEquals("testuser", username);
  }

  @Test
  void extractUsername_AccessTokenUsedAsRefreshToken_ThrowsException() {
    String accessToken = jwtUtil.generateAccessToken("testuser");

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> refreshTokenService.extractUsername(accessToken));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void extractUsername_InvalidToken_ThrowsException() {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> refreshTokenService.extractUsername("invalid.token.here"));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void extractUsername_ExpiredToken_ThrowsException() {
    ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", -1000L);
    String token = refreshTokenService.generateRefreshToken("testuser");

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> refreshTokenService.extractUsername(token));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void validateRefreshTokenForUser_MatchingUser_Success() {
    String token = refreshTokenService.generateRefreshToken("testuser");
    UserDetails userDetails = new User("testuser", "password", Collections.emptyList());

    refreshTokenService.validateRefreshTokenForUser(token, userDetails);
    // No exception thrown means success.
  }

  @Test
  void validateRefreshTokenForUser_MismatchedUser_ThrowsException() {
    String token = refreshTokenService.generateRefreshToken("testuser");
    when(unrelatedUserDetails.getUsername()).thenReturn("otheruser");

    assertThrows(
        ResponseStatusException.class,
        () -> refreshTokenService.validateRefreshTokenForUser(token, unrelatedUserDetails));
  }

  @Test
  void rotate_ActiveToken_RevokesOldAndIssuesNewToken() {
    String oldToken = refreshTokenService.generateRefreshToken("testuser");
    when(refreshTokenRepository.revokeIfActive(anyString())).thenReturn(1);

    String newToken = refreshTokenService.rotate(oldToken, "testuser");

    assertNotNull(newToken);
    assertNotEquals(oldToken, newToken);
    assertEquals("testuser", refreshTokenService.extractUsername(newToken));
    // Once for the initial token generated above, once more for the rotated token.
    verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
  }

  @Test
  void rotate_AlreadyUsedToken_ThrowsUnauthorized() {
    String oldToken = refreshTokenService.generateRefreshToken("testuser");
    when(refreshTokenRepository.revokeIfActive(anyString())).thenReturn(0);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> refreshTokenService.rotate(oldToken, "testuser"));
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void revoke_ActiveToken_RevokesSuccessfully() {
    String token = refreshTokenService.generateRefreshToken("testuser");
    when(refreshTokenRepository.revokeIfActive(anyString())).thenReturn(1);

    refreshTokenService.revoke(token);

    verify(refreshTokenRepository).revokeIfActive(anyString());
  }

  @Test
  void revoke_AlreadyRevokedToken_ThrowsUnauthorized() {
    String token = refreshTokenService.generateRefreshToken("testuser");
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
    refreshTokenService.revokeAllForUser("testuser");

    verify(refreshTokenRepository).revokeAllActiveForUser("testuser");
  }
}
