package tn.inovexahub.hardware_store.security;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

  private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

  private static final int MAX_ATTEMPTS = 5;
  private static final long WINDOW_MS = 15 * 60 * 1000L;

  public boolean isBlocked(String key) {
    AttemptInfo info = attempts.get(key);
    if (info == null) {
      return false;
    }
    if (System.currentTimeMillis() - info.windowStart() > WINDOW_MS) {
      attempts.remove(key, info);
      return false;
    }
    return info.count() >= MAX_ATTEMPTS;
  }

  public void recordFailure(String key) {
    attempts.compute(
        key,
        (k, existing) -> {
          long now = System.currentTimeMillis();
          if (existing == null || now - existing.windowStart() > WINDOW_MS) {
            return new AttemptInfo(1, now);
          }
          return new AttemptInfo(existing.count() + 1, existing.windowStart());
        });
  }

  public void reset(String key) {
    attempts.remove(key);
  }

  @Scheduled(fixedRate = 5 * 60 * 1000)
  public void evictExpiredEntries() {
    long now = System.currentTimeMillis();
    Iterator<Map.Entry<String, AttemptInfo>> it = attempts.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, AttemptInfo> entry = it.next();
      if (now - entry.getValue().windowStart() > WINDOW_MS) {
        it.remove();
      }
    }
  }

  private record AttemptInfo(int count, long windowStart) {}
}
