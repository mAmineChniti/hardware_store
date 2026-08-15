package tn.inovexahub.hardware_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
import tn.inovexahub.hardware_store.repository.UserRepository;
import tn.inovexahub.hardware_store.security.JwtUtil;
import tn.inovexahub.hardware_store.security.LoginRateLimiter;
import tn.inovexahub.hardware_store.service.AccessTokenService;
import tn.inovexahub.hardware_store.service.PasswordResetService;
import tn.inovexahub.hardware_store.service.RefreshTokenService;
import tn.inovexahub.hardware_store.service.UserService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final AccessTokenService accessTokenService;
  private final RefreshTokenService refreshTokenService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final LoginRateLimiter loginRateLimiter;
  private final PasswordResetService passwordResetService;
  private final UserService userService;

  public AuthController(
      AuthenticationManager authenticationManager,
      JwtUtil jwtUtil,
      AccessTokenService accessTokenService,
      RefreshTokenService refreshTokenService,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      LoginRateLimiter loginRateLimiter,
      PasswordResetService passwordResetService,
      UserService userService) {
    this.authenticationManager = authenticationManager;
    this.jwtUtil = jwtUtil;
    this.accessTokenService = accessTokenService;
    this.refreshTokenService = refreshTokenService;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.loginRateLimiter = loginRateLimiter;
    this.passwordResetService = passwordResetService;
    this.userService = userService;
  }

  @PostMapping("/login")
  @Operation(
      summary = "Login user",
      description = "Authenticate user credentials and return access and refresh tokens")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(
            responseCode = "429",
            description = "Too many login attempts",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
      })
  public ResponseEntity<LoginResponse> login(
      @RequestBody(description = "User login credentials", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          LoginRequest loginRequest,
      HttpServletRequest request) {
    String clientIp = getClientIp(request);

    if (loginRateLimiter.isBlocked("login:" + clientIp)) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Try again later.");
    }

    try {
      String normalizedEmail = loginRequest.getEmail().toLowerCase(Locale.ROOT).trim();
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(normalizedEmail, loginRequest.getPassword()));

      loginRateLimiter.reset("login:" + clientIp);

      String email = authentication.getName();
      User user =
          userRepository
              .findByEmail(email)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

      String accessToken = accessTokenService.generateAccessToken(user.getId());
      String refreshToken = refreshTokenService.generateRefreshToken(user.getId());

      return ResponseEntity.ok(buildLoginResponse(user, accessToken, refreshToken));
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      loginRateLimiter.recordFailure("login:" + clientIp);
      throw e;
    }
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh access token",
      description =
          "Exchange a valid raw refresh token for a new access token and rotated refresh token."
              + " The presented refresh token is atomically revoked so it cannot be replayed.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token refresh successful",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid/expired/already used/revoked refresh token or disabled account",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
      })
  public ResponseEntity<LoginResponse> refresh(
      @RequestBody(
              description = "Refresh token request containing raw refresh token",
              required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          RefreshTokenRequest refreshTokenRequest) {
    String rawRefreshToken = refreshTokenRequest.getRefreshToken();
    Long userId = refreshTokenService.extractUserId(rawRefreshToken);
    refreshTokenService.validateRefreshTokenForUser(rawRefreshToken, userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (!Boolean.TRUE.equals(user.getEnabled())) {
      // Defense-in-depth: kill the presented token too, so a disabled account's refresh token
      // cannot be replayed even if it is later re-enabled without issuing new tokens.
      refreshTokenService.revoke(rawRefreshToken);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is disabled");
    }

    String accessToken = accessTokenService.generateAccessToken(user.getId());
    String newRefreshToken = refreshTokenService.rotate(rawRefreshToken, userId);

    return ResponseEntity.ok(buildLoginResponse(user, accessToken, newRefreshToken));
  }

  @PostMapping("/logout")
  @Operation(
      summary = "Logout user",
      description = "Atomically revoke the provided raw refresh token so it cannot be reused")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Logout successful", content = @Content),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid/expired refresh token, or already revoked/used",
            content = @Content)
      })
  public ResponseEntity<Void> logout(
      @RequestBody(
              description = "Refresh token request containing raw refresh token to revoke",
              required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          RefreshTokenRequest refreshTokenRequest) {
    refreshTokenService.revoke(refreshTokenRequest.getRefreshToken());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/forgot-password")
  @Operation(
      summary = "Request password reset",
      description = "Send a 6-digit OTP code to the registered email address")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OTP sent successfully",
            content = @Content),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "429", description = "Too many attempts", content = @Content)
      })
  public ResponseEntity<Void> forgotPassword(
      @RequestBody(description = "Email address to receive OTP", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          ForgotPasswordRequest request,
      HttpServletRequest httpRequest) {
    String clientIp = getClientIp(httpRequest);

    if (loginRateLimiter.isBlocked("forgot:" + clientIp)) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again later.");
    }

    try {
      passwordResetService.requestPasswordReset(request.getEmail());
    } catch (IllegalArgumentException e) {
      // Silently ignore to prevent account enumeration
    }
    loginRateLimiter.recordFailure("forgot:" + clientIp);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-password")
  @Operation(
      summary = "Reset password with OTP",
      description = "Validate the OTP and set a new password")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password reset successfully",
            content = @Content),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid/expired OTP or request payload",
            content = @Content),
        @ApiResponse(responseCode = "429", description = "Too many attempts", content = @Content)
      })
  public ResponseEntity<Void> resetPassword(
      @RequestBody(description = "OTP code and new password", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          ResetPasswordRequest request,
      HttpServletRequest httpRequest) {
    String clientIp = getClientIp(httpRequest);

    if (loginRateLimiter.isBlocked("reset:" + clientIp)) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again later.");
    }

    try {
      String normalizedEmail = request.getEmail().toLowerCase(Locale.ROOT).trim();
      passwordResetService.resetPassword(
          normalizedEmail, request.getOtpCode(), request.getNewPassword());
      userRepository
          .findByEmail(normalizedEmail)
          .ifPresent(
              user -> {
                refreshTokenService.revokeAllForUser(user.getId());
                accessTokenService.revokeAllForUser(user.getId());
              });
    } catch (IllegalArgumentException e) {
      loginRateLimiter.recordFailure("reset:" + clientIp);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
    return ResponseEntity.ok().build();
  }

  @PostMapping("/register")
  @Operation(
      summary = "Register new user",
      description = "Create a new user account with default EMPLOYEE role")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Registration successful",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = User.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content)
      })
  public ResponseEntity<User> register(
      @RequestBody(description = "New user registration details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          RegisterRequest registerRequest) {
    String normalizedEmail = registerRequest.getEmail().toLowerCase(Locale.ROOT).trim();
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    User user = new User();
    user.setFirstName(registerRequest.getFirstName());
    user.setLastName(registerRequest.getLastName());
    user.setEmail(normalizedEmail);
    user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    user.setRole(UserRole.EMPLOYEE);
    user.setEnabled(true);

    try {
      User savedUser = userRepository.save(user);
      return ResponseEntity.ok(savedUser);
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }
  }

  @PutMapping("/me")
  @Operation(
      summary = "Update own profile",
      description = "Update the authenticated user's first name, last name and email")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = User.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid update payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Authenticated user not found",
            content = @Content),
        @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content)
      })
  public ResponseEntity<User> updateMe(
      @RequestBody(description = "Profile update details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          UpdateUserRequest updateUserRequest,
      Authentication authentication) {
    User user =
        userRepository
            .findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    User updatedUser = userService.updateUserProfile(user, updateUserRequest);
    return ResponseEntity.ok(updatedUser);
  }

  @PutMapping("/users/{id}/role")
  @Operation(
      summary = "Change user role",
      description = "Change another user's role (EMPLOYEE or ADMIN). Admin only.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role changed successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "400", description = "Invalid role value", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Cannot change own role",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
      })
  public ResponseEntity<User> changeUserRole(
      @Parameter(description = "ID of user whose role to change", example = "2", required = true)
          @PathVariable
          Long id,
      @RequestBody(description = "New role details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          ChangeRoleRequest changeRoleRequest,
      Authentication authentication) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (authentication.getName().equals(user.getEmail())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change your own role");
    }

    user.setRole(UserRole.valueOf(changeRoleRequest.getRole().toUpperCase(Locale.ROOT)));
    User updatedUser = userRepository.save(user);
    return ResponseEntity.ok(updatedUser);
  }

  @DeleteMapping("/users/{id}")
  @Operation(summary = "Delete user", description = "Delete a user account by ID")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "User deleted successfully",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
      })
  public ResponseEntity<Void> deleteUser(
      @Parameter(description = "ID of user to delete", example = "1", required = true) @PathVariable
          Long id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    refreshTokenService.revokeAllForUser(user.getId());
    accessTokenService.revokeAllForUser(user.getId());
    userRepository.delete(user);
    return ResponseEntity.noContent().build();
  }

  private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
    LoginResponse response = new LoginResponse();
    response.setAccessToken(accessToken);
    response.setRefreshToken(refreshToken);
    response.setAccessTokenExpiresIn(jwtUtil.getAccessExpirationMs() / 1000);
    response.setRefreshTokenExpiresIn(jwtUtil.getRefreshExpirationMs() / 1000);
    response.setTokenType("Bearer");
    response.setEmail(user.getEmail());
    response.setFirstName(user.getFirstName());
    response.setLastName(user.getLastName());
    response.setRole(user.getRole().name());
    return response;
  }

  private String getClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
