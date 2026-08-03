package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tn.inovexahub.hardware_store.entity.AccessToken;
import tn.inovexahub.hardware_store.repository.AccessTokenRepository;
import tn.inovexahub.hardware_store.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {

  private static final Long USER_ID = 1L;

  private JwtUtil jwtUtil;
  private AccessTokenService accessTokenService;

  @Mock private AccessTokenRepository accessTokenRepository;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(
        jwtUtil,
        "secret",
        "test-secret-key-for-testing-must-be-at-least-256-bits-long-for-security");
    ReflectionTestUtils.setField(jwtUtil, "accessExpiration", 900000L);
    ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 1800000L);

    accessTokenService = new AccessTokenService(jwtUtil, accessTokenRepository);
  }

  @Test
  void generateAccessToken_ReturnsSignedJwt() {
    String token = accessTokenService.generateAccessToken(USER_ID);

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void generateAccessToken_PersistsTokenRecord() {
    accessTokenService.generateAccessToken(USER_ID);

    ArgumentCaptor<AccessToken> captor = ArgumentCaptor.forClass(AccessToken.class);
    verify(accessTokenRepository).save(captor.capture());

    AccessToken persisted = captor.getValue();
    assertEquals(USER_ID, persisted.getUserId());
    assertFalse(persisted.isRevoked());
    assertNotNull(persisted.getTokenHash());
    assertEquals(64, persisted.getTokenHash().length());
    assertNotNull(persisted.getExpiresAt());
  }

  @Test
  void isActive_ActiveToken_ReturnsTrue() {
    String token = accessTokenService.generateAccessToken(USER_ID);
    AccessToken record = activeRecord(USER_ID, LocalDateTime.now().plusMinutes(5));
    when(accessTokenRepository.findByTokenHash(TokenHashUtil.sha256Hex(token)))
        .thenReturn(Optional.of(record));

    assertTrue(accessTokenService.isActive(token, USER_ID));
  }

  @Test
  void isActive_UnknownToken_ReturnsFalse() {
    when(accessTokenRepository.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Optional.empty());

    assertFalse(accessTokenService.isActive("some-token", USER_ID));
  }

  @Test
  void isActive_RevokedToken_ReturnsFalse() {
    String token = accessTokenService.generateAccessToken(USER_ID);
    AccessToken record = activeRecord(USER_ID, LocalDateTime.now().plusMinutes(5));
    record.setRevoked(true);
    when(accessTokenRepository.findByTokenHash(TokenHashUtil.sha256Hex(token)))
        .thenReturn(Optional.of(record));

    assertFalse(accessTokenService.isActive(token, USER_ID));
  }

  @Test
  void isActive_ExpiredToken_ReturnsFalse() {
    String token = accessTokenService.generateAccessToken(USER_ID);
    AccessToken record = activeRecord(USER_ID, LocalDateTime.now().minusSeconds(1));
    when(accessTokenRepository.findByTokenHash(TokenHashUtil.sha256Hex(token)))
        .thenReturn(Optional.of(record));

    assertFalse(accessTokenService.isActive(token, USER_ID));
  }

  @Test
  void isActive_OtherUsersToken_ReturnsFalse() {
    String token = accessTokenService.generateAccessToken(USER_ID);
    AccessToken record = activeRecord(2L, LocalDateTime.now().plusMinutes(5));
    when(accessTokenRepository.findByTokenHash(TokenHashUtil.sha256Hex(token)))
        .thenReturn(Optional.of(record));

    assertFalse(accessTokenService.isActive(token, USER_ID));
  }

  @Test
  void revokeAllForUser_DelegatesToRepository() {
    accessTokenService.revokeAllForUser(USER_ID);

    verify(accessTokenRepository).revokeAllActiveForUser(USER_ID);
  }

  private AccessToken activeRecord(Long userId, LocalDateTime expiresAt) {
    AccessToken record = new AccessToken();
    record.setUserId(userId);
    record.setTokenHash("hash");
    record.setExpiresAt(expiresAt);
    record.setRevoked(false);
    return record;
  }
}
