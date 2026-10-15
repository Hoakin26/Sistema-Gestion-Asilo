package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** YS-58. Un medicamento permanente de la ficha médica. */
public record MedicamentoHabitualRequest(
        @NotBlank(message = "El nombre del medicamento es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,

        // Obligatorios los dos: un medicamento sin dosis ni frecuencia no le
        // sirve a quien tiene que administrarlo.
        @NotBlank(message = "La dosis es obligatoria")
        @Size(max = 100, message = "La dosis no puede exceder 100 caracteres")
        String dosis,

        @NotBlank(message = "La frecuencia es obligatoria")
        @Size(max = 100, message = "La frecuencia no puede exceder 100 caracteres")
        String frecuencia) {
}
