package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.ServiciosPendientesResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.OrdenNoPermitidoException;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorAsilo;
import app.cabezadealgodon.com.gestionasilobackend.services.ServicioPendienteService;

/**
 * HU-21 (YS-142). Las citas que el familiar todavía no termina de pagar
 * (RF-14), para darles seguimiento. Del Administrador del Asilo, el que cobra,
 * como la cuenta del familiar.
 */
@RestController
@RequestMapping("/api/servicios-pendientes")
@SoloAdministradorAsilo
public class ServicioPendienteController {

    private final ServicioPendienteService servicioPendienteService;

    public ServicioPendienteController(ServicioPendienteService servicioPendienteService) {
        this.servicioPendienteService = servicioPendienteService;
    }

    /**
     * Filtrable por paciente (nombre o DPI) y por rango de fechas de la cita,
     * las dos incluidas. Sin filtros, todas las pendientes. OBS-01: por
     * páginas de 20, en un orden fijo, la más antigua primero.
     */
    @GetMapping
    public ServiciosPendientesResponse pendientes(@RequestParam(required = false) String paciente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable paginado) {
        return servicioPendienteService.pendientes(paciente, desde, hasta,
                OrdenNoPermitidoException.validar(paginado, Set.of()));
    }
}
