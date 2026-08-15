package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.dto.UpdateUserRequest;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.UserRole;
import tn.inovexahub.hardware_store.repository.PasswordResetTokenRepository;
import tn.inovexahub.hardware_store.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;

  @Mock private RefreshTokenService refreshTokenService;

  @Mock private AccessTokenService accessTokenService;

  @InjectMocks private UserService userService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setFirstName("John");
    testUser.setLastName("Doe");
    testUser.setEmail("john@example.com");
    testUser.setPassword("encodedPassword");
    testUser.setRole(UserRole.EMPLOYEE);
    testUser.setEnabled(true);
  }

  @Test
  void updateUserProfile_WithFirstNameOnly_UpdatesFirstName() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setFirstName("Jane");

    when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);

    User result = userService.updateUserProfile(testUser, request);

    assertEquals("Jane", result.getFirstName());
    assertEquals("Doe", result.getLastName());
    assertEquals("john@example.com", result.getEmail());
    verify(userRepository).saveAndFlush(testUser);
  }

  @Test
  void updateUserProfile_WithLastNameOnly_UpdatesLastName() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setLastName("Smith");

    when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);

    User result = userService.updateUserProfile(testUser, request);

    assertEquals("John", result.getFirstName());
    assertEquals("Smith", result.getLastName());
    assertEquals("john@example.com", result.getEmail());
    verify(userRepository).saveAndFlush(testUser);
  }

  @Test
  void updateUserProfile_WithBothNames_UpdatesBoth() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setFirstName("Jane");
    request.setLastName("Smith");

    when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);

    User result = userService.updateUserProfile(testUser, request);

    assertEquals("Jane", result.getFirstName());
    assertEquals("Smith", result.getLastName());
    verify(userRepository).saveAndFlush(testUser);
  }

  @Test
  void updateUserProfile_WithSameEmail_DoesNotRevokeTokens() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("john@example.com");

    when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);

    User result = userService.updateUserProfile(testUser, request);

    assertEquals("john@example.com", result.getEmail());
    verify(passwordResetTokenRepository, never()).revokeAllForUserId(any());
    verify(refreshTokenService, never()).revokeAllForUser(any());
    verify(accessTokenService, never()).revokeAllForUser(any());
  }

  @Test
  void updateUserProfile_WithNewEmail_RevokeTokens() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("new@example.com");

    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);

    User result = userService.updateUserProfile(testUser, request);

    assertEquals("new@example.com", result.getEmail());
    verify(passwordResetTokenRepository).revokeAllForUserId(1L);
    verify(refreshTokenService).revokeAllForUser(1L);
    verify(accessTokenService).revokeAllForUser(1L);
  }

  @Test
  void updateUserProfile_WithNewEmailNormalized_TrimsAndLowercases() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("  NEW@EXAMPLE.COM  ");

    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);

    User result = userService.updateUserProfile(testUser, request);

    assertEquals("new@example.com", result.getEmail());
  }

  @Test
  void updateUserProfile_WithExistingEmail_ThrowsConflict() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("existing@example.com");

    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> userService.updateUserProfile(testUser, request));

    assertEquals(409, ex.getStatusCode().value());
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void updateUserProfile_WithDataIntegrityViolation_ThrowsConflict() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("new@example.com");

    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(testUser))
        .thenThrow(new DataIntegrityViolationException("Duplicate key"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> userService.updateUserProfile(testUser, request));

    assertEquals(409, ex.getStatusCode().value());
  }

  @Test
  void updateUserProfile_WithAllFields_UpdatesAll() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setFirstName("Jane");
    request.setLastName("Smith");
    request.setEmail("new@example.com");

    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);

    User result = userService.updateUserProfile(testUser, request);

    assertEquals("Jane", result.getFirstName());
    assertEquals("Smith", result.getLastName());
    assertEquals("new@example.com", result.getEmail());
    verify(passwordResetTokenRepository).revokeAllForUserId(1L);
    verify(refreshTokenService).revokeAllForUser(1L);
    verify(accessTokenService).revokeAllForUser(1L);
  }

  @Test
  void updateUserProfile_WithNullRequest_DoesNothing() {
    UpdateUserRequest request = new UpdateUserRequest();

    when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);

    User result = userService.updateUserProfile(testUser, request);

    assertEquals("John", result.getFirstName());
    assertEquals("Doe", result.getLastName());
    assertEquals("john@example.com", result.getEmail());
    verify(userRepository).saveAndFlush(testUser);
  }
}
