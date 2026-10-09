package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;

/**
 * YS-99. Una visita médica como la ve el especialista: el paciente, el motivo
 * de la referencia (criterio de HU-07) y la fecha en que se inició.
 *
 * <p>
 * No lleva el costo de la consulta: es un dato para la cuenta del familiar y
 * para la fundación, no para quien atiende. El diagnóstico y las
 * observaciones se agregan con HU-09.
 */
public record VisitaMedicaResponse(
        Long id,
        EstadoVisita estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaCierre,
        Long citaId,
        LocalDateTime fechaCita,
        Long solicitudId,
        String numeroSolicitud,
        EstadoSolicitud estadoSolicitud,
        String motivo,
        Paciente paciente,
        EspecialidadResponse especialidad,
        ResumenPersona especialista,
        ResumenPersona medicoGeneral,
        ResumenPersona enfermeroAcompanante) {

    /** Lo que el encabezado de la visita muestra del interno. */
    public record Paciente(Long id, String nombreCompleto, LocalDate fechaNacimiento) {
    }
}
