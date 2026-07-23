package tn.inovexahub.hardware_store.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
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
  public String generateRefreshToken(String username) {
    String rawToken = jwtUtil.generateRefreshToken(username);

    RefreshToken entity = new RefreshToken();
    entity.setUsername(username);
    entity.setTokenHash(hash(rawToken));
    entity.setExpiresAt(
        LocalDateTime.now().plusNanos(jwtUtil.getRefreshExpirationMs() * 1_000_000L));
    entity.setRevoked(false);
    refreshTokenRepository.save(entity);

    return rawToken;
  }

  /** Validates the raw JWT itself (signature, type, expiry) and returns its subject username. */
  public String extractUsername(String refreshToken) {
    if (!jwtUtil.validateRefreshToken(refreshToken)) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }

    return jwtUtil.extractUsername(refreshToken);
  }

  public void validateRefreshTokenForUser(String refreshToken, UserDetails userDetails) {
    String username = extractUsername(refreshToken);
    if (!userDetails.getUsername().equals(username)) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Refresh token does not match user");
    }
  }

  /**
   * Atomically rotates a refresh token: the presented token is revoked (so it can never be
   * presented again) and a brand new refresh token is issued and persisted for the same user, all
   * within a single transaction. Callers must have already validated the token belongs to {@code
   * username} (e.g. via {@link #validateRefreshTokenForUser}).
   *
   * @return the newly issued raw refresh token
   * @throws ResponseStatusException with 401 if the presented token was already used, revoked, or
   *     was never issued by this service
   */
  @Transactional
  public String rotate(String presentedRefreshToken, String username) {
    revokeActiveToken(presentedRefreshToken);
    return generateRefreshToken(username);
  }

  /**
   * Revokes the presented refresh token so it can no longer be used, e.g. on logout.
   *
   * @throws ResponseStatusException with 401 if the token is malformed/expired, or was already
   *     revoked/used
   */
  @Transactional
  public void revoke(String presentedRefreshToken) {
    extractUsername(presentedRefreshToken); // validates signature, type and expiry first
    revokeActiveToken(presentedRefreshToken);
  }

  /** Revokes all currently active refresh tokens for a user, e.g. when the account is disabled. */
  @Transactional
  public void revokeAllForUser(String username) {
    refreshTokenRepository.revokeAllActiveForUser(username);
  }

  private void revokeActiveToken(String rawToken) {
    int updated = refreshTokenRepository.revokeIfActive(hash(rawToken));
    if (updated == 0) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Refresh token already used or revoked");
    }
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
