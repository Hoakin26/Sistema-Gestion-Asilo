package app.cabezadealgodon.com.gestionasilobackend.exception;

/**
 * YS-58. Ya existe un interno con ese DPI.
 *
 * <p>
 * Es el equivalente de {@link CorreoDuplicadoException} para los internos: el
 * DPI identifica a una persona, así que dos expedientes con el mismo número
 * serían la misma persona registrada dos veces, con su historial clínico
 * partido en dos.
 */
public class DpiDuplicadoException extends RuntimeException {

    public DpiDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
