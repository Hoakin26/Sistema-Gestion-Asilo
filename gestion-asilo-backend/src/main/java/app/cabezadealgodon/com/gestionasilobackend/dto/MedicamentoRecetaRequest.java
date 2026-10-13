package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * YS-108. Un medicamento de la receta. Los cuatro campos son obligatorios
 * (criterio de HU-10). Un error sale con el nombre del campo y su fila,
 * "medicamentos[1].dosis", para que la pantalla lo pinte donde está.
 */
public record MedicamentoRecetaRequest(
        @NotBlank(message = "El nombre del medicamento es obligatorio")
        @Size(max = 150, message = "El nombre del medicamento no puede exceder 150 caracteres")
        String medicamento,

        @NotBlank(message = "La dosis es obligatoria")
        @Size(max = 100, message = "La dosis no puede exceder 100 caracteres")
        String dosis,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad tiene que ser al menos 1")
        @Max(value = 999, message = "La cantidad no puede pasar de 999")
        Integer cantidad,

        @NotBlank(message = "El tiempo o la forma de aplicación es obligatorio")
        @Size(max = 200, message = "El tiempo o la forma de aplicación no puede exceder 200 caracteres")
        String formaAplicacion) {
}
