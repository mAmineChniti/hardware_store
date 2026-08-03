package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class TokenHashUtilTest {

  @Test
  void sha256Hex_knownInput_returnsExpectedDigest() {
    // Well-known SHA-256 digest of "abc" (RFC 6234 / NIST test vector).
    assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        TokenHashUtil.sha256Hex("abc"));
  }

  @Test
  void sha256Hex_whenSha256Unavailable_throwsIllegalStateException() {
    try (MockedStatic<MessageDigest> mocked = mockStatic(MessageDigest.class)) {
      mocked
          .when(() -> MessageDigest.getInstance("SHA-256"))
          .thenThrow(new NoSuchAlgorithmException("SHA-256 not available"));

      assertThrows(IllegalStateException.class, () -> TokenHashUtil.sha256Hex("abc"));
    }
  }
}
