package com.dgranda.nominaec.security;

import com.dgranda.nominaec.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Issues and validates short-lived HS256 tokens signed with JWT_SECRET (>= 256 bits). */
@Service
public class JwtService {

    private final SecretKey key;
    private final AppProperties.Jwt config;
    private final Clock clock;

    public JwtService(AppProperties properties, Clock clock) {
        this.config = properties.jwt();
        this.key = Keys.hmacShaKeyFor(config.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String issue(String username, String role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(config.issuer())
                .subject(username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long expirationSeconds() {
        return config.expirationMinutes() * 60L;
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(config.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
