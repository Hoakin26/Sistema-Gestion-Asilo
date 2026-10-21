package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.FormaPago;

/**
 * YS-133. Un pago del historial de la cuenta (HU-34), con quién lo registró.
 * YS-176: si se anuló, con el motivo, quién y cuándo; sin anular, esos tres
 * van vacíos.
 */
public record PagoResponse(
        Long id,
        BigDecimal monto,
        LocalDate fecha,
        FormaPago formaPago,
        String referencia,
        String registradoPor,
        LocalDateTime fechaRegistro,
        boolean anulado,
        String motivoAnulacion,
        String anuladoPor,
        LocalDateTime fechaAnulacion) {
}
