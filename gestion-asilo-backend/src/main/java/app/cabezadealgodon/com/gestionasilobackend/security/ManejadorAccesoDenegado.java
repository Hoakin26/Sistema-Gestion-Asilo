package app.cabezadealgodon.com.gestionasilobackend.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import app.cabezadealgodon.com.gestionasilobackend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * YS-54. Responde 403 con el mismo formato de error que el resto de la API,
 * igual que {@link EntryPointNoAutenticado} hace con el 401. Sin esto Spring
 * devuelve un 403 con el cuerpo vacío y el frontend no tiene nada que mostrarle
 * al usuario, que se queda ante una pantalla que no reacciona.
 *
 * <p>
 * Atiende las denegaciones que ocurren en la cadena de filtros. Las que nacen
 * de {@code @PreAuthorize} sobre un controlador las resuelve antes Spring MVC,
 * en {@code ManejadorGlobalExcepciones}; los dos producen la misma respuesta y
 * registran por el mismo componente, así que cuál actúe es indistinto para
 * quien recibe el 403.
 */
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final RegistroAccesoDenegado registro;

    public ManejadorAccesoDenegado(ObjectMapper objectMapper, RegistroAccesoDenegado registro) {
        this.objectMapper = objectMapper;
        this.registro = registro;
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
            AccessDeniedException excepcion) throws IOException {
        registro.registrar(peticion);

        respuesta.setStatus(HttpStatus.FORBIDDEN.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");

        ErrorResponse cuerpo = ErrorResponse.de(HttpStatus.FORBIDDEN, RegistroAccesoDenegado.MENSAJE,
                peticion.getRequestURI());
        objectMapper.writeValue(respuesta.getWriter(), cuerpo);
    }
}
