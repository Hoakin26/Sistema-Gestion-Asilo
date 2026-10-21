package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoDonante;

/**
 * YS-145. Una donación del histórico (HU-22), con quién la registró. Si se
 * anuló, con el motivo, quién y cuándo; sin anular, esos tres van vacíos.
 */
public record DonacionResponse(
        Long id,
        BigDecimal monto,
        LocalDate fecha,
        String donante,
        TipoDonante tipoDonante,
        String registradaPor,
        LocalDateTime fechaRegistro,
        boolean anulada,
        String motivoAnulacion,
        String anuladaPor,
        LocalDateTime fechaAnulacion) {
}
