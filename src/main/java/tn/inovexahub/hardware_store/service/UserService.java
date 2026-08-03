package tn.inovexahub.hardware_store.service;

import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.dto.UpdateUserRequest;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.repository.PasswordResetTokenRepository;
import tn.inovexahub.hardware_store.repository.UserRepository;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final RefreshTokenService refreshTokenService;
  private final AccessTokenService accessTokenService;

  public UserService(
      UserRepository userRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      RefreshTokenService refreshTokenService,
      AccessTokenService accessTokenService) {
    this.userRepository = userRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.refreshTokenService = refreshTokenService;
    this.accessTokenService = accessTokenService;
  }

  @Transactional
  public User updateUserProfile(User user, UpdateUserRequest updateUserRequest) {
    if (updateUserRequest.getFirstName() != null) {
      user.setFirstName(updateUserRequest.getFirstName());
    }
    if (updateUserRequest.getLastName() != null) {
      user.setLastName(updateUserRequest.getLastName());
    }
    if (updateUserRequest.getEmail() != null) {
      String normalizedEmail = updateUserRequest.getEmail().toLowerCase(Locale.ROOT).trim();
      if (!normalizedEmail.equals(user.getEmail())
          && userRepository.existsByEmail(normalizedEmail)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
      }
      if (!normalizedEmail.equals(user.getEmail())) {
        passwordResetTokenRepository.revokeAllForUserId(user.getId());
        refreshTokenService.revokeAllForUser(user.getId());
        accessTokenService.revokeAllForUser(user.getId());
        user.setEmail(normalizedEmail);
      }
    }

    try {
      return userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }
  }
}
