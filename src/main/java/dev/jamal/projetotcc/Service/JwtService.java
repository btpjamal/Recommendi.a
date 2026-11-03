package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.Entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {
    private static final long EXPIRATION = 1000L * 60 * 15;
    private static final String ISSUER = "recommendia";
    private final SecretKey secretKey;

    public JwtService(@Value("${security.jwt.secret}") String secret) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("JWT_SECRET deve ser Base64 válido.", ex); }
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET deve possuir pelo menos 256 bits.");
        secretKey = Keys.hmacShaKeyFor(bytes);
    }

    public String gerarToken(User user) {
        return Jwts.builder().issuer(ISSUER).subject(user.getId().toString())
                .claim("tokenVersion", user.getTokenVersion())
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(secretKey).compact();
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(secretKey).requireIssuer(ISSUER).build()
                .parseSignedClaims(token).getPayload();
    }

    public Long extrairUserId(String token) {
        return Long.valueOf(claims(token).getSubject());
    }

    public boolean tokenValido(String token, User user) {
        Claims claims = claims(token);
        Object version = claims.get("tokenVersion");
        return user.getId().toString().equals(claims.getSubject())
                && version instanceof Number
                && ((Number) version).longValue() == user.getTokenVersion()
                && claims.getIssuedAt() != null && claims.getExpiration() != null
                && claims.getExpiration().after(new Date())
                && user.isEnabled() && user.isAccountNonLocked();
    }
}
