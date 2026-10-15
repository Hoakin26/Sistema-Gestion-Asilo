package app.cabezadealgodon.com.gestionasilobackend.eventos;

/**
 * YS-84. Se publica cada vez que se crea una solicitud de referencia.
 *
 * <p>
 * Lo escucha {@link AvisoFamiliarListener}, que le escribe al familiar (HU-04,
 * RF-04). El servicio que crea la solicitud no tiene por qué saber que existen
 * los correos (principio abierto/cerrado, sección 4.7 del diseño).
 *
 * <p>
 * Lleva solo el id y no la entidad: quien lo escuche después de confirmada la
 * transacción tiene que volver a leer la solicitud, y una entidad pasada de
 * mano en mano llegaría desconectada de la base.
 */
public record SolicitudCreadaEvent(Long solicitudId) {
}
