package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import tn.inovexahub.hardware_store.entity.PasswordResetToken;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.UserRole;
import tn.inovexahub.hardware_store.repository.PasswordResetTokenRepository;
import tn.inovexahub.hardware_store.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private EmailService emailService;
  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private TransactionTemplate transactionTemplate;

  private PasswordResetService passwordResetService;
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private static final Pattern SIX_DIGIT_OTP = Pattern.compile("^\\d{6}$");

  @BeforeEach
  void setUp() {
    lenient()
        .doAnswer(
            inv -> {
              java.util.function.Consumer<?> callback = inv.getArgument(0);
              callback.accept(null);
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());

    passwordResetService =
        new PasswordResetService(
            passwordResetTokenRepository,
            userRepository,
            emailService,
            passwordEncoder,
            applicationEventPublisher,
            transactionTemplate,
            10);
  }

  @Test
  void requestPasswordReset_UserDeletedBetweenLookupAndLock_SilentNoOp() {
    User user = createUser("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

    passwordResetService.requestPasswordReset("test@example.com");

    verify(passwordResetTokenRepository, never()).revokeAllActiveForUserId(any());
    verify(passwordResetTokenRepository, never()).save(any());
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  void requestPasswordReset_ExistingEmail_SavesTokenAndPublishesEvent() {
    User user = createUser("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
    when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    LocalDateTime start = LocalDateTime.now();
    passwordResetService.requestPasswordReset("test@example.com");
    LocalDateTime end = LocalDateTime.now();

    verify(passwordResetTokenRepository).revokeAllActiveForUserId(1L);

    ArgumentCaptor<PasswordResetToken> tokenCaptor =
        ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(passwordResetTokenRepository).save(tokenCaptor.capture());

    PasswordResetToken savedToken = tokenCaptor.getValue();
    assertEquals("test@example.com", savedToken.getEmail());
    assertEquals(user, savedToken.getUser());
    assertNotNull(savedToken.getOtpCode());
    assertTrue(SIX_DIGIT_OTP.matcher(eventOtpCode(savedToken)).matches());
    assertFalse(savedToken.getUsed());
    assertEquals(0, savedToken.getFailedAttempts());
    LocalDateTime expectedMin = start.plusMinutes(10);
    LocalDateTime expectedMax = end.plusMinutes(10);
    assertFalse(savedToken.getExpiresAt().isBefore(expectedMin));
    assertFalse(savedToken.getExpiresAt().isAfter(expectedMax));

    ArgumentCaptor<OtpEmailRequestedEvent> eventCaptor =
        ArgumentCaptor.forClass(OtpEmailRequestedEvent.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    OtpEmailRequestedEvent event = eventCaptor.getValue();
    assertEquals("test@example.com", event.getEmail());
    assertEquals(10, event.getExpiryMinutes());
    assertTrue(SIX_DIGIT_OTP.matcher(event.getOtpCode()).matches());
    assertTrue(passwordEncoder.matches(event.getOtpCode(), savedToken.getOtpCode()));
  }

  @Test
  void requestPasswordReset_NonExistingEmail_NoOp() {
    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    passwordResetService.requestPasswordReset("nobody@example.com");

    verify(passwordResetTokenRepository, never()).save(any());
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  void resetPassword_ValidOtp_ResetsPasswordAndMarksUsed() {
    User user = createUser("test@example.com");
    user.setPassword(passwordEncoder.encode("oldPassword"));

    String rawOtp = "123456";
    PasswordResetToken token = createToken(user, rawOtp);

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordResetTokenRepository.findByUserIdAndUsedFalseForUpdate(1L))
        .thenReturn(Optional.of(token));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    passwordResetService.resetPassword("test@example.com", rawOtp, "newPassword123");

    assertTrue(token.getUsed());
    assertTrue(passwordEncoder.matches("newPassword123", user.getPassword()));
    assertFalse(passwordEncoder.matches("oldPassword", user.getPassword()));

    verify(userRepository).save(user);
    verify(passwordResetTokenRepository).save(token);
  }

  @Test
  void resetPassword_InvalidOtp_IncrementsFailedAttempts() {
    User user = createUser("test@example.com");
    PasswordResetToken token = createToken(user, "123456");

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordResetTokenRepository.findByUserIdAndUsedFalseForUpdate(1L))
        .thenReturn(Optional.of(token));

    InvalidOtpException ex =
        assertThrows(
            InvalidOtpException.class,
            () -> passwordResetService.resetPassword("test@example.com", "999999", "newPass123"));
    assertEquals("Invalid or already used OTP code", ex.getMessage());
    assertEquals(1, token.getFailedAttempts());
    assertFalse(token.getUsed());

    verify(userRepository, never()).save(any());
  }

  @Test
  void resetPassword_TooManyFailedAttempts_InvalidatesToken() {
    User user = createUser("test@example.com");
    PasswordResetToken token = createToken(user, "123456");
    token.setFailedAttempts(4);

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordResetTokenRepository.findByUserIdAndUsedFalseForUpdate(1L))
        .thenReturn(Optional.of(token));

    assertThrows(
        InvalidOtpException.class,
        () -> passwordResetService.resetPassword("test@example.com", "999999", "newPass123"));
    assertEquals(5, token.getFailedAttempts());
    assertTrue(token.getUsed());
  }

  @Test
  void resetPassword_ExpiredOtp_ThrowsException() {
    User user = createUser("test@example.com");
    PasswordResetToken token = createToken(user, "123456");
    token.setExpiresAt(LocalDateTime.now().minusMinutes(5));

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordResetTokenRepository.findByUserIdAndUsedFalseForUpdate(1L))
        .thenReturn(Optional.of(token));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> passwordResetService.resetPassword("test@example.com", "123456", "newPass123"));
    assertEquals("OTP code has expired. Request a new one.", ex.getMessage());

    verify(userRepository, never()).save(any());
  }

  @Test
  void resetPassword_NoUnusedToken_ThrowsException() {
    User user = createUser("test@example.com");

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordResetTokenRepository.findByUserIdAndUsedFalseForUpdate(1L))
        .thenReturn(Optional.empty());

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> passwordResetService.resetPassword("test@example.com", "123456", "newPass123"));
    assertEquals("Invalid or already used OTP code", ex.getMessage());

    verify(userRepository, never()).save(any());
  }

  @Test
  void resetPassword_UserNotFound_ThrowsException() {
    when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                passwordResetService.resetPassword("unknown@example.com", "123456", "newPass123"));
    assertEquals("User not found", ex.getMessage());
  }

  @Test
  void requestPasswordReset_ConcurrentRequests_OnlyOneActiveToken() throws Exception {
    User user = createUser("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

    List<PasswordResetToken> savedTokens = new CopyOnWriteArrayList<>();
    when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
        .thenAnswer(
            inv -> {
              PasswordResetToken t = inv.getArgument(0);
              savedTokens.add(t);
              return t;
            });

    int threadCount = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    List<Future<?>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      futures.add(
          executor.submit(
              () -> {
                try {
                  if (!startLatch.await(5, TimeUnit.SECONDS)) {
                    return;
                  }
                  passwordResetService.requestPasswordReset("test@example.com");
                } catch (InterruptedException ie) {
                  Thread.currentThread().interrupt();
                } finally {
                  doneLatch.countDown();
                }
              }));
    }

    startLatch.countDown();
    if (!doneLatch.await(10, TimeUnit.SECONDS)) {
      executor.shutdownNow();
      fail("Timed out waiting for concurrent threads");
    }
    executor.shutdown();

    // All threads should complete without throwing (concurrent constraint violations are caught)
    for (Future<?> f : futures) {
      f.get();
    }

    // Each call should have issued exactly one token via save()
    assertEquals(threadCount, savedTokens.size());

    // All saved tokens should reference the same user
    for (PasswordResetToken t : savedTokens) {
      assertEquals(user.getId(), t.getUser().getId());
    }

    // RevokeAll should have been called for each request (serialized by the lock)
    verify(passwordResetTokenRepository, atLeastOnce()).revokeAllActiveForUserId(1L);
  }

  private String eventOtpCode(PasswordResetToken savedToken) {
    ArgumentCaptor<OtpEmailRequestedEvent> captor =
        ArgumentCaptor.forClass(OtpEmailRequestedEvent.class);
    verify(applicationEventPublisher).publishEvent(captor.capture());
    return captor.getValue().getOtpCode();
  }

  private PasswordResetToken createToken(User user, String rawOtp) {
    PasswordResetToken token = new PasswordResetToken();
    token.setId(1L);
    token.setUser(user);
    token.setEmail(user.getEmail());
    token.setOtpCode(passwordEncoder.encode(rawOtp));
    token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    token.setUsed(false);
    token.setFailedAttempts(0);
    return token;
  }

  @Test
  void requestPasswordReset_DataIntegrityViolation_SilentlyIgnored() {
    User user = createUser("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findByIdForUpdate(1L))
        .thenThrow(new DataIntegrityViolationException("concurrent insert"));

    // Should not throw
    passwordResetService.requestPasswordReset("test@example.com");

    // No token should be saved since the exception was caught
    verify(passwordResetTokenRepository, never()).save(any());
  }

  private User createUser(String email) {
    User user = new User();
    user.setId(1L);
    user.setFirstName("Test");
    user.setLastName("User");
    user.setEmail(email);
    user.setPassword("encodedPassword");
    user.setRole(UserRole.EMPLOYEE);
    user.setEnabled(true);
    return user;
  }
}
