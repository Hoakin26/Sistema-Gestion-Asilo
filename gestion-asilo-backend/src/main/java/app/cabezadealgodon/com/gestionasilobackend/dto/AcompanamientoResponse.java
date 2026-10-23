package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;

/**
 * YS-188. Una solicitud de referencia en la que el enfermero(a) acompaña al
 * interno (RF-03): a quién, a qué especialidad y por qué, quién la refirió, en
 * qué estado está y, si la fundación ya la asignó, la cita.
 *
 * <p>
 * {@code cita} es {@code null} mientras la solicitud espera asignación.
 */
public record AcompanamientoResponse(
        Long solicitudId,
        String numero,
        EstadoSolicitud estado,
        ResumenPersona interno,
        EspecialidadResponse especialidad,
        String motivo,
        ResumenPersona medicoGeneral,
        LocalDateTime fechaCreacion,
        CitaAsignada cita) {

    /** La cita de la solicitud: cuándo, cuánto dura y con quién. */
    public record CitaAsignada(Long id, LocalDateTime fechaHora, int duracionMinutos, ResumenPersona especialista) {
    }
}
