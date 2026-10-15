package app.cabezadealgodon.com.gestionasilobackend.exception;

/**
 * YS-84. Un campo que pasó la validación de formato del DTO pero que el
 * servicio rechaza al mirar la base: por ejemplo, el id de un interno que no
 * existe o el de un usuario que no es de enfermería.
 *
 * <p>
 * Se responde 400 con el mismo cuerpo que una validación fallida, con el error
 * bajo el nombre del campo, para que el frontend lo pinte junto a él. No es un
 * 404: el recurso que no existe no es el de la URL, es un dato del formulario.
 */
public class CampoInvalidoException extends RuntimeException {

    private final String campo;

    public CampoInvalidoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
