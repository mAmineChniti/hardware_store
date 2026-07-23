package tn.inovexahub.hardware_store.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.RefreshToken;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final long refreshExpirationMs;

  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository,
      @Value("${jwt.refresh-expiration}") long refreshExpirationMs) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshExpirationMs = refreshExpirationMs;
  }

  public long getRefreshExpirationMs() {
    return refreshExpirationMs;
  }

  private String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  @Transactional
  public RefreshToken createRefreshToken(User user) {
    String rawToken = UUID.randomUUID().toString();
    String hashedToken = hashToken(rawToken);

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken(hashedToken);
    refreshToken.setUser(user);
    refreshToken.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)));
    refreshToken.setRevoked(false);
    RefreshToken savedToken = refreshTokenRepository.save(refreshToken);

    // Set the raw token temporarily on the saved entity to return to the controller
    savedToken.setToken(rawToken);
    return savedToken;
  }

  @Transactional(readOnly = true)
  public RefreshToken verifyRefreshToken(String rawToken) {
    String hashedToken = hashToken(rawToken);
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(hashedToken)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

    if (Boolean.TRUE.equals(refreshToken.getRevoked())
        || !refreshToken.getExpiresAt().isAfter(LocalDateTime.now())) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
    }

    return refreshToken;
  }

  @Transactional
  public RefreshToken rotateRefreshToken(RefreshToken refreshToken) {
    // Revoke the old token first
    refreshToken.setRevoked(true);
    refreshTokenRepository.save(refreshToken);
    // Create and save new token
    return createRefreshToken(refreshToken.getUser());
  }

  @Transactional
  public RefreshToken atomicRefresh(String rawToken) {
    String hashedToken = hashToken(rawToken);
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(hashedToken)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

    // Validate the token
    if (Boolean.TRUE.equals(refreshToken.getRevoked())
        || !refreshToken.getExpiresAt().isAfter(LocalDateTime.now())) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
    }

    // Revoke the old token and create new one atomically
    refreshToken.setRevoked(true);
    refreshTokenRepository.save(refreshToken);
    return createRefreshToken(refreshToken.getUser());
  }

  @Transactional
  public void revokeRefreshToken(String rawToken) {
    String hashedToken = hashToken(rawToken);
    refreshTokenRepository
        .findByToken(hashedToken)
        .ifPresent(
            refreshToken -> {
              refreshToken.setRevoked(true);
              refreshTokenRepository.save(refreshToken);
            });
  }
}
