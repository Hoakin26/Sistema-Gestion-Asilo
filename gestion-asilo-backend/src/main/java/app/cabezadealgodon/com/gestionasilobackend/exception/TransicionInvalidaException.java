package app.cabezadealgodon.com.gestionasilobackend.exception;

/**
 * YS-83. Se intentó llevar un registro a un estado que su diagrama no
 * contempla desde el estado actual (DE-01 a DE-04).
 *
 * <p>
 * Se traduce a 409, como indica la tabla de códigos del diseño (sección 4.9):
 * los datos enviados están bien formados, pero chocan con el estado actual del
 * registro. No reutiliza {@link OperacionNoPermitidaException}, que responde
 * 403 porque significa otra cosa: que esa operación no le corresponde a quien
 * la pide.
 */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
