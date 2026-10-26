package app.cabezadealgodon.com.gestionasilobackend.exception;

import java.sql.SQLTransientConnectionException;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * DEF-03. Reconoce la falla de "no hay conexión con la base de datos" y guarda
 * el mensaje para el usuario. Lo usan ManejadorGlobalExcepciones y el filtro
 * JWT, que falla antes de llegar a Spring MVC.
 */
public final class BaseDeDatosNoDisponible {

    public static final String MENSAJE =
            "El sistema no puede conectarse a la base de datos en este momento. Intente de nuevo en unos minutos";

    private BaseDeDatosNoDisponible() {
    }

    /** Si la falla, o alguna de sus causas, es que no se pudo conseguir una conexión. */
    public static boolean esLaCausa(Throwable falla) {
        for (Throwable actual = falla; actual != null; actual = actual.getCause()) {
            if (actual instanceof CannotCreateTransactionException
                    || actual instanceof DataAccessResourceFailureException
                    || actual instanceof SQLTransientConnectionException) {
                return true;
            }
        }
        return false;
    }
}
