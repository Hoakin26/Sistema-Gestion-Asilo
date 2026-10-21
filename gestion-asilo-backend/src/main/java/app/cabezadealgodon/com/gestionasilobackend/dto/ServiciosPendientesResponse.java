package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * YS-142. El listado de citas pendientes de pago (HU-21), de la más antigua a
 * la más reciente, con lo que suman entre todas. Los totales se calculan aquí,
 * en {@code BigDecimal}, y no en la pantalla.
 */
public record ServiciosPendientesResponse(
        List<CitaPendienteResponse> citas,
        BigDecimal montoTotal,
        BigDecimal montoPagado,
        BigDecimal saldoPendiente) {
}
