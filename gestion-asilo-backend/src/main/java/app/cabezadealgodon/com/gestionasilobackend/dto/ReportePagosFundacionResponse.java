package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.models.FormaPago;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;

/**
 * YS-160. El reporte de pagos a la fundación (HU-27, RF-20a): cada pago que el
 * asilo le hizo entre dos fechas, con su fecha, su concepto (consulta,
 * laboratorio o farmacia) y su monto, sin los anulados. Decidido el 22/10:
 * va aparte del reporte de entradas, como los separó la revisión v1.1.
 *
 * <p>
 * {@code periodo} es el mes de la deuda que pagó, que no tiene por qué ser el
 * mes en que se pagó.
 */
public record ReportePagosFundacionResponse(
        LocalDate desde,
        LocalDate hasta,
        List<PagoAFundacion> pagos,
        int cantidadPagos,
        BigDecimal totalPagado,
        List<TotalPorConcepto> porConcepto) {

    /** Un pago a la fundación: cuándo, por qué servicio y de qué mes, cómo se pagó y cuánto. */
    public record PagoAFundacion(Long id, LocalDate fecha, TipoServicio concepto, String periodo, FormaPago formaPago,
            String referencia, BigDecimal monto) {
    }

    /** Lo pagado por un concepto, y en cuántos pagos. */
    public record TotalPorConcepto(TipoServicio concepto, int cantidad, BigDecimal total) {
    }
}
