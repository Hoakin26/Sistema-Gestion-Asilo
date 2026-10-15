package app.cabezadealgodon.com.gestionasilobackend.exception;

/**
 * YS-69. Error en un campo concreto del cambio de contraseña. Se responde con
 * 400 y no con 401: el frontend trata todo 401 como sesión terminada y sacaría
 * del sistema a quien solo se equivocó al escribir.
 *
 * <p>
 * YS-84. Es un caso particular de {@link CampoInvalidoException}, que se
 * generalizó cuando la solicitud de referencia necesitó el mismo trato.
 */
public class CambioPasswordInvalidoException extends CampoInvalidoException {

    public CambioPasswordInvalidoException(String campo, String mensaje) {
        super(campo, mensaje);
    }
}
