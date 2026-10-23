package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.models.FormaPago;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoDonante;

/**
 * YS-160. El reporte de entradas (HU-27, RF-20b): lo que el asilo recibió
 * entre dos fechas, por tipo de entrada. Las donaciones (HU-22) y los cobros a
 * familiares (HU-34), cada uno con su detalle y su total, y el total de las
 * dos. Sin las donaciones ni los pagos anulados, que no entraron.
 */
public record ReporteEntradasResponse(
        LocalDate desde,
        LocalDate hasta,
        List<DonacionRecibida> donaciones,
        BigDecimal totalDonaciones,
        List<TotalPorDonante> porTipoDonante,
        List<CobroRecibido> cobros,
        BigDecimal totalCobros,
        BigDecimal totalEntradas) {

    /** Una donación recibida: cuándo, de quién y cuánto. */
    public record DonacionRecibida(Long id, LocalDate fecha, String donante, TipoDonante tipoDonante,
            BigDecimal monto) {
    }

    /** Lo donado por un tipo de donante, y en cuántas donaciones. */
    public record TotalPorDonante(TipoDonante tipoDonante, int cantidad, BigDecimal total) {
    }

    /** Un pago del familiar: cuándo, de qué paciente y de qué mes, cómo se pagó y cuánto. */
    public record CobroRecibido(Long id, LocalDate fecha, ResumenPersona paciente, String periodo,
            FormaPago formaPago, BigDecimal monto) {
    }
}
