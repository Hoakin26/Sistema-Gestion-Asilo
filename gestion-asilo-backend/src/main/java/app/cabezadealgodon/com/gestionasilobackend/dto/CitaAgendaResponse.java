package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;

/**
 * YS-99. Una cita en la agenda del especialista: a quién atiende, a qué hora,
 * por qué lo refirieron y quién lo acompaña. Lleva el motivo porque el
 * especialista es personal clínico y lo necesita para preparar la consulta.
 *
 * <p>
 * {@code visitaId} está vacío mientras la visita no se inicie; con él, la
 * agenda ofrece "Continuar visita" en vez de "Iniciar visita".
 */
public record CitaAgendaResponse(
        Long id,
        LocalDateTime fechaHora,
        int duracionMinutos,
        Long solicitudId,
        String numeroSolicitud,
        EstadoSolicitud estado,
        String motivo,
        ResumenPersona interno,
        EspecialidadResponse especialidad,
        ResumenPersona enfermeroAcompanante,
        Long visitaId) {
}
