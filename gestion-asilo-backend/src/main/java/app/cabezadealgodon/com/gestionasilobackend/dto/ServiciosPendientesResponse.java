package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;

/**
 * YS-142. El listado de citas pendientes de pago (HU-21), de la más antigua a
 * la más reciente, con lo que suman entre todas. Los totales se calculan aquí,
 * en {@code BigDecimal}, y no en la pantalla.
 *
 * <p>
 * OBS-01. Las citas llegan por páginas, pero los totales siguen siendo de
 * todas las del filtro, no solo de la página.
 */
public record ServiciosPendientesResponse(
        Page<CitaPendienteResponse> citas,
        BigDecimal montoTotal,
        BigDecimal montoPagado,
        BigDecimal saldoPendiente) {
}
