package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import app.cabezadealgodon.com.gestionasilobackend.dto.GastoOperativoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.GastoOperativoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistoricoGastosResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.OrdenNoPermitidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoGasto;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorAsilo;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.GastoOperativoService;
import jakarta.validation.Valid;

/**
 * HU-23 (YS-148). Los gastos operativos del asilo (RF-16): luz, agua y los
 * demás egresos de su operación. Del Administrador del Asilo, el que lleva sus
 * finanzas.
 */
@RestController
@RequestMapping("/api/gastos-operativos")
@SoloAdministradorAsilo
public class GastoOperativoController {

    private final GastoOperativoService gastoService;

    public GastoOperativoController(GastoOperativoService gastoService) {
        this.gastoService = gastoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GastoOperativoResponse registrar(@Valid @RequestBody GastoOperativoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gastoService.registrar(request, usuario.getUsuario());
    }

    /**
     * El histórico, filtrable por rango de fechas y por tipo de gasto;
     * {@code tipo} se puede repetir ({@code tipo=LUZ&tipo=AGUA}).
     * OBS-01: por páginas de 20, en un orden fijo, el más reciente primero.
     */
    @GetMapping
    public HistoricoGastosResponse historico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "tipo", required = false) Set<TipoGasto> tipos,
            @PageableDefault(size = 20) Pageable paginado) {
        return gastoService.historico(desde, hasta, tipos, OrdenNoPermitidoException.validar(paginado, Set.of()));
    }

    /** Anula un gasto mal registrado, con su motivo. */
    @PostMapping("/{id}/anulacion")
    public GastoOperativoResponse anular(@PathVariable Long id, @Valid @RequestBody AnulacionPagoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gastoService.anular(id, request.motivo(), usuario.getUsuario());
    }
}
