package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoGasto;

/**
 * YS-148. El histórico de gastos con los filtros pedidos (HU-23), y lo que
 * suman en el período: en total y por tipo de gasto, sin los anulados. Los
 * totales se calculan aquí, en {@code BigDecimal}, y no en la pantalla.
 *
 * <p>
 * OBS-01. Los gastos llegan por páginas, pero los totales siguen siendo de
 * todo el filtro, no solo de la página.
 */
public record HistoricoGastosResponse(
        Page<GastoOperativoResponse> gastos,
        BigDecimal total,
        List<TotalPorTipo> porTipo) {

    /** Lo de un tipo de gasto: cuántos gastos vigentes y cuánto suman. */
    public record TotalPorTipo(TipoGasto tipoGasto, int cantidad, BigDecimal total) {
    }
}
