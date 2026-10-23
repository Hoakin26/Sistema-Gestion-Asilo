package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.time.YearMonth;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.AnulacionPagoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.CuentaPorPagarResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistorialPagosFundacionResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.PagoRequest;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorAsilo;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.CuentaPorPagarService;
import jakarta.validation.Valid;

/**
 * HU-20 (YS-139). Las cuentas por pagar del asilo a la fundación (RF-13): lo
 * que le debe por servicio y por mes, y los pagos que le hace. Del
 * Administrador del Asilo, el que lleva las finanzas del asilo; la deuda no
 * dice nada de ningún interno, solo cuántos servicios y cuánto.
 */
@RestController
@RequestMapping("/api")
@SoloAdministradorAsilo
public class CuentaPorPagarController {

    private final CuentaPorPagarService cuentaPorPagarService;

    public CuentaPorPagarController(CuentaPorPagarService cuentaPorPagarService) {
        this.cuentaPorPagarService = cuentaPorPagarService;
    }

    /** El histórico: lo adeudado, lo pagado y el estado de cada mes y servicio. */
    @GetMapping("/cuentas-por-pagar")
    public List<CuentaPorPagarResponse> historico() {
        return cuentaPorPagarService.historico();
    }

    /** La deuda de un servicio en un mes ("2026-10/CONSULTA"), con sus pagos. */
    @GetMapping("/cuentas-por-pagar/{periodo}/{tipoServicio}/pagos")
    public HistorialPagosFundacionResponse pagos(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @PathVariable TipoServicio tipoServicio) {
        return cuentaPorPagarService.historial(periodo, tipoServicio);
    }

    /** Registra un pago a la fundación sobre esa deuda. */
    @PostMapping("/cuentas-por-pagar/{periodo}/{tipoServicio}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    public HistorialPagosFundacionResponse registrarPago(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @PathVariable TipoServicio tipoServicio, @Valid @RequestBody PagoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return cuentaPorPagarService.registrar(periodo, tipoServicio, request, usuario.getUsuario());
    }

    /** Anula un pago a la fundación mal registrado, con su motivo. */
    @PostMapping("/pagos-fundacion/{id}/anulacion")
    public HistorialPagosFundacionResponse anularPago(@PathVariable Long id,
            @Valid @RequestBody AnulacionPagoRequest request, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return cuentaPorPagarService.anular(id, request.motivo(), usuario.getUsuario());
    }
}
