package tn.inovexahub.hardware_store.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.dto.ChangeRoleRequest;
import tn.inovexahub.hardware_store.dto.ForgotPasswordRequest;
import tn.inovexahub.hardware_store.dto.LoginRequest;
import tn.inovexahub.hardware_store.dto.LoginResponse;
import tn.inovexahub.hardware_store.dto.RefreshTokenRequest;
import tn.inovexahub.hardware_store.dto.RegisterRequest;
import tn.inovexahub.hardware_store.dto.ResetPasswordRequest;
import tn.inovexahub.hardware_store.dto.UpdateUserRequest;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.UserRole;
import tn.inovexahub.hardware_store.repository.PasswordResetTokenRepository;
import tn.inovexahub.hardware_store.repository.UserRepository;
import tn.inovexahub.hardware_store.security.JwtUtil;
import tn.inovexahub.hardware_store.security.LoginRateLimiter;
import tn.inovexahub.hardware_store.service.AccessTokenService;
import tn.inovexahub.hardware_store.service.PasswordResetService;
import tn.inovexahub.hardware_store.service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtUtil jwtUtil;
  @Mock private AccessTokenService accessTokenService;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private UserRepository userRepository;
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
            accessTokenService,
            refreshTokenService,
            userRepository,
            passwordEncoder,
            loginRateLimiter,
            passwordResetService,
            passwordResetTokenRepository);
  }

  private void stubRateLimiter() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
  }

  private org.springframework.security.core.Authentication mockAuthentication(String username) {
    org.springframework.security.core.Authentication auth =
        org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
    when(auth.getName()).thenReturn(username);
    return auth;
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

  private void stubLoginSuccess(User user) {
    stubRateLimiter();
    when(loginRateLimiter.isBlocked("login:127.0.0.1")).thenReturn(false);

    Authentication auth = org.mockito.Mockito.mock(Authentication.class);
    when(auth.getName()).thenReturn("test@example.com");
    when(authenticationManager.authenticate(any())).thenReturn(auth);

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(accessTokenService.generateAccessToken(1L)).thenReturn("access-token");
    when(refreshTokenService.generateRefreshToken(1L)).thenReturn("refresh-token");
    when(jwtUtil.getAccessExpirationMs()).thenReturn(3600000L);
    when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);
  }

  private LoginRequest buildLoginRequest() {
    LoginRequest req = new LoginRequest();
    req.setEmail("test@example.com");
    req.setPassword("password");
    return req;
  }

  // --- login ---

  @Test
  void login_Success_ReturnsLoginResponse() {
    User user = createUser("test@example.com");
    stubLoginSuccess(user);

    ResponseEntity<LoginResponse> response = authController.login(buildLoginRequest(), httpRequest);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    LoginResponse body = response.getBody();
    assertNotNull(body);
    assertEquals("access-token", body.getAccessToken());
    assertEquals("refresh-token", body.getRefreshToken());
    assertEquals("Bearer", body.getTokenType());
    assertEquals("test@example.com", body.getEmail());
    assertEquals("Test", body.getFirstName());
    assertEquals("User", body.getLastName());
    assertEquals("EMPLOYEE", body.getRole());
    assertEquals(3600L, body.getAccessTokenExpiresIn());
    assertEquals(604800L, body.getRefreshTokenExpiresIn());
    verify(loginRateLimiter).reset("login:127.0.0.1");
  }

  @Test
  void login_NormalizesEmailBeforeAuthentication() {
    User user = createUser("test@example.com");
    stubLoginSuccess(user);

    LoginRequest req = new LoginRequest();
    req.setEmail("  TEST@EXAMPLE.COM  ");
    req.setPassword("password");

    ResponseEntity<LoginResponse> response = authController.login(req, httpRequest);

    ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
        ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
    verify(authenticationManager).authenticate(captor.capture());
    assertEquals("test@example.com", captor.getValue().getPrincipal());
    assertEquals("test@example.com", response.getBody().getEmail());
  }

  @Test
  void login_RateLimited_ReturnsTooManyRequests() {
    stubRateLimiter();
    when(loginRateLimiter.isBlocked("login:127.0.0.1")).thenReturn(true);

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.login(buildLoginRequest(), httpRequest));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    verify(authenticationManager, never()).authenticate(any());
  }

  @Test
  void login_BadCredentials_RecordsFailureAndThrows() {
    stubRateLimiter();
    when(loginRateLimiter.isBlocked("login:127.0.0.1")).thenReturn(false);
    when(authenticationManager.authenticate(any()))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    BadCredentialsException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            BadCredentialsException.class,
            () -> authController.login(buildLoginRequest(), httpRequest));

    assertEquals("Bad credentials", ex.getMessage());
    verify(loginRateLimiter).recordFailure("login:127.0.0.1");
  }

  @Test
  void login_UserNotFound_DoesNotRecordFailureAndThrows() {
    stubRateLimiter();
    when(loginRateLimiter.isBlocked("login:127.0.0.1")).thenReturn(false);

    Authentication auth = org.mockito.Mockito.mock(Authentication.class);
    when(auth.getName()).thenReturn("test@example.com");
    when(authenticationManager.authenticate(any())).thenReturn(auth);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.login(buildLoginRequest(), httpRequest));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    verify(loginRateLimiter, never()).recordFailure("login:127.0.0.1");
  }

  // --- refresh ---

  @Test
  void refresh_Success_ReturnsNewTokens() {
    RefreshTokenRequest refreshReq = new RefreshTokenRequest();
    refreshReq.setRefreshToken("old-refresh-token");

    when(refreshTokenService.extractUserId("old-refresh-token")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(createUser("test@example.com")));
    when(accessTokenService.generateAccessToken(1L)).thenReturn("new-access");
    when(refreshTokenService.rotate("old-refresh-token", 1L)).thenReturn("new-refresh");
    when(jwtUtil.getAccessExpirationMs()).thenReturn(3600000L);
    when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);

    ResponseEntity<LoginResponse> response = authController.refresh(refreshReq);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    LoginResponse body = response.getBody();
    assertNotNull(body);
    assertEquals("new-access", body.getAccessToken());
    assertEquals("new-refresh", body.getRefreshToken());
    assertEquals("Bearer", body.getTokenType());
    assertEquals("test@example.com", body.getEmail());
    assertEquals("EMPLOYEE", body.getRole());
  }

  @Test
  void refresh_DisabledUser_ReturnsUnauthorized() {
    RefreshTokenRequest refreshReq = new RefreshTokenRequest();
    refreshReq.setRefreshToken("old-refresh-token");

    when(refreshTokenService.extractUserId("old-refresh-token")).thenReturn(1L);

    User disabledUser = createUser("test@example.com");
    disabledUser.setEnabled(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(disabledUser));

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> authController.refresh(refreshReq));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    verify(refreshTokenService).revoke("old-refresh-token");
  }

  @Test
  void refresh_UserNotFound_ReturnsNotFound() {
    RefreshTokenRequest refreshReq = new RefreshTokenRequest();
    refreshReq.setRefreshToken("old-refresh-token");

    when(refreshTokenService.extractUserId("old-refresh-token")).thenReturn(999L);
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> authController.refresh(refreshReq));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // --- deleteUser ---

  @Test
  void deleteUser_ExistingUser_ReturnsNoContent() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(createUser("test@example.com")));

    ResponseEntity<Void> response = authController.deleteUser(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(refreshTokenService).revokeAllForUser(1L);
    verify(accessTokenService).revokeAllForUser(1L);
    verify(userRepository).delete(any(User.class));
  }

  @Test
  void deleteUser_NonExistingUser_ReturnsNotFound() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> authController.deleteUser(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    verify(refreshTokenService, never()).revokeAllForUser(anyLong());
    verify(accessTokenService, never()).revokeAllForUser(anyLong());
    verify(userRepository, never()).delete(any(User.class));
  }

  // --- updateMe ---

  @Test
  void updateMe_WithFirstNameAndLastName_ReturnsOk() {
    User existing = createUser("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest();
    request.setFirstName("Updated");
    request.setLastName("Name");

    ResponseEntity<User> response =
        authController.updateMe(request, mockAuthentication("test@example.com"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Updated", existing.getFirstName());
    assertEquals("Name", existing.getLastName());
    verify(userRepository).save(existing);
  }

  @Test
  void updateMe_WithNewEmail_RevokesPasswordResetAndRefreshTokens() {
    User existing = createUser("old@example.com");
    when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(existing));
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("new@example.com");

    ResponseEntity<User> response =
        authController.updateMe(request, mockAuthentication("old@example.com"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("new@example.com", existing.getEmail());
    verify(passwordResetTokenRepository).revokeAllForUserId(1L);
    verify(refreshTokenService).revokeAllForUser(1L);
    verify(accessTokenService).revokeAllForUser(1L);
  }

  @Test
  void updateMe_NewEmail_NormalizesBeforeConflictCheckAndSave() {
    User existing = createUser("old@example.com");
    when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(existing));
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("  NEW@EXAMPLE.COM  ");

    ResponseEntity<User> response =
        authController.updateMe(request, mockAuthentication("old@example.com"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("new@example.com", existing.getEmail());
    verify(userRepository).existsByEmail("new@example.com");
    verify(refreshTokenService).revokeAllForUser(1L);
    verify(accessTokenService).revokeAllForUser(1L);
  }

  @Test
  void updateMe_SameEmail_ReturnsOk() {
    User existing = createUser("same@example.com");
    when(userRepository.findByEmail("same@example.com")).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("same@example.com");

    ResponseEntity<User> response =
        authController.updateMe(request, mockAuthentication("same@example.com"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(passwordResetTokenRepository, never()).revokeAllForUserId(1L);
    verify(refreshTokenService, never()).revokeAllForUser(anyLong());
    verify(accessTokenService, never()).revokeAllForUser(anyLong());
  }

  @Test
  void updateMe_DuplicateEmail_ReturnsConflict() {
    User existing = createUser("old@example.com");
    when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(existing));
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("taken@example.com");

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.updateMe(request, mockAuthentication("old@example.com")));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void updateMe_UserNotFound_ReturnsNotFound() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    UpdateUserRequest request = new UpdateUserRequest();
    request.setFirstName("Updated");

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.updateMe(request, mockAuthentication("ghost@example.com")));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // --- changeUserRole ---

  @Test
  void changeUserRole_AdminChangesOtherUser_ReturnsOk() {
    User existing = createUser("test@example.com");
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    ChangeRoleRequest req = new ChangeRoleRequest();
    req.setRole("ADMIN");

    ResponseEntity<User> response =
        authController.changeUserRole(1L, req, mockAuthentication("admin@example.com"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(UserRole.ADMIN, existing.getRole());
    verify(userRepository).save(existing);
  }

  @Test
  void changeUserRole_AdminDemotesOtherUserToEmployee_ReturnsOk() {
    User existing = createUser("test@example.com");
    existing.setRole(UserRole.ADMIN);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    ChangeRoleRequest req = new ChangeRoleRequest();
    req.setRole("EMPLOYEE");

    ResponseEntity<User> response =
        authController.changeUserRole(1L, req, mockAuthentication("admin@example.com"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(UserRole.EMPLOYEE, existing.getRole());
    verify(userRepository).save(existing);
  }

  @Test
  void changeUserRole_SelfRoleChange_ReturnsForbidden() {
    User existing = createUser("test@example.com");
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

    ChangeRoleRequest req = new ChangeRoleRequest();
    req.setRole("ADMIN");

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.changeUserRole(1L, req, mockAuthentication("test@example.com")));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void changeUserRole_InvalidRole_ThrowsIllegalArgumentException() {
    User existing = createUser("test@example.com");
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

    ChangeRoleRequest req = new ChangeRoleRequest();
    req.setRole("SUPERUSER");

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> authController.changeUserRole(1L, req, mockAuthentication("admin@example.com")));

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void changeUserRole_NonExistingUser_ReturnsNotFound() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    ChangeRoleRequest req = new ChangeRoleRequest();
    req.setRole("ADMIN");

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () ->
                authController.changeUserRole(
                    999L,
                    req,
                    org.mockito.Mockito.mock(
                        org.springframework.security.core.Authentication.class)));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // --- getClientIp (tested indirectly through login) ---

  @Test
  void getClientIp_WithXForwardedFor_UsesFirstIp() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
    when(loginRateLimiter.isBlocked("login:10.0.0.1")).thenReturn(true);

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.login(buildLoginRequest(), httpRequest));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    verify(loginRateLimiter).isBlocked("login:10.0.0.1");
  }

  @Test
  void getClientIp_WithBlankForwardedFor_FallsBackToRemoteAddr() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("  ");
    when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.100");
    when(loginRateLimiter.isBlocked("login:192.168.1.100")).thenReturn(true);

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> authController.login(buildLoginRequest(), httpRequest));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    verify(loginRateLimiter).isBlocked("login:192.168.1.100");
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
    verify(refreshTokenService).revokeAllForUser(1L);
    verify(accessTokenService).revokeAllForUser(1L);
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
    request.setFirstName("New");
    request.setLastName("User");
    request.setEmail("new@example.com");
    request.setPassword("password123");

    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

    User savedUser = createUser("new@example.com");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    ResponseEntity<User> response = authController.register(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void register_DuplicateEmail_ReturnsConflict() {
    RegisterRequest request = new RegisterRequest();
    request.setFirstName("New");
    request.setLastName("User");
    request.setEmail("existing@example.com");
    request.setPassword("password123");

    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> authController.register(request));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verify(userRepository, never()).save(any());
  }
}
