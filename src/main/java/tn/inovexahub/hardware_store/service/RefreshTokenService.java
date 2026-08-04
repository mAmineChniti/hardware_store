package tn.inovexahub.hardware_store.service;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.RefreshToken;
import tn.inovexahub.hardware_store.repository.RefreshTokenRepository;
import tn.inovexahub.hardware_store.security.JwtUtil;

@Service
public class RefreshTokenService {

  private final JwtUtil jwtUtil;
  private final RefreshTokenRepository refreshTokenRepository;

  public RefreshTokenService(JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository) {
    this.jwtUtil = jwtUtil;
    this.refreshTokenRepository = refreshTokenRepository;
  }

  /** Issues a new refresh token for the given user and persists a record of it. */
  @Transactional
  public String generateRefreshToken(Long userId) {
    String rawToken = jwtUtil.generateRefreshToken(userId);

    RefreshToken entity = new RefreshToken();
    entity.setUserId(userId);
    entity.setTokenHash(hash(rawToken));
    entity.setExpiresAt(
        LocalDateTime.now().plusNanos(jwtUtil.getRefreshExpirationMs() * 1_000_000L));
    entity.setRevoked(false);
    refreshTokenRepository.save(entity);

    return rawToken;
  }

  /** Validates the raw JWT itself (signature, type, expiry) and returns its subject user ID. */
  public Long extractUserId(String refreshToken) {
    if (!jwtUtil.validateRefreshToken(refreshToken)) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }
    Long userId = jwtUtil.extractUserId(refreshToken);
    if (userId == null) {
      // E.g. a legacy token whose subject is an email rather than a numeric user ID.
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }
    return userId;
  }

  public void validateRefreshTokenForUser(String refreshToken, Long userId) {
    Long tokenUserId = extractUserId(refreshToken);
    if (!tokenUserId.equals(userId)) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Refresh token does not match user");
    }
  }

  /**
   * Atomically rotates a refresh token: the presented token is revoked (so it can never be
   * presented again) and a brand new refresh token is issued and persisted for the same user, all
   * within a single transaction. Callers must have already validated the token belongs to {@code
   * userId} (e.g. via {@link #validateRefreshTokenForUser}).
   *
   * @return the newly issued raw refresh token
   * @throws ResponseStatusException with 401 if the presented token was already used, revoked, or
   *     was never issued by this service
   */
  @Transactional
  public String rotate(String presentedRefreshToken, Long userId) {
    revokeActiveToken(presentedRefreshToken);
    return generateRefreshToken(userId);
  }

  /**
   * Revokes the presented refresh token so it can no longer be used, e.g. on logout.
   *
   * @throws ResponseStatusException with 401 if the token is malformed/expired, or was already
   *     revoked/used
   */
  @Transactional
  public void revoke(String presentedRefreshToken) {
    extractUserId(presentedRefreshToken); // validates signature, type and expiry first
    revokeActiveToken(presentedRefreshToken);
  }

  /** Revokes all currently active refresh tokens for a user, e.g. when the account is disabled. */
  @Transactional
  public void revokeAllForUser(Long userId) {
    refreshTokenRepository.revokeAllActiveForUser(userId);
  }

  private void revokeActiveToken(String rawToken) {
    int updated = refreshTokenRepository.revokeIfActive(hash(rawToken));
    if (updated == 0) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Refresh token already used or revoked");
    }
  }

  private String hash(String rawToken) {
    return TokenHashUtil.sha256Hex(rawToken);
  }
}
