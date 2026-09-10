package app.cabezadealgodon.com.gestionasilobackend.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import app.cabezadealgodon.com.gestionasilobackend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

/** Traduce las excepciones a respuestas HTTP con un cuerpo uniforme. */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex,
            HttpServletRequest peticion) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ErrorResponse cuerpo = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), "Hay campos inválidos", peticion.getRequestURI(), errores);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(HttpServletRequest peticion) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es válido: revise el formato JSON y los valores enviados",
                peticion.getRequestURI()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException ex,
            HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.de(HttpStatus.NOT_FOUND, ex.getMessage(), peticion.getRequestURI()));
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarCorreoDuplicado(CorreoDuplicadoException ex,
            HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de(HttpStatus.CONFLICT, ex.getMessage(), peticion.getRequestURI()));
    }

    /**
     * Red de seguridad por si una restricción de la base de datos se dispara pese
     * a la validación del service (p. ej. dos peticiones simultáneas con el mismo
     * correo). El mensaje es genérico a propósito: el detalle de la excepción
     * revela nombres de tablas y restricciones.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.de(HttpStatus.CONFLICT,
                "La operación entra en conflicto con datos ya existentes", peticion.getRequestURI()));
    }
}
