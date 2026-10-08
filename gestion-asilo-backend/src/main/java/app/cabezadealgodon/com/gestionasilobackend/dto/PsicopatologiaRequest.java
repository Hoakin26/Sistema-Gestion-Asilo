package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** YS-58. Un padecimiento de la ficha médica. */
public record PsicopatologiaRequest(
        @NotBlank(message = "El nombre del padecimiento es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,

        // Opcional: no siempre se conoce la fecha del diagnóstico.
        @PastOrPresent(message = "La fecha de diagnóstico no puede estar en el futuro")
        LocalDate fechaDiagnostico,

        @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
        String observaciones) {
}
