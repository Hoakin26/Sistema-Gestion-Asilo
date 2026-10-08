package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDate;

/**
 * YS-58. La fila de la lista de internos.
 *
 * <p>
 * Más corto que {@link InternoResponse} a propósito: la lista puede traer
 * decenas de filas y no necesita la cuota ni los datos del familiar. Enviar de
 * menos en un listado es gratis; enviar de más multiplica el peso de cada
 * respuesta por el número de filas.
 */
public record InternoResumenResponse(
        Long id,
        String nombreCompleto,
        String dpi,
        LocalDate fechaIngreso,
        boolean activo) {
}
