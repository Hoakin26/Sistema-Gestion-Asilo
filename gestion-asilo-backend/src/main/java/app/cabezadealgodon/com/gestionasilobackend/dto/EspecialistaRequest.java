package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * YS-167. El perfil de un especialista, para registrarlo o actualizarlo.
 *
 * <p>
 * {@code usuarioId} solo cuenta al registrar: el perfil no cambia de cuenta.
 * {@code activo} solo cuenta al actualizar: un perfil nuevo nace activo.
 */
public record EspecialistaRequest(
        Long usuarioId,

        @NotNull(message = "Seleccione la especialidad")
        Long especialidadId,

        @NotNull(message = "Indique cuánto dura cada cita")
        @Min(value = 10, message = "Una cita dura al menos 10 minutos")
        @Max(value = 120, message = "Una cita no puede durar más de 120 minutos")
        Integer duracionCitaMinutos,

        @NotEmpty(message = "Indique al menos un día de atención")
        List<@Valid HorarioAtencionDto> horarios,

        Boolean activo) {
}
