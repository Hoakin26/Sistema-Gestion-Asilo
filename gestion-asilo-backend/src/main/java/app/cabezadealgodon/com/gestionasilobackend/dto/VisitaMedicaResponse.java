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
 * para la fundación, no para quien atiende. HU-09 (YS-105) le agrega el
 * diagnóstico y las observaciones.
 */
public record VisitaMedicaResponse(
        Long id,
        EstadoVisita estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaCierre,
        String diagnostico,
        String observaciones,
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
