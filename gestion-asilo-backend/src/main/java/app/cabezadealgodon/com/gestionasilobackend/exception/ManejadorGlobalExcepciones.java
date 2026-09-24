package app.cabezadealgodon.com.gestionasilobackend.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
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

    /**
     * Mismo mensaje para contraseña incorrecta, correo inexistente y cuenta
     * desactivada. Distinguirlos le diría a un atacante qué correos están
     * registrados, y Spring lanza DisabledException antes de comprobar la
     * contraseña: bastaría con acertar el correo para saberlo.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> manejarAutenticacion(HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.de(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", peticion.getRequestURI()));
    }

    /**
     * YS-69. Mismo formato que una validación fallida (errores por campo), para
     * que el frontend lo muestre junto al campo. 400 y no 401: ver la excepción.
     */
    @ExceptionHandler(CambioPasswordInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarCambioPassword(CambioPasswordInvalidoException ex,
            HttpServletRequest peticion) {
        ErrorResponse cuerpo = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), "Hay campos inválidos", peticion.getRequestURI(),
                Map.of(ex.getCampo(), ex.getMessage()));
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException ex,
            HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.de(HttpStatus.NOT_FOUND, ex.getMessage(), peticion.getRequestURI()));
    }

    /**
     * YS-71. 403 y no 401: quien la recibe tiene sesión válida, solo que esa
     * operación no corresponde. Un 401 haría que el frontend lo diera por
     * expulsado del sistema.
     */
    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ErrorResponse> manejarOperacionNoPermitida(OperacionNoPermitidaException ex,
            HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.de(HttpStatus.FORBIDDEN, ex.getMessage(), peticion.getRequestURI()));
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
