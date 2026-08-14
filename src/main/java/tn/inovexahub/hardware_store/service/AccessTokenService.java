package tn.inovexahub.hardware_store.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.AccessToken;
import tn.inovexahub.hardware_store.repository.AccessTokenRepository;
import tn.inovexahub.hardware_store.security.JwtUtil;

/**
 * Issues access tokens and tracks them in a server-side allowlist so they can be revoked before
 * they naturally expire. Every authenticated request is validated against this store by {@link
 * tn.inovexahub.hardware_store.security.JwtAuthenticationFilter}.
 */
@Service
public class AccessTokenService {

  private final JwtUtil jwtUtil;
  private final AccessTokenRepository accessTokenRepository;

  public AccessTokenService(JwtUtil jwtUtil, AccessTokenRepository accessTokenRepository) {
    this.jwtUtil = jwtUtil;
    this.accessTokenRepository = accessTokenRepository;
  }

  /** Issues a new access token for the given user and persists a record of it. */
  @Transactional
  public String generateAccessToken(Long userId) {
    String rawToken = jwtUtil.generateAccessToken(userId);

    AccessToken entity = new AccessToken();
    entity.setUserId(userId);
    entity.setTokenHash(TokenHashUtil.sha256Hex(rawToken));
    entity.setExpiresAt(
        LocalDateTime.now().plusNanos(jwtUtil.getAccessExpirationMs() * 1_000_000L));
    entity.setRevoked(false);
    accessTokenRepository.save(entity);

    return rawToken;
  }

  /**
   * Returns whether the given access token is currently valid for the user: it must have a
   * persisted record (i.e. be issued by this service), belong to the user, be unexpired, and not be
   * revoked.
   */
  public boolean isActive(String rawToken, Long userId) {
    return accessTokenRepository
        .findByTokenHash(TokenHashUtil.sha256Hex(rawToken))
        .map(
            record ->
                record.getUserId().equals(userId)
                    && !record.isRevoked()
                    && record.getExpiresAt().isAfter(LocalDateTime.now()))
        .orElse(false);
  }

  /** Revokes all currently active access tokens for a user, e.g. when the email changes. */
  @Transactional
  public void revokeAllForUser(Long userId) {
    accessTokenRepository.revokeAllActiveForUser(userId);
  }
}
