package tn.inovexahub.hardware_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.dto.LoginRequest;
import tn.inovexahub.hardware_store.dto.LoginResponse;
import tn.inovexahub.hardware_store.dto.RefreshTokenRequest;
import tn.inovexahub.hardware_store.dto.RegisterRequest;
import tn.inovexahub.hardware_store.dto.UpdateUserRequest;
import tn.inovexahub.hardware_store.entity.RefreshToken;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.UserRole;
import tn.inovexahub.hardware_store.repository.UserRepository;
import tn.inovexahub.hardware_store.security.JwtUtil;
import tn.inovexahub.hardware_store.service.RefreshTokenService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final RefreshTokenService refreshTokenService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthController(
      AuthenticationManager authenticationManager,
      JwtUtil jwtUtil,
      RefreshTokenService refreshTokenService,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    this.authenticationManager = authenticationManager;
    this.jwtUtil = jwtUtil;
    this.refreshTokenService = refreshTokenService;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @PostMapping("/login")
  @Operation(
      summary = "Login user",
      description = "Authenticate user and return access and refresh tokens")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
      })
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), loginRequest.getPassword()));

    String username = authentication.getName();
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    String accessToken = jwtUtil.generateAccessToken(username);
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

    return ResponseEntity.ok(buildLoginResponse(user, accessToken, refreshToken));
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh access token",
      description = "Exchange a valid refresh token for a new access token and refresh token")
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
            responseCode = "401",
            description = "Invalid/expired/revoked refresh token or disabled account",
            content = @Content)
      })
  public ResponseEntity<LoginResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
    RefreshToken rotatedRefreshToken =
        refreshTokenService.atomicRefresh(refreshTokenRequest.getRefreshToken());
    User user = rotatedRefreshToken.getUser();

    if (!Boolean.TRUE.equals(user.getEnabled())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is disabled");
    }

    String accessToken = jwtUtil.generateAccessToken(user.getUsername());

    return ResponseEntity.ok(buildLoginResponse(user, accessToken, rotatedRefreshToken));
  }

  @PostMapping("/logout")
  @Operation(summary = "Logout user", description = "Revoke the provided refresh token")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Logout successful", content = @Content)
      })
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
    refreshTokenService.revokeRefreshToken(refreshTokenRequest.getRefreshToken());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/register")
  @Operation(summary = "Register new user", description = "Create a new user account")
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
            responseCode = "409",
            description = "Username already exists",
            content = @Content)
      })
  public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest registerRequest) {
    if (userRepository.existsByUsername(registerRequest.getUsername())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
    }

    User user = new User();
    user.setUsername(registerRequest.getUsername());
    user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    user.setFullName(registerRequest.getFullName());
    user.setRole(UserRole.EMPLOYEE);
    user.setEnabled(true);

    User savedUser = userRepository.save(user);
    return ResponseEntity.ok(savedUser);
  }

  @PutMapping("/users/{id}")
  @Operation(summary = "Update user", description = "Update user information")
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
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
      })
  public ResponseEntity<User> updateUser(
      @PathVariable Long id, @Valid @RequestBody UpdateUserRequest updateUserRequest) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (updateUserRequest.getFullName() != null) {
      user.setFullName(updateUserRequest.getFullName());
    }
    if (updateUserRequest.getRole() != null) {
      user.setRole(UserRole.valueOf(updateUserRequest.getRole().toUpperCase()));
    }

    User updatedUser = userRepository.save(user);
    return ResponseEntity.ok(updatedUser);
  }

  @DeleteMapping("/users/{id}")
  @Operation(summary = "Delete user", description = "Delete a user account")
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
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    userRepository.delete(user);
    return ResponseEntity.noContent().build();
  }

  private LoginResponse buildLoginResponse(
      User user, String accessToken, RefreshToken refreshToken) {
    long refreshTokenExpiresIn =
        Duration.between(LocalDateTime.now(), refreshToken.getExpiresAt()).getSeconds();

    LoginResponse response = new LoginResponse();
    response.setAccessToken(accessToken);
    response.setRefreshToken(refreshToken.getToken());
    response.setAccessTokenExpiresIn(jwtUtil.getAccessExpirationMs() / 1000);
    response.setRefreshTokenExpiresIn(Math.max(refreshTokenExpiresIn, 0));
    response.setTokenType("Bearer");
    response.setUsername(user.getUsername());
    response.setRole(user.getRole().name());
    return response;
  }
}
