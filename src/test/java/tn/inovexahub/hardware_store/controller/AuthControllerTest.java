package tn.inovexahub.hardware_store.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.dto.ForgotPasswordRequest;
import tn.inovexahub.hardware_store.dto.RegisterRequest;
import tn.inovexahub.hardware_store.dto.ResetPasswordRequest;
import tn.inovexahub.hardware_store.dto.UpdateUserRequest;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.UserRole;
import tn.inovexahub.hardware_store.repository.PasswordResetTokenRepository;
import tn.inovexahub.hardware_store.repository.UserRepository;
import tn.inovexahub.hardware_store.security.JwtUtil;
import tn.inovexahub.hardware_store.security.LoginRateLimiter;
import tn.inovexahub.hardware_store.service.PasswordResetService;
import tn.inovexahub.hardware_store.service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtUtil jwtUtil;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private UserRepository userRepository;
  @Mock private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private LoginRateLimiter loginRateLimiter;
  @Mock private PasswordResetService passwordResetService;
  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private HttpServletRequest httpRequest;

  private AuthController authController;

  @BeforeEach
  void setUp() {
    authController =
        new AuthController(
            authenticationManager,
            jwtUtil,
            refreshTokenService,
            userRepository,
            userDetailsService,
            passwordEncoder,
            loginRateLimiter,
            passwordResetService,
            passwordResetTokenRepository);
  }

  private void stubRateLimiter() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
  }

  // --- forgotPassword ---

  @Test
  void forgotPassword_ValidEmail_ReturnsOk() {
    stubRateLimiter();
    when(loginRateLimiter.isBlocked("forgot:127.0.0.1")).thenReturn(false);
    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail("user@example.com");
    doNothing().when(passwordResetService).requestPasswordReset("user@example.com");

    ResponseEntity<Void> response = authController.forgotPassword(request, httpRequest);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(passwordResetService).requestPasswordReset("user@example.com");
    verify(loginRateLimiter).recordFailure("forgot:127.0.0.1");
  }

  @Test
  void forgotPassword_NonExistingEmail_ReturnsOk() {
    stubRateLimiter();
    when(loginRateLimiter.isBlocked("forgot:127.0.0.1")).thenReturn(false);
    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail("nobody@example.com");
    doThrow(new IllegalArgumentException("No account found with that email address"))
        .when(passwordResetService)
        .requestPasswordReset("nobody@example.com");

    ResponseEntity<Void> response = authController.forgotPassword(request, httpRequest);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(loginRateLimiter).recordFailure("forgot:127.0.0.1");
  }

  @Test
  void forgotPassword_RateLimited_ReturnsTooManyRequests() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    when(loginRateLimiter.isBlocked("forgot:127.0.0.1")).thenReturn(true);

    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail("user@example.com");

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.forgotPassword(request, httpRequest));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    verify(passwordResetService, never()).requestPasswordReset(any());
  }

  // --- resetPassword ---

  @Test
  void resetPassword_ValidOtp_ReturnsOk() {
    stubRateLimiter();
    when(loginRateLimiter.isBlocked("reset:127.0.0.1")).thenReturn(false);
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setEmail("user@example.com");
    request.setOtpCode("123456");
    request.setNewPassword("newSecurePass123");
    doNothing()
        .when(passwordResetService)
        .resetPassword("user@example.com", "123456", "newSecurePass123");

    User user = createUser("user@example.com");
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

    ResponseEntity<Void> response = authController.resetPassword(request, httpRequest);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(passwordResetService).resetPassword("user@example.com", "123456", "newSecurePass123");
    verify(refreshTokenService).revokeAllForUser("testuser");
  }

  @Test
  void resetPassword_InvalidOtp_ReturnsBadRequest() {
    stubRateLimiter();
    when(loginRateLimiter.isBlocked("reset:127.0.0.1")).thenReturn(false);
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setEmail("user@example.com");
    request.setOtpCode("999999");
    request.setNewPassword("newSecurePass123");
    doThrow(new IllegalArgumentException("Invalid or already used OTP code"))
        .when(passwordResetService)
        .resetPassword("user@example.com", "999999", "newSecurePass123");

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.resetPassword(request, httpRequest));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    verify(loginRateLimiter).recordFailure("reset:127.0.0.1");
  }

  @Test
  void resetPassword_ExpiredOtp_ReturnsBadRequest() {
    stubRateLimiter();
    when(loginRateLimiter.isBlocked("reset:127.0.0.1")).thenReturn(false);
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setEmail("user@example.com");
    request.setOtpCode("123456");
    request.setNewPassword("newSecurePass123");
    doThrow(new IllegalArgumentException("OTP code has expired. Request a new one."))
        .when(passwordResetService)
        .resetPassword("user@example.com", "123456", "newSecurePass123");

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.resetPassword(request, httpRequest));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    verify(loginRateLimiter).recordFailure("reset:127.0.0.1");
  }

  @Test
  void resetPassword_RateLimited_ReturnsTooManyRequests() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    when(loginRateLimiter.isBlocked("reset:127.0.0.1")).thenReturn(true);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setEmail("user@example.com");
    request.setOtpCode("123456");
    request.setNewPassword("newSecurePass123");

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.resetPassword(request, httpRequest));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    verify(passwordResetService, never()).resetPassword(any(), any(), any());
  }

  // --- register ---

  @Test
  void register_NewUser_ReturnsOk() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setEmail("new@example.com");
    request.setPassword("password123");
    request.setFullName("New User");

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

    User savedUser = createUser("new@example.com");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    ResponseEntity<User> response = authController.register(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void register_DuplicateUsername_ReturnsConflict() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("existing");
    request.setEmail("new@example.com");
    request.setPassword("password123");
    request.setFullName("New User");

    when(userRepository.existsByUsername("existing")).thenReturn(true);

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> authController.register(request));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verify(userRepository, never()).save(any());
  }

  @Test
  void register_DuplicateEmail_ReturnsConflict() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setEmail("existing@example.com");
    request.setPassword("password123");
    request.setFullName("New User");

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> authController.register(request));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verify(userRepository, never()).save(any());
  }

  // --- updateUser ---

  @Test
  void updateUser_NewEmail_ReturnsOk() {
    User existing = createUser("old@example.com");
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("new@example.com");

    ResponseEntity<User> response = authController.updateUser(1L, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("new@example.com", existing.getEmail());
  }

  @Test
  void updateUser_SameEmail_ReturnsOk() {
    User existing = createUser("same@example.com");
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("same@example.com");

    ResponseEntity<User> response = authController.updateUser(1L, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void updateUser_DuplicateEmail_ReturnsConflict() {
    User existing = createUser("old@example.com");
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("taken@example.com");

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> authController.updateUser(1L, request));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
  }

  @Test
  void updateUser_NonExistingUser_ReturnsNotFound() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    UpdateUserRequest request = new UpdateUserRequest();
    request.setFullName("Updated");

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> authController.updateUser(999L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  private User createUser(String email) {
    User user = new User();
    user.setId(1L);
    user.setUsername("testuser");
    user.setEmail(email);
    user.setPassword("encodedPassword");
    user.setFullName("Test User");
    user.setRole(UserRole.EMPLOYEE);
    user.setEnabled(true);
    return user;
  }
}
