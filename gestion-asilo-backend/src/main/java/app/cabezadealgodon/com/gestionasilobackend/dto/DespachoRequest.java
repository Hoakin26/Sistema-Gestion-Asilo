package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * YS-126. Un despacho de la farmacia (HU-16): los medicamentos de la receta
 * que entrega ahora. Los que no van en la lista quedan pendientes, que es como
 * se marca un medicamento sin existencias (CU-005, E.1); por eso la lista
 * puede llegar vacía, pero no faltar.
 *
 * <p>
 * HU-17 le agrega a cada medicamento entregado su costo, en esta misma
 * petición (YS-129).
 */
public record DespachoRequest(
        @NotNull(message = "Indique qué medicamentos se entregaron")
        @Size(max = 30, message = "Una receta no tiene más de 30 medicamentos")
        @Valid
        List<MedicamentoEntregadoRequest> entregados) {
}
