package app.cabezadealgodon.com.gestionasilobackend.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;

import app.cabezadealgodon.com.gestionasilobackend.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * YS-74. Cierra la API a quien todavía usa una contraseña que eligió otra
 * persona, dejando pasar solo lo que necesita para salir de esa situación.
 *
 * <p>
 * La regla vive en el backend y no en la pantalla por lo mismo que en HU-31: si
 * solo la aplicara el frontend, bastaría con escribir otra dirección para
 * seguir trabajando con una credencial que el administrador conoce, que es
 * exactamente lo que esta historia viene a impedir.
 *
 * <p>
 * <b>No se anota como {@code @Component} a propósito.</b> Spring Boot registra
 * automáticamente todo bean de tipo {@code Filter} en el contenedor de
 * servlets, de modo que el filtro correría dos veces: una fuera de la cadena de
 * seguridad y otra dentro. Como {@link OncePerRequestFilter} salta la segunda,
 * la única que se ejecutaría sería la de afuera, donde el usuario todavía no
 * está autenticado y la comprobación siempre daría "no hay nada que bloquear".
 * El bloqueo quedaría desactivado sin que nada lo advirtiera. Por eso lo
 * construye {@code SecurityConfig} directamente.
 */
public class CambioPasswordObligatorioFilter extends OncePerRequestFilter {

    /** Lo lee el frontend para llevar al usuario a la pantalla de cambio. */
    public static final String CODIGO = "CAMBIO_PASSWORD_REQUERIDO";

    private static final String MENSAJE = "Debe cambiar su contraseña antes de usar el sistema: "
            + "la que está usando se la asignó otra persona";

    /**
     * Cambiar la contraseña es la salida del callejón, y sin ella el usuario
     * quedaría encerrado. Cerrar sesión se permite para que pueda irse y
     * volver después; el login no hace falta listarlo porque es público y
     * llega aquí sin usuario autenticado.
     */
    private static final Set<String> RUTAS_PERMITIDAS = Set.of(
            "/api/auth/password",
            "/api/auth/logout");

    private final ObjectMapper objectMapper;

    public CambioPasswordObligatorioFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        if (!debeBloquearse(peticion)) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        respuesta.setStatus(HttpStatus.FORBIDDEN.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");

        ErrorResponse cuerpo = ErrorResponse.de(HttpStatus.FORBIDDEN, MENSAJE, peticion.getRequestURI(), CODIGO);
        objectMapper.writeValue(respuesta.getWriter(), cuerpo);
    }

    /**
     * La marca se lee del usuario que acaba de cargar el filtro JWT desde la
     * base, no de un claim del token: así, restablecer la contraseña de alguien
     * que está trabajando surte efecto en su siguiente petición y no cuando
     * expire su token.
     */
    private boolean debeBloquearse(HttpServletRequest peticion) {
        if (RUTAS_PERMITIDAS.contains(peticion.getRequestURI())) {
            return false;
        }

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null
                && autenticacion.getPrincipal() instanceof UsuarioAutenticado autenticado
                && autenticado.getUsuario().isDebeCambiarPassword();
    }
}
