package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoGasto;

/**
 * YS-148. Un gasto del histórico (HU-23), con quién lo registró. Si se anuló,
 * con el motivo, quién y cuándo; sin anular, esos tres van vacíos.
 */
public record GastoOperativoResponse(
        Long id,
        TipoGasto tipoGasto,
        String descripcion,
        BigDecimal monto,
        LocalDate fecha,
        String registradoPor,
        LocalDateTime fechaRegistro,
        boolean anulado,
        String motivoAnulacion,
        String anuladoPor,
        LocalDateTime fechaAnulacion) {
}
