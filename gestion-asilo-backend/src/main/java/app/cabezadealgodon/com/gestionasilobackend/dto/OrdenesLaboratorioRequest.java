package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** YS-111. Uno o más exámenes a ordenar de una vez (criterio de HU-11). */
public record OrdenesLaboratorioRequest(
        @NotEmpty(message = "Seleccione al menos un examen")
        @Size(max = 20, message = "No se pueden ordenar más de 20 exámenes a la vez")
        List<@Valid ExamenOrdenadoRequest> examenes) {
}
