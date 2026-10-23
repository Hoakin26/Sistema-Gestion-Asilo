package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.util.List;

/**
 * YS-139. Una deuda con la fundación con sus pagos (HU-20), del más reciente
 * al más antiguo, los anulados incluidos. Como {@link HistorialPagosResponse}
 * en la cuenta del familiar, es también lo que responde registrar o anular un
 * pago.
 */
public record HistorialPagosFundacionResponse(CuentaPorPagarResponse cuenta, List<PagoResponse> pagos) {
}
