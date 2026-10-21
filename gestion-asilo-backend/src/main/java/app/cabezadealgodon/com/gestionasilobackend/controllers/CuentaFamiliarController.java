package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.AnulacionPagoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.CuentaMensualResumenResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.EstadoCuentaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistorialPagosResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.PagoRequest;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorAsilo;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.CuentaFamiliarService;
import app.cabezadealgodon.com.gestionasilobackend.services.PagoService;
import jakarta.validation.Valid;

/**
 * HU-18 (YS-132). La cuenta del familiar (RF-12): las cuentas mensuales y el
 * estado de cuenta de cada interno. Todo es del Administrador del Asilo, el
 * que cobra; ningún rol clínico tiene por qué ver lo que paga una familia, ni
 * la fundación el detalle de cada interno. HU-34 (YS-133, YS-176) le agrega
 * los pagos del familiar y su anulación (RF-22).
 */
@RestController
@RequestMapping("/api")
@SoloAdministradorAsilo
public class CuentaFamiliarController {

    private final CuentaFamiliarService cuentaService;
    private final PagoService pagoService;

    public CuentaFamiliarController(CuentaFamiliarService cuentaService, PagoService pagoService) {
        this.cuentaService = cuentaService;
        this.pagoService = pagoService;
    }

    /**
     * HU-34 (YS-133). Registra un pago del familiar sobre una cuenta mensual.
     * Responde la cuenta con su saldo y su estado nuevos, y el historial.
     */
    @PostMapping("/cuentas/{id}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    public HistorialPagosResponse registrarPago(@PathVariable Long id, @Valid @RequestBody PagoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pagoService.registrar(id, request, usuario.getUsuario());
    }

    /** HU-34 (YS-133). El historial de pagos de una cuenta, los anulados incluidos. */
    @GetMapping("/cuentas/{id}/pagos")
    public HistorialPagosResponse pagos(@PathVariable Long id) {
        return pagoService.historial(id);
    }

    /** HU-34 (YS-176). Anula un pago mal registrado, con su motivo. */
    @PostMapping("/pagos/{id}/anulacion")
    public HistorialPagosResponse anularPago(@PathVariable Long id, @Valid @RequestBody AnulacionPagoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pagoService.anular(id, request.motivo(), usuario.getUsuario());
    }

    /** Las cuentas ya consolidadas de un mes ("2026-10"); sin él, las del mes en curso. */
    @GetMapping("/cuentas")
    public List<CuentaMensualResumenResponse> cuentas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo) {
        return cuentaService.cuentas(periodo == null ? YearMonth.now() : periodo);
    }

    /** Consolida el mes: crea o recalcula la cuenta de cada interno. */
    @PostMapping("/cuentas/consolidar")
    public List<CuentaMensualResumenResponse> consolidar(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo) {
        return cuentaService.consolidar(periodo);
    }

    /**
     * El estado de cuenta de un interno entre dos fechas. Sin ellas, desde el
     * primer día del mes en curso hasta hoy.
     */
    @GetMapping("/internos/{id}/estado-cuenta")
    public EstadoCuentaResponse estadoCuenta(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        return cuentaService.estadoCuenta(id, desde == null ? hoy.withDayOfMonth(1) : desde, hasta == null ? hoy : hasta);
    }
}
