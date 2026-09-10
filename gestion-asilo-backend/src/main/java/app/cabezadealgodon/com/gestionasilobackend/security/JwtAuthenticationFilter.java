package app.cabezadealgodon.com.gestionasilobackend.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Autentica cada petición a partir del token del encabezado Authorization. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String cabecera = peticion.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.validar(cabecera.substring(PREFIJO.length()))
                    .ifPresent(claims -> autenticar(claims, peticion));
        }

        cadena.doFilter(peticion, respuesta);
    }

    /**
     * El rol y el estado se releen de la base de datos en vez de confiar en los
     * claims: así, desactivar a alguien, cambiarle el rol o cerrarle la sesión
     * surte efecto de inmediato y no cuando expire su token.
     */
    private void autenticar(Claims claims, HttpServletRequest peticion) {
        try {
            UsuarioAutenticado usuario = usuarioDetailsService.loadUserByUsername(claims.getSubject());
            if (!usuario.isEnabled() || sesionCerrada(claims, usuario.getUsuario())) {
                return;
            }

            UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(peticion));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
        }
    }

    /** YS-68: el token es anterior al último cierre de sesión del usuario. */
    private boolean sesionCerrada(Claims claims, Usuario usuario) {
        LocalDateTime corte = usuario.getFechaInvalidacionSesiones();
        if (corte == null) {
            return false;
        }

        Long emisionMs = claims.get(JwtService.EMISION, Long.class);
        if (emisionMs == null) {
            return true;
        }

        long corteMs = corte.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return emisionMs < corteMs;
    }
}
