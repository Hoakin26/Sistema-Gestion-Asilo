package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoCuenta;

/**
 * YS-132. Una cuenta mensual, como la lista "Cuentas por cobrar" (HU-18): de
 * quién es, a cargo de quién, cuánto suma y cuánto falta pagar. Solo para el
 * Administrador del Asilo, y sin un dato clínico (RNF-03): ni siquiera el
 * desglose, que está en {@link EstadoCuentaResponse}.
 *
 * <p>
 * YS-133. Con lo pagado y la fecha en que vence (HU-34). El estado es el de
 * hoy (DE-04), con la mora incluida.
 */
public record CuentaMensualResumenResponse(
        Long id,
        ResumenPersona interno,
        ResumenPersona familiar,
        String periodo,
        BigDecimal cuotaBase,
        BigDecimal totalServicios,
        BigDecimal totalDescuentos,
        BigDecimal totalAPagar,
        BigDecimal totalPagado,
        BigDecimal saldo,
        EstadoCuenta estado,
        LocalDate fechaVencimiento,
        LocalDateTime fechaConsolidacion) {
}
