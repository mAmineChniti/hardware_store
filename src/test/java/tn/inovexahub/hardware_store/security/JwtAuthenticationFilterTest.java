package tn.inovexahub.hardware_store.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtUtil jwtUtil;
  @Mock private UserDetailsService userDetailsService;
  @Mock private FilterChain filterChain;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilter_NoAuthorizationHeader_DoesNotAuthenticate() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_NonBearerHeader_DoesNotAuthenticate() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Basic abcdef");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_InvalidAccessToken_DoesNotAuthenticate() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid.token.here");
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(jwtUtil.validateAccessTokenAndGetUsername("invalid.token.here")).thenReturn(null);

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_ValidTokenEnabledUser_SetsAuthentication() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid.token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    UserDetails userDetails =
        new User("testuser", "password", true, true, true, true, Collections.emptyList());
    when(jwtUtil.validateAccessTokenAndGetUsername("valid.token")).thenReturn("testuser");
    when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

    filter.doFilter(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertEquals(userDetails, authentication.getPrincipal());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_ValidTokenDisabledUser_DoesNotAuthenticate() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid.token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    UserDetails userDetails =
        new User("testuser", "password", false, true, true, true, Collections.emptyList());
    when(jwtUtil.validateAccessTokenAndGetUsername("valid.token")).thenReturn("testuser");
    when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_UserNoLongerExists_DoesNotAuthenticateAndDoesNotThrow() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid.token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(jwtUtil.validateAccessTokenAndGetUsername("valid.token")).thenReturn("ghost");
    when(userDetailsService.loadUserByUsername("ghost"))
        .thenThrow(new UsernameNotFoundException("User not found: ghost"));

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}
