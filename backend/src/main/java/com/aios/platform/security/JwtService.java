package com.aios.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    private SecretKey signingKey() {
        byte[] raw = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            byte[] padded = new byte[32];
            for (int i = 0; i < 32; i++) {
                padded[i] = raw[i % raw.length];
            }
            raw = padded;
        }
        return Keys.hmacShaKeyFor(raw);
    }

    public String createAccessToken(Long userId, String username) {
        long expMs = props.getAccessTokenMinutes() * 60_000;
        return buildToken(userId, username, "access", expMs);
    }

    public String createRefreshToken(Long userId, String username) {
        long expMs = props.getRefreshTokenDays() * 24 * 60 * 60_000;
        return buildToken(userId, username, "refresh", expMs);
    }

    private String buildToken(Long userId, String username, String typ, long ttlMs) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("typ", typ)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        Object typ = claims.get("typ");
        return "refresh".equals(typ);
    }
}
