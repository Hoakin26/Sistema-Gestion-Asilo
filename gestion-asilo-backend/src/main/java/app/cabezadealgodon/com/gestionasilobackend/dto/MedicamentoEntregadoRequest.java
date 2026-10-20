package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * YS-126. Un medicamento que la farmacia entrega en el despacho, por el id con
 * que viene en {@link RecetaResponse}. Un error sale con su fila,
 * "entregados[1].id", como en la receta.
 */
public record MedicamentoEntregadoRequest(
        @NotNull(message = "Indique el medicamento")
        Long id) {
}
