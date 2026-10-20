package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;

/**
 * YS-132. El estado de cuenta de un interno entre dos fechas (HU-18): lo que
 * se le cobra al familiar, desglosado por servicio, con el detalle de cada
 * cargo y las cuentas mensuales que caen en el rango. Solo para el
 * Administrador del Asilo.
 *
 * <p>
 * <b>Por tipo de servicio y nunca por nombre</b> (diseño, 4.9): una línea que
 * dijera "Consulta de Psiquiatría" o el nombre de un antipsicótico revelaría el
 * padecimiento del interno sin mostrar un diagnóstico. Por eso
 * {@link Cargo} lleva el tipo, la fecha, el número de solicitud y los montos, y
 * nada más: ni la especialidad, ni el examen, ni el medicamento (RNF-03).
 *
 * <p>
 * Los totales son los del rango: las cuotas de los meses ya consolidados que
 * caen en él más los cargos de esas fechas. {@code saldoTotal} es otra cosa: lo
 * que el familiar debe por todas las cuentas del interno, en cualquier mes.
 */
public record EstadoCuentaResponse(
        Long internoId,
        LocalDate desde,
        LocalDate hasta,
        List<Subtotal> desglose,
        List<Cargo> cargos,
        List<CuentaMensualResumenResponse> cuentas,
        BigDecimal totalOriginal,
        BigDecimal totalDescuentos,
        BigDecimal totalAPagar,
        BigDecimal saldoTotal,
        int cargosSinConsolidar) {

    /**
     * Lo de un concepto: la cuota ("CUOTA") o un tipo de servicio
     * ("CONSULTA", "LABORATORIO", "FARMACIA"), con cuántos hubo y cuánto suman.
     */
    public record Subtotal(String concepto, int cantidad, BigDecimal original, BigDecimal descuento,
            BigDecimal neto) {
    }

    /**
     * Un cargo, sin nada clínico. {@code periodo} es la cuenta mensual en que
     * se consolidó; vacío si su mes todavía no se consolida.
     */
    public record Cargo(Long id, LocalDate fecha, TipoServicio tipo, String numeroSolicitud,
            BigDecimal montoOriginal, BigDecimal descuento, BigDecimal montoNeto, String periodo) {
    }
}
