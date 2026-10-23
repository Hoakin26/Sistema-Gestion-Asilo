package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.util.List;

/**
 * YS-133. Una cuenta mensual con su historial de pagos (HU-34), del más
 * reciente al más antiguo, los anulados incluidos. Es también lo que responde
 * registrar o anular un pago: la pantalla recibe de una vez el saldo y el
 * estado nuevos.
 */
public record HistorialPagosResponse(CuentaMensualResumenResponse cuenta, List<PagoResponse> pagos) {
}
