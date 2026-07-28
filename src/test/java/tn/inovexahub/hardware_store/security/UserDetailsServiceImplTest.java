package tn.inovexahub.hardware_store.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.UserRole;
import tn.inovexahub.hardware_store.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserDetailsServiceImpl userDetailsServiceImpl;

  @Test
  void loadUserByUsername_found_returnsUserDetails() {
    User user = new User();
    user.setUsername("testuser");
    user.setPassword("hashedpass");
    user.setEnabled(true);
    user.setRole(UserRole.EMPLOYEE);

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

    UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername("testuser");

    assertEquals("testuser", userDetails.getUsername());
    assertEquals("hashedpass", userDetails.getPassword());
    assertTrue(userDetails.isEnabled());
    assertTrue(userDetails.isAccountNonExpired());
    assertTrue(userDetails.isAccountNonLocked());
    assertTrue(userDetails.isCredentialsNonExpired());
    assertEquals(1, userDetails.getAuthorities().size());
    assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

    verify(userRepository).findByUsername("testuser");
  }

  @Test
  void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    UsernameNotFoundException exception =
        assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsServiceImpl.loadUserByUsername("unknown"));

    assertTrue(exception.getMessage().contains("unknown"));
    verify(userRepository).findByUsername("unknown");
  }
}
