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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
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
import tn.inovexahub.hardware_store.service.PasswordResetService;
import tn.inovexahub.hardware_store.service.RefreshTokenService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final RefreshTokenService refreshTokenService;
  private final UserRepository userRepository;
  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final LoginRateLimiter loginRateLimiter;
  private final PasswordResetService passwordResetService;
  private final PasswordResetTokenRepository passwordResetTokenRepository;

  public AuthController(
      AuthenticationManager authenticationManager,
      JwtUtil jwtUtil,
      RefreshTokenService refreshTokenService,
      UserRepository userRepository,
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder,
      LoginRateLimiter loginRateLimiter,
      PasswordResetService passwordResetService,
      PasswordResetTokenRepository passwordResetTokenRepository) {
    this.authenticationManager = authenticationManager;
    this.jwtUtil = jwtUtil;
    this.refreshTokenService = refreshTokenService;
    this.userRepository = userRepository;
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
    this.loginRateLimiter = loginRateLimiter;
    this.passwordResetService = passwordResetService;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
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
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginRequest.getUsername(), loginRequest.getPassword()));

      loginRateLimiter.reset("login:" + clientIp);

      String username = authentication.getName();
      User user =
          userRepository
              .findByUsername(username)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

      String accessToken = jwtUtil.generateAccessToken(username);
      String refreshToken = refreshTokenService.generateRefreshToken(username);

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
    String username = refreshTokenService.extractUsername(rawRefreshToken);
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    refreshTokenService.validateRefreshTokenForUser(rawRefreshToken, userDetails);

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (!Boolean.TRUE.equals(user.getEnabled())) {
      // Defense-in-depth: kill the presented token too, so a disabled account's refresh token
      // cannot be replayed even if it is later re-enabled without issuing new tokens.
      refreshTokenService.revoke(rawRefreshToken);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is disabled");
    }

    String accessToken = jwtUtil.generateAccessToken(user.getUsername());
    String newRefreshToken = refreshTokenService.rotate(rawRefreshToken, username);

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
      String normalizedEmail = request.getEmail().toLowerCase().trim();
      passwordResetService.resetPassword(
          normalizedEmail, request.getOtpCode(), request.getNewPassword());
      userRepository
          .findByEmail(normalizedEmail)
          .ifPresent(user -> refreshTokenService.revokeAllForUser(user.getUsername()));
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
        @ApiResponse(
            responseCode = "409",
            description = "Username already exists",
            content = @Content)
      })
  public ResponseEntity<User> register(
      @RequestBody(description = "New user registration details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          RegisterRequest registerRequest) {
    if (userRepository.existsByUsername(registerRequest.getUsername())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
    }

    String normalizedEmail = registerRequest.getEmail().toLowerCase().trim();
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    User user = new User();
    user.setUsername(registerRequest.getUsername());
    user.setEmail(normalizedEmail);
    user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    user.setFullName(registerRequest.getFullName());
    user.setRole(UserRole.EMPLOYEE);
    user.setEnabled(true);

    User savedUser = userRepository.save(user);
    return ResponseEntity.ok(savedUser);
  }

  @PutMapping("/users/{id}")
  @Operation(summary = "Update user", description = "Update user information (full name, role)")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = User.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid user update payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
      })
  public ResponseEntity<User> updateUser(
      @Parameter(description = "ID of user to update", example = "1", required = true) @PathVariable
          Long id,
      @RequestBody(description = "User update details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          UpdateUserRequest updateUserRequest) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (updateUserRequest.getFullName() != null) {
      user.setFullName(updateUserRequest.getFullName());
    }
    if (updateUserRequest.getEmail() != null) {
      String normalizedEmail = updateUserRequest.getEmail().toLowerCase().trim();
      if (!normalizedEmail.equals(user.getEmail())
          && userRepository.existsByEmail(normalizedEmail)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
      }
      if (!normalizedEmail.equals(user.getEmail())) {
        passwordResetTokenRepository.revokeAllForUserId(user.getId());
      }
      user.setEmail(normalizedEmail);
    }
    if (updateUserRequest.getRole() != null) {
      user.setRole(UserRole.valueOf(updateUserRequest.getRole().toUpperCase()));
    }

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

    refreshTokenService.revokeAllForUser(user.getUsername());
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
    response.setUsername(user.getUsername());
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
