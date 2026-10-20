package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * HU-17 (YS-129). Lo que la farmacia cobró de una receta: el costo de cada
 * medicamento entregado y el total. Solo para la farmacia.
 *
 * <p>
 * Va aparte de {@link RecetaResponse}, que también recibe el especialista: él
 * receta por criterio clínico y no ve costos, como decidió HU-14 para el
 * laboratorio el 19/10.
 */
public record CostosRecetaResponse(List<Costo> entregados, BigDecimal total) {

    /** {@code id} es el del medicamento en {@link RecetaResponse}. */
    public record Costo(Long id, BigDecimal costo) {
    }
}
