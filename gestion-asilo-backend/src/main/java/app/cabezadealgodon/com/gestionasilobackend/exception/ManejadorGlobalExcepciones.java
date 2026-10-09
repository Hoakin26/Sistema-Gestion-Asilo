package app.cabezadealgodon.com.gestionasilobackend.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import app.cabezadealgodon.com.gestionasilobackend.dto.ErrorResponse;
import app.cabezadealgodon.com.gestionasilobackend.security.RegistroAccesoDenegado;
import jakarta.servlet.http.HttpServletRequest;

/** Traduce las excepciones a respuestas HTTP con un cuerpo uniforme. */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private final RegistroAccesoDenegado registroAccesoDenegado;

    public ManejadorGlobalExcepciones(RegistroAccesoDenegado registroAccesoDenegado) {
        this.registroAccesoDenegado = registroAccesoDenegado;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex,
            HttpServletRequest peticion) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ErrorResponse cuerpo = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), "Hay campos inválidos", peticion.getRequestURI(),
                null, errores);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(HttpServletRequest peticion) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es válido: revise el formato JSON y los valores enviados",
                peticion.getRequestURI()));
    }

    /**
     * YS-87. Un parámetro de la URL que no se puede convertir, como
     * {@code ?estado=INVENTADO}. Sin esto Spring respondía 400 con su propio
     * cuerpo, distinto del de toda la API.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarParametroInvalido(MethodArgumentTypeMismatchException ex,
            HttpServletRequest peticion) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(HttpStatus.BAD_REQUEST,
                "El valor de '" + ex.getName() + "' no es válido", peticion.getRequestURI()));
    }

    /**
     * YS-87. Un {@code sort} por un campo que no existe. Spring Data lo
     * lanza al armar la consulta y, sin esto, salía como un 500: un error del
     * servidor por algo que escribió mal el cliente.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> manejarOrdenInvalido(PropertyReferenceException ex,
            HttpServletRequest peticion) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(HttpStatus.BAD_REQUEST,
                "No se puede ordenar por '" + ex.getPropertyName() + "'", peticion.getRequestURI()));
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
     *
     * <p>
     * YS-84. Atiende cualquier {@link CampoInvalidoException}, de la que el
     * cambio de contraseña es un caso particular.
     */
    @ExceptionHandler(CampoInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarCampoInvalido(CampoInvalidoException ex,
            HttpServletRequest peticion) {
        ErrorResponse cuerpo = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), "Hay campos inválidos", peticion.getRequestURI(),
                null, Map.of(ex.getCampo(), ex.getMessage()));
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

    /**
     * YS-54. El 403 por rol. Es distinto del de arriba: aquel responde a una
     * regla de negocio sobre un caso puntual, y este a que el rol no tiene nada
     * que hacer en ese endpoint, así que además queda registrado.
     *
     * <p>
     * Hace falta aquí y no solo en el manejador de la cadena de filtros porque
     * {@code @PreAuthorize} sobre un controlador lanza la excepción dentro del
     * DispatcherServlet, y Spring MVC la atiende antes de que llegue al filtro.
     * Sin este método la petición saldría con un 403 sin cuerpo.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(HttpServletRequest peticion) {
        registroAccesoDenegado.registrar(peticion);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.de(HttpStatus.FORBIDDEN,
                RegistroAccesoDenegado.MENSAJE, peticion.getRequestURI()));
    }

    /**
     * Identificadores que ya pertenecen a otro registro: el correo de un
     * usuario y, desde YS-58, el DPI de un interno. Comparten manejador porque
     * comparten significado —lo que se intenta crear ya existe— y el 409 es la
     * misma respuesta para ambos.
     */
    @ExceptionHandler({ CorreoDuplicadoException.class, DpiDuplicadoException.class })
    public ResponseEntity<ErrorResponse> manejarDuplicado(RuntimeException ex, HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de(HttpStatus.CONFLICT, ex.getMessage(), peticion.getRequestURI()));
    }

    /**
     * YS-83. Una transición que el diagrama de estados no contempla: por
     * ejemplo, asignarle cita a una solicitud que ya está cancelada.
     */
    @ExceptionHandler(TransicionInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarTransicionInvalida(TransicionInvalidaException ex,
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
