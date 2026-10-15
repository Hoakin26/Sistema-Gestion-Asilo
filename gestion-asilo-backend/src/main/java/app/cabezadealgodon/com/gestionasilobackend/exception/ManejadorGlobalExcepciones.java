package app.cabezadealgodon.com.gestionasilobackend.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import app.cabezadealgodon.com.gestionasilobackend.dto.ErrorResponse;
import app.cabezadealgodon.com.gestionasilobackend.security.RegistroAccesoDenegado;
import jakarta.servlet.http.HttpServletRequest;

/** Traduce las excepciones a respuestas HTTP con un cuerpo uniforme. */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    /**
     * YS-172. Lo que se le dice al usuario cuando el servidor falla por algo
     * que nadie previó. Genérico a propósito: el detalle de la excepción
     * revela nombres de clases, tablas y columnas, y a quien usa el sistema no
     * le sirve. La traza completa queda en el log.
     */
    public static final String MENSAJE_INESPERADO =
            "Ocurrió un error inesperado. Intente de nuevo y, si se repite, avise al administrador del sistema";

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

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
     * YS-167. Falta un parámetro obligatorio de la URL, como la fecha al
     * consultar las horas libres. Sin esto Spring respondía con su propio
     * cuerpo de error.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> manejarParametroFaltante(MissingServletRequestParameterException ex,
            HttpServletRequest peticion) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(HttpStatus.BAD_REQUEST,
                "Falta el parámetro '" + ex.getParameterName() + "'", peticion.getRequestURI()));
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

    /** YS-166. Lo mismo, para las listas que validan el orden por su cuenta. */
    @ExceptionHandler(OrdenNoPermitidoException.class)
    public ResponseEntity<ErrorResponse> manejarOrdenNoPermitido(OrdenNoPermitidoException ex,
            HttpServletRequest peticion) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de(HttpStatus.BAD_REQUEST, ex.getMessage(), peticion.getRequestURI()));
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
     * misma respuesta para ambos. Desde HU-06, también cualquier
     * {@link ConflictoException}: un especialista duplicado o una hora ocupada.
     */
    @ExceptionHandler({ CorreoDuplicadoException.class, DpiDuplicadoException.class, ConflictoException.class })
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

    /**
     * YS-172. La red de seguridad: lo que ningún método de arriba atiende.
     *
     * <p>
     * Sin ella, una falla no prevista salía del controlador sin traducir. El
     * contenedor la reenviaba a {@code /error}, ese reenvío no lleva el token,
     * y la respuesta terminaba siendo 401: el frontend la tomaba por una
     * sesión vencida y sacaba al usuario, que perdía lo que estaba escribiendo.
     * Atiende también los {@link Error} de Java, como el de una clase
     * compilada con errores, porque Spring MVC los entrega envueltos en una
     * excepción.
     *
     * <p>
     * Las excepciones de Spring que ya traen su estado se respetan: una
     * dirección que no existe es 404 y un método que no corresponde, 405, con
     * sus cabeceras (el {@code Allow} del 405). Este método se consulta antes
     * que el resolvedor de Spring que las atendía, y sin esa distinción las
     * convertiría todas en 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarNoPrevisto(Exception ex, HttpServletRequest peticion) {
        HttpStatus estado = HttpStatus.INTERNAL_SERVER_ERROR;
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(estado);
        if (ex instanceof org.springframework.web.ErrorResponse conEstado) {
            HttpStatus propio = HttpStatus.resolve(conEstado.getStatusCode().value());
            if (propio != null) {
                estado = propio;
                respuesta = ResponseEntity.status(estado).headers(conEstado.getHeaders());
            }
        }

        if (!estado.is4xxClientError()) {
            log.error("Error no previsto en {} {}", peticion.getMethod(), peticion.getRequestURI(), ex);
        }
        return respuesta.body(ErrorResponse.de(estado, mensajePara(estado), peticion.getRequestURI()));
    }

    /**
     * YS-172. El mensaje de una respuesta de error que no trae uno propio. Lo
     * usan este manejador y el cuerpo del reenvío a {@code /error}
     * ({@link AtributosDeError}), para que digan lo mismo.
     */
    static String mensajePara(HttpStatus estado) {
        return switch (estado) {
            case NOT_FOUND -> "La dirección solicitada no existe";
            case METHOD_NOT_ALLOWED -> "Esa operación no está disponible en esta dirección";
            default -> estado.is4xxClientError() ? "No se pudo procesar la petición" : MENSAJE_INESPERADO;
        };
    }
}
