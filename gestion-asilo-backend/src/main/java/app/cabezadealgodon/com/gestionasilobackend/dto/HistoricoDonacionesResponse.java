package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoDonante;

/**
 * YS-145. El histórico de donaciones con los filtros pedidos (HU-22), y lo que
 * suman en el período: en total y por tipo de donante, sin las anuladas. Los
 * totales se calculan aquí, en {@code BigDecimal}, y no en la pantalla.
 *
 * <p>
 * OBS-01. Las donaciones llegan por páginas, pero los totales siguen siendo de
 * todo el filtro, no solo de la página.
 */
public record HistoricoDonacionesResponse(
        Page<DonacionResponse> donaciones,
        BigDecimal total,
        List<TotalPorTipo> porTipo) {

    /** Lo de un tipo de donante: cuántas donaciones vigentes y cuánto suman. */
    public record TotalPorTipo(TipoDonante tipoDonante, int cantidad, BigDecimal total) {
    }
}
