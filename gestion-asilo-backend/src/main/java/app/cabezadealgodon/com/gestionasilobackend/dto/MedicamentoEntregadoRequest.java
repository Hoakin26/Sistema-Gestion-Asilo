package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * YS-126. Un medicamento que la farmacia entrega en el despacho, por el id con
 * que viene en {@link RecetaResponse}. Un error sale con su fila,
 * "entregados[1].id", como en la receta.
 *
 * <p>
 * YS-129. Con su costo (HU-17), obligatorio y mayor que cero, por la cantidad
 * recetada. Va como texto, igual que el costo del laboratorio: el servicio lo
 * lee como lo escribe una persona, con las mismas reglas y mensajes, y el error
 * sale en "entregados[1].costo".
 */
public record MedicamentoEntregadoRequest(
        @NotNull(message = "Indique el medicamento")
        Long id,
        String costo) {
}
