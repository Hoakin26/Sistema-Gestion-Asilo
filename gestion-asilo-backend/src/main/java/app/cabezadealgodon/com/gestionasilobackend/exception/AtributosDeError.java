package app.cabezadealgodon.com.gestionasilobackend.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 * YS-172. El cuerpo de lo que responde el reenvío interno a {@code /error}:
 * el mismo formato que el resto de la API ({@code ErrorResponse}), en vez del
 * de Spring Boot.
 *
 * <p>
 * Llega ahí lo que falla fuera de Spring MVC, adonde el manejador global no
 * alcanza: por ejemplo, el filtro JWT cuando la base de datos no responde, o
 * una respuesta de error que arma el propio contenedor. Con el formato de
 * Spring Boot, el frontend no encontraba el mensaje.
 *
 * <p>
 * El detalle de la falla no se incluye: Spring Boot ya lo deja fuera por
 * defecto, y aquí ni siquiera se lee.
 */
@Component
public class AtributosDeError extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest peticion, ErrorAttributeOptions opciones) {
        Map<String, Object> deSpring = super.getErrorAttributes(peticion, ErrorAttributeOptions.defaults());
        HttpStatus estado = deSpring.get("status") instanceof Integer codigo && HttpStatus.resolve(codigo) != null
                ? HttpStatus.valueOf(codigo)
                : HttpStatus.INTERNAL_SERVER_ERROR;

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now());
        cuerpo.put("estado", estado.value());
        cuerpo.put("error", estado.getReasonPhrase());
        cuerpo.put("mensaje", ManejadorGlobalExcepciones.mensajePara(estado));
        cuerpo.put("ruta", deSpring.get("path"));
        return cuerpo;
    }
}
