package tn.inovexahub.hardware_store.security;

import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tn.inovexahub.hardware_store.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  public UserDetailsServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    tn.inovexahub.hardware_store.entity.User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    return toUserDetails(user);
  }

  public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
    tn.inovexahub.hardware_store.entity.User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
    return toUserDetails(user);
  }

  private UserDetails toUserDetails(tn.inovexahub.hardware_store.entity.User user) {
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
    Collection<? extends GrantedAuthority> authorities = Collections.singletonList(authority);

    return new User(
        user.getEmail(), user.getPassword(), user.getEnabled(), true, true, true, authorities);
  }
}
