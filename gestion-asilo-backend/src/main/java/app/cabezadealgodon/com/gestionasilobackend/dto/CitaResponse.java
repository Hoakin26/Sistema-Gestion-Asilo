package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;

/** YS-91. Una cita asignada, como la muestra el detalle de la solicitud. */
public record CitaResponse(
        Long id,
        Long solicitudId,
        ResumenPersona especialista,
        EspecialidadResponse especialidad,
        LocalDateTime fechaHora,
        int duracionMinutos) {
}
