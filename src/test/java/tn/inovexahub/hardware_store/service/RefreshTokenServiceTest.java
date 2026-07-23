package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.RefreshToken;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  private RefreshTokenService refreshTokenService;

  private User user;
  private RefreshToken refreshToken;

  @BeforeEach
  void setUp() {
    refreshTokenService = new RefreshTokenService(refreshTokenRepository, 1800000L);

    user = new User();
    user.setId(1L);
    user.setUsername("testuser");
    user.setEnabled(true);

    refreshToken = new RefreshToken();
    refreshToken.setId(1L);
    refreshToken.setToken("hashed-token");
    refreshToken.setUser(user);
    refreshToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
    refreshToken.setRevoked(false);
    refreshToken.setCreatedAt(LocalDateTime.now());
  }

  @Test
  void createRefreshToken_Success() {
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

    RefreshToken createdToken = refreshTokenService.createRefreshToken(user);

    assertNotNull(createdToken);
    assertNotNull(createdToken.getToken());
    assertEquals(user, createdToken.getUser());
    verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
  }

  @Test
  void verifyRefreshToken_ValidToken_Success() {
    when(refreshTokenRepository.findByToken(any(String.class)))
        .thenReturn(Optional.of(refreshToken));

    RefreshToken verifiedToken = refreshTokenService.verifyRefreshToken("raw-token");

    assertNotNull(verifiedToken);
    assertEquals(refreshToken.getToken(), verifiedToken.getToken());
  }

  @Test
  void verifyRefreshToken_InvalidToken_ThrowsException() {
    when(refreshTokenRepository.findByToken(any(String.class))).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> refreshTokenService.verifyRefreshToken("invalid-token"));
  }

  @Test
  void verifyRefreshToken_RevokedToken_ThrowsException() {
    refreshToken.setRevoked(true);
    when(refreshTokenRepository.findByToken(any(String.class)))
        .thenReturn(Optional.of(refreshToken));

    assertThrows(
        ResponseStatusException.class, () -> refreshTokenService.verifyRefreshToken("raw-token"));
  }

  @Test
  void verifyRefreshToken_ExpiredToken_ThrowsException() {
    refreshToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    when(refreshTokenRepository.findByToken(any(String.class)))
        .thenReturn(Optional.of(refreshToken));

    assertThrows(
        ResponseStatusException.class, () -> refreshTokenService.verifyRefreshToken("raw-token"));
  }

  @Test
  void rotateRefreshToken_Success() {
    RefreshToken newToken = new RefreshToken();
    newToken.setId(2L);
    newToken.setToken("new-hashed-token");
    newToken.setUser(user);
    newToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
    newToken.setRevoked(false);

    when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(newToken);

    RefreshToken rotatedToken = refreshTokenService.rotateRefreshToken(refreshToken);

    assertNotNull(rotatedToken);
    assertFalse(rotatedToken.getRevoked());
    assertTrue(refreshToken.getRevoked());
    verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
  }

  @Test
  void revokeRefreshToken_Success() {
    when(refreshTokenRepository.findByToken(any(String.class)))
        .thenReturn(Optional.of(refreshToken));

    refreshTokenService.revokeRefreshToken("raw-token");

    verify(refreshTokenRepository, times(1)).save(refreshToken);
  }

  @Test
  void revokeRefreshToken_NonExistentToken_DoesNothing() {
    when(refreshTokenRepository.findByToken(any(String.class))).thenReturn(Optional.empty());

    refreshTokenService.revokeRefreshToken("non-existent-token");

    verify(refreshTokenRepository, times(0)).save(any(RefreshToken.class));
  }

  @Test
  void getRefreshExpirationMs_ReturnsConfiguredValue() {
    long expirationMs = refreshTokenService.getRefreshExpirationMs();
    assertEquals(1800000L, expirationMs);
  }
}
