package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.util.List;

/**
 * YS-167. Un especialista con su horario, ordenado de lunes a domingo.
 *
 * <p>
 * El médico va como {@link ResumenPersona}: la Fundación lo elige para una
 * cita, no administra su cuenta.
 */
public record EspecialistaResponse(
        Long id,
        ResumenPersona medico,
        EspecialidadResponse especialidad,
        int duracionCitaMinutos,
        boolean activo,
        List<HorarioAtencionDto> horarios) {
}
