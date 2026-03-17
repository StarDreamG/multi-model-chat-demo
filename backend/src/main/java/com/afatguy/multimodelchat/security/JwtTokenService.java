package com.afatguy.multimodelchat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(AppUserPrincipal principal) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + jwtProperties.getExpireSeconds() * 1000);

        return Jwts.builder()
            .subject(principal.username())
            .claim("uid", principal.userId())
            .claim("displayName", principal.displayName())
            .claim("roles", principal.roles())
            .issuedAt(now)
            .expiration(expiresAt)
            .signWith(secretKey())
            .compact();
    }

    public OffsetDateTime extractExpireTime(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().toInstant().atOffset(OffsetDateTime.now().getOffset());
    }

    public AppUserPrincipal parsePrincipal(String token) {
        Claims claims = parseClaims(token);
        Long userId = claims.get("uid", Long.class);
        String displayName = claims.get("displayName", String.class);
        List<String> roles = claims.get("roles", List.class);
        return new AppUserPrincipal(userId, claims.getSubject(), displayName, roles);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey secretKey() {
        String rawSecret = jwtProperties.getSecret();
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(rawSecret);
        } catch (Exception ex) {
            keyBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}