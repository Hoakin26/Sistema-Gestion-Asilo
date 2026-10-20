package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;

/**
 * YS-136. Lo que cuesta un servicio después del descuento de la fundación
 * (HU-19): el original, lo descontado y el neto, con
 * {@code montoNeto = montoOriginal - descuento} y nunca menor que cero.
 */
public record DesgloseCosto(BigDecimal montoOriginal, BigDecimal descuento, BigDecimal montoNeto) {
}
