package tn.inovexahub.hardware_store.config;

import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.inovexahub.hardware_store.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class ScheduledTokenCleanupTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private ScheduledTokenCleanup scheduledTokenCleanup;

  @Test
  void deleteExpiredRefreshTokens_zeroDeleted() {
    when(refreshTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(0L);

    scheduledTokenCleanup.deleteExpiredRefreshTokens();

    verify(refreshTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
  }

  @Test
  void deleteExpiredRefreshTokens_positiveDeleted() {
    when(refreshTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(7L);

    scheduledTokenCleanup.deleteExpiredRefreshTokens();

    verify(refreshTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
  }
}
