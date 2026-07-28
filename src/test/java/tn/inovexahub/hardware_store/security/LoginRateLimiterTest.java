package tn.inovexahub.hardware_store.security;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

  private LoginRateLimiter loginRateLimiter;

  @BeforeEach
  void setUp() {
    loginRateLimiter = new LoginRateLimiter();
  }

  @Test
  void isBlocked_keyNotPresent_returnsFalse() {
    assertFalse(loginRateLimiter.isBlocked("unknown-key"));
  }

  @Test
  void isBlocked_belowThreshold_returnsFalse() {
    for (int i = 0; i < 4; i++) {
      loginRateLimiter.recordFailure("user1");
    }
    assertFalse(loginRateLimiter.isBlocked("user1"));
  }

  @Test
  void isBlocked_atThreshold_returnsTrue() {
    for (int i = 0; i < 5; i++) {
      loginRateLimiter.recordFailure("user1");
    }
    assertTrue(loginRateLimiter.isBlocked("user1"));
  }

  @Test
  void recordFailure_incrementsCount() {
    loginRateLimiter.recordFailure("user1");
    loginRateLimiter.recordFailure("user1");
    loginRateLimiter.recordFailure("user1");

    assertFalse(loginRateLimiter.isBlocked("user1"));

    loginRateLimiter.recordFailure("user1");
    assertFalse(loginRateLimiter.isBlocked("user1"));

    loginRateLimiter.recordFailure("user1");
    assertTrue(loginRateLimiter.isBlocked("user1"));
  }

  @Test
  void reset_clearsAttempts() {
    for (int i = 0; i < 5; i++) {
      loginRateLimiter.recordFailure("user1");
    }
    assertTrue(loginRateLimiter.isBlocked("user1"));

    loginRateLimiter.reset("user1");
    assertFalse(loginRateLimiter.isBlocked("user1"));
  }

  @Test
  void evictExpiredEntries_runsWithoutError() {
    for (int i = 0; i < 3; i++) {
      loginRateLimiter.recordFailure("user1");
    }
    assertDoesNotThrow(loginRateLimiter::evictExpiredEntries);
  }

  @Test
  void differentKeys_areIndependent() {
    for (int i = 0; i < 5; i++) {
      loginRateLimiter.recordFailure("user1");
    }
    assertTrue(loginRateLimiter.isBlocked("user1"));
    assertFalse(loginRateLimiter.isBlocked("user2"));
  }

  @Test
  void isBlocked_ExpiredWindow_ReturnsFalseAndRemovesEntry() {
    for (int i = 0; i < 5; i++) {
      loginRateLimiter.recordFailure("user1");
    }
    assertTrue(loginRateLimiter.isBlocked("user1"));

    // Replace the entry with a backdated AttemptInfo via record canonical constructor
    try {
      Field attemptsField = LoginRateLimiter.class.getDeclaredField("attempts");
      attemptsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      ConcurrentHashMap<String, Object> attemptsMap =
          (ConcurrentHashMap<String, Object>) attemptsField.get(loginRateLimiter);
      Class<?> attemptInfoClass = attemptsMap.get("user1").getClass();
      java.lang.reflect.Constructor<?> ctor =
          attemptInfoClass.getDeclaredConstructor(int.class, long.class);
      ctor.setAccessible(true);
      Object expiredInfo = ctor.newInstance(5, System.currentTimeMillis() - 16 * 60 * 1000L);
      attemptsMap.put("user1", expiredInfo);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    assertFalse(loginRateLimiter.isBlocked("user1"));
  }

  @Test
  void recordFailure_ExpiredWindow_ResetsCount() {
    for (int i = 0; i < 5; i++) {
      loginRateLimiter.recordFailure("user1");
    }
    assertTrue(loginRateLimiter.isBlocked("user1"));

    // Replace the entry with a backdated AttemptInfo via record canonical constructor
    try {
      Field attemptsField = LoginRateLimiter.class.getDeclaredField("attempts");
      attemptsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      ConcurrentHashMap<String, Object> attemptsMap =
          (ConcurrentHashMap<String, Object>) attemptsField.get(loginRateLimiter);
      Class<?> attemptInfoClass = attemptsMap.get("user1").getClass();
      java.lang.reflect.Constructor<?> ctor =
          attemptInfoClass.getDeclaredConstructor(int.class, long.class);
      ctor.setAccessible(true);
      Object expiredInfo = ctor.newInstance(5, System.currentTimeMillis() - 16 * 60 * 1000L);
      attemptsMap.put("user1", expiredInfo);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // A new failure should start a fresh window since the old one is expired
    loginRateLimiter.recordFailure("user1");
    assertFalse(loginRateLimiter.isBlocked("user1"));
  }

  @Test
  void evictExpiredEntries_RemovesExpiredEntries() {
    loginRateLimiter.recordFailure("expired-user");
    loginRateLimiter.recordFailure("expired-user");
    loginRateLimiter.recordFailure("expired-user");
    loginRateLimiter.recordFailure("active-user");
    loginRateLimiter.recordFailure("active-user");
    loginRateLimiter.recordFailure("active-user");

    try {
      Field attemptsField = LoginRateLimiter.class.getDeclaredField("attempts");
      attemptsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      ConcurrentHashMap<String, Object> attemptsMap =
          (ConcurrentHashMap<String, Object>) attemptsField.get(loginRateLimiter);
      Class<?> attemptInfoClass = attemptsMap.get("expired-user").getClass();
      java.lang.reflect.Constructor<?> ctor =
          attemptInfoClass.getDeclaredConstructor(int.class, long.class);
      ctor.setAccessible(true);
      Object expiredInfo = ctor.newInstance(3, System.currentTimeMillis() - 16 * 60 * 1000L);
      attemptsMap.put("expired-user", expiredInfo);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    loginRateLimiter.evictExpiredEntries();

    try {
      Field attemptsField = LoginRateLimiter.class.getDeclaredField("attempts");
      attemptsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      ConcurrentHashMap<String, Object> attemptsMap =
          (ConcurrentHashMap<String, Object>) attemptsField.get(loginRateLimiter);
      assertFalse(attemptsMap.containsKey("expired-user"));
      assertTrue(attemptsMap.containsKey("active-user"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void recordFailure_NewKey_CreatesNewWindow() {
    loginRateLimiter.recordFailure("new-user");
    assertFalse(loginRateLimiter.isBlocked("new-user"));

    loginRateLimiter.recordFailure("new-user");
    assertFalse(loginRateLimiter.isBlocked("new-user"));
  }

  @Test
  void isBlocked_MultipleKeys_IndependentTracking() {
    for (int i = 0; i < 5; i++) {
      loginRateLimiter.recordFailure("blocked-user");
    }
    for (int i = 0; i < 3; i++) {
      loginRateLimiter.recordFailure("active-user");
    }
    assertTrue(loginRateLimiter.isBlocked("blocked-user"));
    assertFalse(loginRateLimiter.isBlocked("active-user"));
  }
}
