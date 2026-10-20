package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoCuenta;

/**
 * YS-132. Una cuenta mensual, como la lista "Cuentas por cobrar" (HU-18): de
 * quién es, a cargo de quién, cuánto suma y cuánto falta pagar. Solo para el
 * Administrador del Asilo, y sin un dato clínico (RNF-03): ni siquiera el
 * desglose, que está en {@link EstadoCuentaResponse}.
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
        BigDecimal saldo,
        EstadoCuenta estado,
        LocalDateTime fechaConsolidacion) {
}
