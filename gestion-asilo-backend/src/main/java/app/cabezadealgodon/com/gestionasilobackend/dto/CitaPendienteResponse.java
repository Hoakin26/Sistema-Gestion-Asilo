package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoPagoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;

/**
 * YS-142. Una cita que el familiar todavía no termina de pagar (HU-21, RF-14):
 * el paciente, la fecha, lo que costó con descuento, lo pagado, el saldo y el
 * estado. {@code servicios} es lo que generó la cita por tipo de servicio, con
 * cuántos y cuánto: por tipo y nunca por nombre, sin especialidad, examen ni
 * medicamento (RNF-03).
 *
 * <p>
 * {@code sinConsolidar}: algún servicio suyo es de un mes que no se ha
 * consolidado, así que todavía no está en la cuenta del familiar.
 * {@code vencida}: alguna de sus cuentas está en mora (YS-176).
 */
public record CitaPendienteResponse(
        Long visitaId,
        String numeroSolicitud,
        LocalDate fecha,
        ResumenPersona paciente,
        List<Servicio> servicios,
        BigDecimal montoTotal,
        BigDecimal montoPagado,
        BigDecimal saldo,
        EstadoPagoServicio estado,
        boolean sinConsolidar,
        boolean vencida) {

    /** Lo de un tipo de servicio en la cita: cuántos hubo y cuánto suman, con descuento. */
    public record Servicio(TipoServicio tipo, int cantidad, BigDecimal monto) {
    }
}
