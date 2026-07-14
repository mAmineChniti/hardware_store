package tn.inovexahub.hardware_store.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

  @Test
  void passwordEncoder_ShouldBeBCrypt() {
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    assertNotNull(passwordEncoder);
    String rawPassword = "testPassword";
    String encodedPassword = passwordEncoder.encode(rawPassword);

    assertNotNull(encodedPassword);
    assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
  }
}
