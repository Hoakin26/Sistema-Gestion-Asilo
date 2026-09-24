package app.cabezadealgodon.com.gestionasilobackend.exception;

/**
 * La petición está bien formada y quien la hace tiene sesión válida, pero la
 * operación no corresponde sobre ese objetivo. Se traduce a 403: no es un 401,
 * que el frontend interpretaría como sesión terminada, ni un 400, que sugeriría
 * un error en los datos enviados.
 */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
