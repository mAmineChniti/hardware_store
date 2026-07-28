package tn.inovexahub.hardware_store.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.PasswordResetToken;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.repository.PasswordResetTokenRepository;
import tn.inovexahub.hardware_store.repository.UserRepository;

@Service
public class PasswordResetService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  static final int MAX_OTP_ATTEMPTS = 5;

  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final UserRepository userRepository;
  private final EmailService emailService;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final int otpExpiryMinutes;

  public PasswordResetService(
      PasswordResetTokenRepository passwordResetTokenRepository,
      UserRepository userRepository,
      EmailService emailService,
      PasswordEncoder passwordEncoder,
      ApplicationEventPublisher applicationEventPublisher,
      @Value("${otp.expiry-minutes:10}") int otpExpiryMinutes) {
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.userRepository = userRepository;
    this.emailService = emailService;
    this.passwordEncoder = passwordEncoder;
    this.applicationEventPublisher = applicationEventPublisher;
    this.otpExpiryMinutes = otpExpiryMinutes;
  }

  @Transactional
  public void requestPasswordReset(String email) {
    User user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
      return;
    }

    passwordResetTokenRepository.revokeAllActiveForUserId(user.getId());

    String otpCode = generateOtp();
    PasswordResetToken token = new PasswordResetToken();
    token.setUser(user);
    token.setEmail(email);
    token.setOtpCode(passwordEncoder.encode(otpCode));
    token.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
    token.setUsed(false);
    token.setFailedAttempts(0);
    passwordResetTokenRepository.save(token);

    applicationEventPublisher.publishEvent(
        new OtpEmailRequestedEvent(email, otpCode, otpExpiryMinutes));
  }

  @Transactional(noRollbackFor = InvalidOtpException.class)
  public void resetPassword(String email, String otpCode, String newPassword) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    PasswordResetToken token =
        passwordResetTokenRepository
            .findByUserIdAndUsedFalseForUpdate(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid or already used OTP code"));

    if (token.isExpired()) {
      throw new IllegalArgumentException("OTP code has expired. Request a new one.");
    }

    if (!passwordEncoder.matches(otpCode, token.getOtpCode())) {
      token.setFailedAttempts(token.getFailedAttempts() + 1);
      if (token.getFailedAttempts() >= MAX_OTP_ATTEMPTS) {
        token.setUsed(true);
      }
      throw new InvalidOtpException("Invalid or already used OTP code");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    token.setUsed(true);
    passwordResetTokenRepository.save(token);
  }

  private String generateOtp() {
    int code = SECURE_RANDOM.nextInt(900000) + 100000;
    return String.valueOf(code);
  }
}
