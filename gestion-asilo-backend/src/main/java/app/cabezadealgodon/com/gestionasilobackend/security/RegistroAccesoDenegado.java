package app.cabezadealgodon.com.gestionasilobackend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * YS-54. Deja constancia de los intentos de acceso denegado.
 *
 * <p>
 * Un 403 aislado suele ser alguien que se equivocó de enlace; el mismo 403
 * repetido es otra cosa. Sin registro no hay forma de distinguirlos, porque el
 * usuario rechazado no reporta nada y el sistema tampoco.
 *
 * <p>
 * Está aparte del manejador de la respuesta porque hay dos caminos por los que
 * se llega a un 403 de autorización —Spring MVC y el filtro de seguridad— y
 * ambos deben registrar igual. Duplicar el formato garantizaría que con el
 * tiempo se separen.
 */
@Component
public class RegistroAccesoDenegado {

    /**
     * El mismo texto para los dos caminos. No dice qué rol haría falta: eso le
     * describiría el mapa de permisos a quien está probando dónde puede entrar.
     */
    public static final String MENSAJE = "No tiene permiso para realizar esta acción. "
            + "Si cree que es un error, consúltelo con el Administrador del Asilo";

    private static final Logger bitacora = LoggerFactory.getLogger(RegistroAccesoDenegado.class);

    /**
     * Se registra quién, con qué rol, qué intentó y desde dónde. Nunca el token
     * ni el cuerpo de la petición: el registro terminaría guardando credenciales
     * y datos de internos en un archivo que no tiene ninguna de sus protecciones.
     */
    public void registrar(HttpServletRequest peticion) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String quien = autenticacion == null ? "sin identificar" : autenticacion.getName();
        String roles = autenticacion == null ? "sin rol" : autenticacion.getAuthorities().toString();

        // WARN y no ERROR: el sistema hizo exactamente lo que debía. Merece
        // atención si se repite, no figurar entre las fallas del servicio.
        bitacora.warn("Acceso denegado: {} con {} intentó {} {} desde {}",
                quien, roles, peticion.getMethod(), peticion.getRequestURI(), origen(peticion));
    }

    /**
     * Se usa la dirección de la conexión y no la cabecera {@code X-Forwarded-For},
     * que el propio cliente puede escribir: un atacante elegiría qué origen queda
     * anotado. Si algún día el sistema queda detrás de un proxy inverso, habrá
     * que leer esa cabecera, pero solo confiando en la que agregue el proxy.
     */
    private String origen(HttpServletRequest peticion) {
        return peticion.getRemoteAddr();
    }
}
