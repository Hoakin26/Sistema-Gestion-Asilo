package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/** YS-167. Un día de atención de un especialista, de ida y de vuelta. */
public record HorarioAtencionDto(
        @NotNull(message = "Indique el día")
        DayOfWeek dia,

        @NotNull(message = "Indique la hora de inicio")
        LocalTime horaInicio,

        @NotNull(message = "Indique la hora de fin")
        LocalTime horaFin) {
}
