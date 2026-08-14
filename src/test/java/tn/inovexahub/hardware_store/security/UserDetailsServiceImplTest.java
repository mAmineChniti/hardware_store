package tn.inovexahub.hardware_store.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    user.setEmail("test@example.com");
    user.setPassword("hashedpass");
    user.setEnabled(true);
    user.setRole(UserRole.EMPLOYEE);

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername("test@example.com");

    assertEquals("test@example.com", userDetails.getUsername());
    assertEquals("hashedpass", userDetails.getPassword());
    assertTrue(userDetails.isEnabled());
    assertTrue(userDetails.isAccountNonExpired());
    assertTrue(userDetails.isAccountNonLocked());
    assertTrue(userDetails.isCredentialsNonExpired());
    assertEquals(1, userDetails.getAuthorities().size());
    assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

    verify(userRepository).findByEmail("test@example.com");
  }

  @Test
  void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
    when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    UsernameNotFoundException exception =
        assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsServiceImpl.loadUserByUsername("unknown@example.com"));

    assertTrue(exception.getMessage().contains("unknown@example.com"));
    verify(userRepository).findByEmail("unknown@example.com");
  }

  @Test
  void loadUserById_found_returnsUserDetails() {
    User user = new User();
    user.setId(42L);
    user.setEmail("test@example.com");
    user.setPassword("hashedpass");
    user.setEnabled(true);
    user.setRole(UserRole.EMPLOYEE);

    when(userRepository.findById(42L)).thenReturn(Optional.of(user));

    UserDetails userDetails = userDetailsServiceImpl.loadUserById(42L);

    assertEquals("test@example.com", userDetails.getUsername());
    assertEquals("hashedpass", userDetails.getPassword());
    assertTrue(userDetails.isEnabled());
    assertEquals(1, userDetails.getAuthorities().size());
    assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

    verify(userRepository).findById(42L);
  }

  @Test
  void loadUserById_notFound_throwsUsernameNotFoundException() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    UsernameNotFoundException exception =
        assertThrows(
            UsernameNotFoundException.class, () -> userDetailsServiceImpl.loadUserById(999L));

    assertTrue(exception.getMessage().contains("999"));
    verify(userRepository).findById(999L);
  }
}
