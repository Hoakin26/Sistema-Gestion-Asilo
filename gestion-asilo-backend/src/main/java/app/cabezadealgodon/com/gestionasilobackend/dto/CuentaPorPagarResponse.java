package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoCuentaPorPagar;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;

/**
 * YS-139. Lo que el asilo le debe a la fundación por un servicio en un mes
 * (HU-20): cuántos servicios hubo, lo adeudado (el neto de sus cargos), lo
 * pagado, el saldo y el estado. Por tipo de servicio y nunca por nombre, como
 * todo lo del Administrador del Asilo (RNF-03).
 */
public record CuentaPorPagarResponse(
        String periodo,
        TipoServicio tipoServicio,
        long cantidadServicios,
        BigDecimal totalAdeudado,
        BigDecimal totalPagado,
        BigDecimal saldo,
        EstadoCuentaPorPagar estado) {
}
