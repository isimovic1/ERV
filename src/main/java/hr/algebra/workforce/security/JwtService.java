package hr.algebra.workforce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private static final int MINIMUM_SECRET_LENGTH = 32;

    @Value("${app.jwt.secret:}")
    private String configuredSecret;

    @Value("${app.jwt.validity-minutes:60}")
    private long validityMinutes;

    private SecretKey signingKey;

    @PostConstruct
    void initialiseKey() {
        if (configuredSecret == null || configuredSecret.length() < MINIMUM_SECRET_LENGTH) {
            signingKey = Jwts.SIG.HS256.key().build();
            log.warn("JWT_SECRET nije postavljen ili je kraći od {} znakova. Generiran je privremeni ključ, "
                    + "pa tokeni neće vrijediti nakon ponovnog pokretanja.", MINIMUM_SECRET_LENGTH);
            return;
        }
        signingKey = Keys.hmacShaKeyFor(configuredSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AppUserDetails user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + Duration.ofMinutes(validityMinutes).toMillis()))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getValidityMinutes() {
        return validityMinutes;
    }
}
