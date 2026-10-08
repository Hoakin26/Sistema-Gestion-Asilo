package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int estado,
        String error,
        String mensaje,
        String ruta,

        /**
         * YS-74. Identifica la causa del error cuando el frontend tiene que
         * reaccionar y no solo mostrar el mensaje. Va nulo en la mayoría de los
         * casos y, gracias a NON_NULL, ni siquiera aparece en el JSON.
         *
         * Existe para que el frontend no tenga que reconocer la situación por
         * el texto del mensaje: eso ataría su comportamiento a una frase en
         * español que cualquiera puede reescribir sin saber que algo depende
         * de ella.
         */
        String codigo,

        Map<String, String> errores) {

    public static ErrorResponse de(HttpStatus estado, String mensaje, String ruta) {
        return de(estado, mensaje, ruta, null);
    }

    public static ErrorResponse de(HttpStatus estado, String mensaje, String ruta, String codigo) {
        return new ErrorResponse(LocalDateTime.now(), estado.value(), estado.getReasonPhrase(), mensaje, ruta,
                codigo, null);
    }
}
