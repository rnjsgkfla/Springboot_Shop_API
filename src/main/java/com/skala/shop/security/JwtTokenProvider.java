package com.skala.shop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMinutes = expirationMinutes;
    }

    public String createToken(String customerId) {
        Date issuedAt = new Date();
        Date expiration = new Date(
            issuedAt.getTime() + expirationMinutes * 60_000
        );

        return Jwts.builder()
            .subject(customerId)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(secretKey)
            .compact();
    }

    public String getCustomerId(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return claims.getSubject();
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
