package jar.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret:}")
    private String secretKey;

    @Value("${jwt.access-expiration-ms:900000}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @PostConstruct
    public void validateSecret() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured. Set JWT_SECRET with at least 32 chars.");
        }
        if (secretKey.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters.");
        }
        if (secretKey.toLowerCase().contains("mysecretkey")) {
            throw new IllegalStateException("JWT secret appears to be default/unsafe. Rotate and configure JWT_SECRET.");
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateToken(String email, String role) {
        return generateAccessToken(email, role, Map.of());
    }

    public String generateToken(String email, String role, Map<String, Object> claims) {
        return generateAccessToken(email, role, claims);
    }

    public String generateAccessToken(String email, String role, Map<String, Object> claims) {
        return buildToken(email, role, "access", accessExpirationMs, claims);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, null, "refresh", refreshExpirationMs, Map.of());
    }

    private String buildToken(String email,
                              String role,
                              String type,
                              long expirationMs,
                              Map<String, Object> claims) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(email)
                .claim("tokenType", type)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256);
        if (role != null && !role.isBlank()) {
            builder.claim("role", role);
        }

        claims.forEach(builder::claim);
        return builder.compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractTokenType(String token) {
        return parseClaims(token).get("tokenType", String.class);
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "access".equals(claims.get("tokenType", String.class))
                    && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "refresh".equals(claims.get("tokenType", String.class))
                    && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        return validateAccessToken(token);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
