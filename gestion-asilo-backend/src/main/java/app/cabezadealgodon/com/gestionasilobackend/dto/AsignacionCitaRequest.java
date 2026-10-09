package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/** YS-91. Con quién y cuándo: lo que la Fundación elige al asignar la cita. */
public record AsignacionCitaRequest(
        @NotNull(message = "Seleccione al especialista")
        Long especialistaId,

        @NotNull(message = "Seleccione la fecha y la hora")
        LocalDateTime fechaHora) {
}
