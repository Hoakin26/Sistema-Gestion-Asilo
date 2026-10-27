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
import app.cabezadealgodon.com.gestionasilobackend.dto.DonacionRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.DonacionResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistoricoDonacionesResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.OrdenNoPermitidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoDonante;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorAsilo;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.DonacionService;
import jakarta.validation.Valid;

/**
 * HU-22 (YS-145). Las donaciones monetarias al asilo (RF-15). Del
 * Administrador del Asilo, el que lleva las finanzas del asilo.
 */
@RestController
@RequestMapping("/api/donaciones")
@SoloAdministradorAsilo
public class DonacionController {

    private final DonacionService donacionService;

    public DonacionController(DonacionService donacionService) {
        this.donacionService = donacionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DonacionResponse registrar(@Valid @RequestBody DonacionRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return donacionService.registrar(request, usuario.getUsuario());
    }

    /**
     * El histórico, filtrable por rango de fechas y por tipo de donante;
     * {@code tipo} se puede repetir ({@code tipo=GOBIERNO&tipo=PARTICULAR}).
     * OBS-01: por páginas de 20, en un orden fijo, la más reciente primera.
     */
    @GetMapping
    public HistoricoDonacionesResponse historico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "tipo", required = false) Set<TipoDonante> tipos,
            @PageableDefault(size = 20) Pageable paginado) {
        return donacionService.historico(desde, hasta, tipos, OrdenNoPermitidoException.validar(paginado, Set.of()));
    }

    /** Anula una donación mal registrada, con su motivo. */
    @PostMapping("/{id}/anulacion")
    public DonacionResponse anular(@PathVariable Long id, @Valid @RequestBody AnulacionPagoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return donacionService.anular(id, request.motivo(), usuario.getUsuario());
    }
}
