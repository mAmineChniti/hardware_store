package tn.inovexahub.hardware_store.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  public static final String TOKEN_TYPE_CLAIM = "type";
  public static final String ACCESS_TOKEN_TYPE = "access";
  public static final String REFRESH_TOKEN_TYPE = "refresh";

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.access-expiration}")
  private Long accessExpiration;

  @Value("${jwt.refresh-expiration}")
  private Long refreshExpiration;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + accessExpiration);

    return Jwts.builder()
        .subject(username)
        .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  public String generateRefreshToken(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + refreshExpiration);

    return Jwts.builder()
        .subject(username)
        .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  public long getAccessExpirationMs() {
    return accessExpiration;
  }

  public long getRefreshExpirationMs() {
    return refreshExpiration;
  }

  public String extractUsername(String token) {
    return getClaims(token).getSubject();
  }

  public boolean validateAccessToken(String token) {
    try {
      Claims claims = getClaims(token);
      if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
        return false;
      }
      return !claims.getExpiration().before(new Date());
    } catch (Exception e) {
      return false;
    }
  }

  public boolean validateRefreshToken(String token) {
    try {
      Claims claims = getClaims(token);
      if (!REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
        return false;
      }
      return !claims.getExpiration().before(new Date());
    } catch (Exception e) {
      return false;
    }
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }
}
