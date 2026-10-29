package app.cabezadealgodon.com.gestionasilobackend.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;

import app.cabezadealgodon.com.gestionasilobackend.dto.ErrorResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.BaseDeDatosNoDisponible;
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

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioDetailsService usuarioDetailsService,
            ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String cabecera = peticion.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                jwtService.validar(cabecera.substring(PREFIJO.length()))
                        .ifPresent(claims -> autenticar(claims, peticion));
            } catch (RuntimeException falla) {
                if (!BaseDeDatosNoDisponible.esLaCausa(falla)) {
                    throw falla;
                }
                // DEF-03. Este filtro corre antes de Spring MVC: sin esto, la falla
                // llegaba al reenvío a /error y salía como 500 genérico.
                log.warn("Base de datos no disponible al validar la sesión en {} {}", peticion.getMethod(),
                        peticion.getRequestURI());
                respuesta.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                respuesta.setHeader(HttpHeaders.RETRY_AFTER, "60");
                respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
                respuesta.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(respuesta.getWriter(), ErrorResponse.de(HttpStatus.SERVICE_UNAVAILABLE,
                        BaseDeDatosNoDisponible.MENSAJE, peticion.getRequestURI()));
                return;
            }
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
