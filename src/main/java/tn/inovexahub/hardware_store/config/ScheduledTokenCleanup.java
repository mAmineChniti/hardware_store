package tn.inovexahub.hardware_store.config;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.repository.AccessTokenRepository;
import tn.inovexahub.hardware_store.repository.RefreshTokenRepository;

@Component
public class ScheduledTokenCleanup {

  private static final Logger log = LoggerFactory.getLogger(ScheduledTokenCleanup.class);

  private final RefreshTokenRepository refreshTokenRepository;
  private final AccessTokenRepository accessTokenRepository;

  public ScheduledTokenCleanup(
      RefreshTokenRepository refreshTokenRepository, AccessTokenRepository accessTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.accessTokenRepository = accessTokenRepository;
  }

  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void deleteExpiredRefreshTokens() {
    long deleted = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    if (deleted > 0) {
      log.info("Cleaned up {} expired refresh tokens", deleted);
    }
  }

  @Scheduled(cron = "0 15 3 * * *")
  @Transactional
  public void deleteExpiredAccessTokens() {
    long deleted = accessTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    if (deleted > 0) {
      log.info("Cleaned up {} expired access tokens", deleted);
    }
  }
}
