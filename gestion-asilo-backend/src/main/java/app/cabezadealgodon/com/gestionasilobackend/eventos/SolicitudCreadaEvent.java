package app.cabezadealgodon.com.gestionasilobackend.eventos;

/**
 * YS-84. Se publica cada vez que se crea una solicitud de referencia.
 *
 * <p>
 * Hoy nadie lo escucha. Existe para que HU-04 —el correo al familiar, RF-04—
 * se agregue escuchándolo, sin tocar {@code SolicitudReferenciaService}: el
 * servicio que crea la solicitud no tiene por qué saber que existen los
 * correos (principio abierto/cerrado, sección 4.7 del diseño).
 *
 * <p>
 * Lleva solo el id y no la entidad: quien lo escuche después de confirmada la
 * transacción tiene que volver a leer la solicitud, y una entidad pasada de
 * mano en mano llegaría desconectada de la base.
 */
public record SolicitudCreadaEvent(Long solicitudId) {
}
