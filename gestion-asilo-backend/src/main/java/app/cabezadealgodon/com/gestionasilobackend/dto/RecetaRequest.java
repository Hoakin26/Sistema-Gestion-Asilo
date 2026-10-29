package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** YS-108. La receta completa, como debe quedar: al corregirla se manda entera. */
public record RecetaRequest(
        @Size(max = 1000, message = "Las indicaciones no pueden exceder 1000 caracteres")
        String indicacionesGenerales,

        @NotEmpty(message = "Agregue al menos un medicamento")
        @Size(max = 30, message = "Una receta no puede tener más de 30 medicamentos")
        List<@Valid MedicamentoRecetaRequest> medicamentos) {
}
