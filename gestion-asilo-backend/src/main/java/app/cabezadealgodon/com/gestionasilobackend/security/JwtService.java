package app.cabezadealgodon.com.gestionasilobackend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Firma y valida los tokens de sesión (HS256). Ver docs/seguridad.md. */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${jwt.secret}") String secreto, @Value("${jwt.expiration}") long expiracionMs) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public static final String EMISION = "emision";

    /**
     * El claim estándar iat solo guarda segundos, lo que vuelve ambiguo cerrar
     * sesión y volver a entrar dentro del mismo segundo. Por eso el token lleva
     * además la emisión en milisegundos, que es la que compara el cierre de
     * sesión (YS-68).
     */
    public String generarToken(String correo, String rol) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(correo)
                .claim("rol", rol)
                .claim(EMISION, ahora.getTime())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    /**
     * Devuelve los claims solo si la firma es válida y el token no ha expirado.
     * Un token manipulado o vencido se trata igual que no traer token.
     */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
