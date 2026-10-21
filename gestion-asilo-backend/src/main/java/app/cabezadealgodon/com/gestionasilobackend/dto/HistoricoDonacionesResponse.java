package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoDonante;

/**
 * YS-145. El histórico de donaciones con los filtros pedidos (HU-22), y lo que
 * suman en el período: en total y por tipo de donante, sin las anuladas. Los
 * totales se calculan aquí, en {@code BigDecimal}, y no en la pantalla.
 */
public record HistoricoDonacionesResponse(
        List<DonacionResponse> donaciones,
        BigDecimal total,
        List<TotalPorTipo> porTipo) {

    /** Lo de un tipo de donante: cuántas donaciones vigentes y cuánto suman. */
    public record TotalPorTipo(TipoDonante tipoDonante, int cantidad, BigDecimal total) {
    }
}
