package app.cabezadealgodon.com.gestionasilobackend.dto;

/**
 * YS-84. Una persona nombrada dentro de otro recurso: el interno referido, el
 * médico que refiere, el enfermero que acompaña.
 *
 * <p>
 * Solo el id y el nombre, a propósito. Quien necesite más de esa persona lo
 * pide por su propio endpoint, con sus propios permisos; así una solicitud no
 * termina filtrando el correo de un usuario o el DPI de un interno a un rol
 * que no podría consultarlos directamente.
 */
public record ResumenPersona(Long id, String nombreCompleto) {
}
