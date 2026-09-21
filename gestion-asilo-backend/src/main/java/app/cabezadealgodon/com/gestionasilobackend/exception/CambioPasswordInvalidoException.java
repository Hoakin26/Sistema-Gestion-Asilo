package app.cabezadealgodon.com.gestionasilobackend.exception;

/**
 * YS-69. Error en un campo concreto del cambio de contraseña. Se responde con
 * 400 y no con 401: el frontend trata todo 401 como sesión terminada y sacaría
 * del sistema a quien solo se equivocó al escribir.
 */
public class CambioPasswordInvalidoException extends RuntimeException {

    private final String campo;

    public CambioPasswordInvalidoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
