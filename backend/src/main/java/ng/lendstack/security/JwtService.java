package ng.lendstack.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import ng.lendstack.domain.User;
import ng.lendstack.domain.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${lendstack.security.jwt.secret}")
    private String secret;

    @Value("${lendstack.security.jwt.access-token-ttl-minutes}")
    private long ttlMinutes;

    private SecretKey key;

    @PostConstruct
    void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must be set and at least 32 characters long.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .claim("name", user.getFullName())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttlMinutes, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact();
    }

    /** Parses and verifies the token; throws JwtException if invalid/expired. */
    public UserPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();
        return new UserPrincipal(
            UUID.fromString(claims.getSubject()),
            claims.get("email", String.class),
            claims.get("name", String.class),
            Role.valueOf(claims.get("role", String.class)),
            true);
    }
}
