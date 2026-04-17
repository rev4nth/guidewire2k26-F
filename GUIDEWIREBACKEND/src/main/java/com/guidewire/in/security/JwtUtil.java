package com.guidewire.in.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
public class JwtUtil {

	private static final String SECRET_STRING = "safeflex_secret";
	private static final long EXPIRATION_MS = 24L * 60 * 60 * 1000;

	private final SecretKey signingKey;

	public JwtUtil() {
		this.signingKey = Keys.hmacShaKeyFor(sha256(SECRET_STRING));
	}

	private static byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public String generateToken(Long userId, String role) {
		Date now = new Date();
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("role", role)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + EXPIRATION_MS))
				.signWith(signingKey)
				.compact();
	}

	public Long extractUserId(String token) {
		return Long.parseLong(parseClaims(token).getSubject());
	}

	public String extractRole(String token) {
		return parseClaims(token).get("role", String.class);
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
