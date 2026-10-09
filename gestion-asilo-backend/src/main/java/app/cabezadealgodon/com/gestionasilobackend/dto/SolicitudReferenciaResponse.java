package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;

/** YS-84. Una solicitud de referencia tal como la devuelve la API. */
public record SolicitudReferenciaResponse(
        Long id,
        String numero,
        EstadoSolicitud estado,
        String motivo,
        ResumenPersona interno,
        EspecialidadResponse especialidad,
        ResumenPersona medicoGeneral,
        ResumenPersona enfermeroAcompanante,
        LocalDateTime fechaCreacion) {
}
