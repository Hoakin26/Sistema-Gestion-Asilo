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
        Map<String, String> errores) {

    public static ErrorResponse de(HttpStatus estado, String mensaje, String ruta) {
        return new ErrorResponse(LocalDateTime.now(), estado.value(), estado.getReasonPhrase(), mensaje, ruta, null);
    }
}
