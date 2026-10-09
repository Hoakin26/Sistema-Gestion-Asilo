package app.cabezadealgodon.com.gestionasilobackend.exception;

/**
 * YS-167 y YS-91. Lo que se pide choca con algo que ya existe: un perfil de
 * especialista para una cuenta que ya tiene uno, o una cita a una hora que el
 * especialista ya tiene ocupada. Se responde 409.
 *
 * <p>
 * Es genérica a propósito: el correo y el DPI tienen excepción propia porque
 * llegaron antes, pero cada conflicto nuevo no necesita una clase nueva para
 * decir lo mismo con otro mensaje.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
