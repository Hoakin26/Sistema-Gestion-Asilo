package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoCuenta;
import app.cabezadealgodon.com.gestionasilobackend.models.FormaPago;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;

/**
 * YS-157. El reporte de cobros (HU-26, RF-19). Decidido el 22/10: el rango es
 * de los pagos recibidos. Cada paciente trae lo que su familiar pagó en esas
 * fechas y las cuentas mensuales a las que se aplicaron esos pagos, con el
 * detalle de los gastos médicos de cada una. El total cobrado es la suma de
 * los pagos, sin los anulados.
 *
 * <p>
 * Los gastos van <b>por tipo de servicio y nunca por nombre</b> (RNF-03),
 * como el estado de cuenta: cuántas consultas, exámenes y medicamentos, y
 * cuánto, sin decir cuáles.
 *
 * <p>
 * {@code paciente} es el filtro pedido; vacío, el reporte es de todos.
 */
public record ReporteCobrosResponse(
        LocalDate desde,
        LocalDate hasta,
        ResumenPersona paciente,
        List<PacienteCobros> pacientes,
        int cantidadPagos,
        BigDecimal totalCobrado,
        List<TotalPorForma> porFormaPago) {

    /** Lo cobrado a un paciente en el período: sus pagos, las cuentas que cubren y el total. */
    public record PacienteCobros(ResumenPersona paciente, List<PagoCobrado> pagos, List<CuentaCobrada> cuentas,
            BigDecimal totalCobrado) {
    }

    /** Un pago recibido: su fecha, la cuenta (el mes) a la que se aplicó, cómo se pagó y cuánto. */
    public record PagoCobrado(Long id, LocalDate fecha, String periodo, FormaPago formaPago, String referencia,
            BigDecimal monto) {
    }

    /**
     * Una cuenta mensual a la que se aplicó algún pago del período: a cargo de
     * quién, la cuota base y los gastos médicos por tipo, el descuento y el
     * total; lo que se cobró de ella en el período, lo pagado hasta hoy, el
     * saldo y el estado.
     */
    public record CuentaCobrada(
            Long id,
            String periodo,
            String familiar,
            BigDecimal cuotaBase,
            List<GastoMedico> gastos,
            BigDecimal totalDescuentos,
            BigDecimal totalAPagar,
            BigDecimal cobradoEnPeriodo,
            BigDecimal totalPagado,
            BigDecimal saldo,
            EstadoCuenta estado) {
    }

    /** Los gastos médicos de un tipo en una cuenta: cuántos, el costo original, el descuento y el neto. */
    public record GastoMedico(TipoServicio tipo, int cantidad, BigDecimal original, BigDecimal descuento,
            BigDecimal neto) {
    }

    /** Lo cobrado con una forma de pago, y en cuántos pagos. */
    public record TotalPorForma(FormaPago formaPago, int cantidad, BigDecimal total) {
    }
}
