package app.cabezadealgodon.com.gestionasilobackend.eventos;

/**
 * YS-91. Se publica cuando la Fundación le asigna la cita a una solicitud.
 *
 * <p>
 * Lo escucha {@link AvisoFamiliarListener} para el segundo aviso del RF-04:
 * el que informa el especialista asignado, que al crear la solicitud todavía
 * no existía. Como en {@link SolicitudCreadaEvent}, lleva solo el id.
 */
public record CitaAsignadaEvent(Long solicitudId) {
}
