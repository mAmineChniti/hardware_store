package tn.inovexahub.hardware_store.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.controller.AuthController;
import tn.inovexahub.hardware_store.dto.RefreshTokenRequest;
import tn.inovexahub.hardware_store.dto.UpdateUserRequest;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.UserRole;
import tn.inovexahub.hardware_store.repository.UserRepository;
import tn.inovexahub.hardware_store.service.AccessTokenService;
import tn.inovexahub.hardware_store.service.RefreshTokenService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthEmailChangeRevocationIntegrationTest {

  @PersistenceContext private EntityManager entityManager;

  @Autowired private AuthController authController;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenService refreshTokenService;
  @Autowired private AccessTokenService accessTokenService;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void emailChange_rejectsUnexpiredRefreshTokenIssuedBeforeChange() {
    User user = new User();
    user.setFirstName("Jane");
    user.setLastName("Doe");
    user.setEmail("old@example.com");
    user.setPassword(passwordEncoder.encode("password"));
    user.setRole(UserRole.EMPLOYEE);
    user.setEnabled(true);
    user = userRepository.saveAndFlush(user);

    String unexpiredRefreshToken = refreshTokenService.generateRefreshToken(user.getId());

    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("old@example.com");

    UpdateUserRequest update = new UpdateUserRequest();
    update.setEmail("new@example.com");
    authController.updateMe(update, authentication);

    RefreshTokenRequest refreshReq = new RefreshTokenRequest();
    refreshReq.setRefreshToken(unexpiredRefreshToken);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> authController.refresh(refreshReq),
            "An unexpired refresh token issued before the email change must be rejected");

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertEquals("new@example.com", userRepository.findById(user.getId()).orElseThrow().getEmail());
  }

  @Test
  void emailChange_revokesUnexpiredAccessTokenIssuedBeforeChange() {
    User user = new User();
    user.setFirstName("John");
    user.setLastName("Smith");
    user.setEmail("old@example.com");
    user.setPassword(passwordEncoder.encode("password"));
    user.setRole(UserRole.EMPLOYEE);
    user.setEnabled(true);
    user = userRepository.saveAndFlush(user);

    String unexpiredAccessToken = accessTokenService.generateAccessToken(user.getId());
    assertTrue(accessTokenService.isActive(unexpiredAccessToken, user.getId()));

    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("old@example.com");

    UpdateUserRequest update = new UpdateUserRequest();
    update.setEmail("new@example.com");
    authController.updateMe(update, authentication);

    // The bulk revocation UPDATE does not refresh entities already loaded in this test's
    // persistence context, so detach them before re-reading the token's DB state.
    entityManager.flush();
    entityManager.clear();

    assertFalse(
        accessTokenService.isActive(unexpiredAccessToken, user.getId()),
        "An unexpired access token issued before the email change must be revoked");
  }
}
