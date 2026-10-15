package app.cabezadealgodon.com.gestionasilobackend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.AsignacionCitaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.CitaResponse;
import app.cabezadealgodon.com.gestionasilobackend.security.MedicoGeneralOFundacion;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorFundacion;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.CitaService;
import jakarta.validation.Valid;

/**
 * HU-06 (YS-91). La cita de una solicitud de referencia: asignarla y
 * consultarla. Vive bajo la solicitud porque una cita no existe sin ella.
 */
@RestController
@RequestMapping("/api/solicitudes-referencia/{solicitudId}/cita")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    /** RF-05: la asigna la Fundación. */
    @PostMapping
    @SoloAdministradorFundacion
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse asignar(@PathVariable Long solicitudId,
            @AuthenticationPrincipal UsuarioAutenticado fundacion,
            @Valid @RequestBody AsignacionCitaRequest request) {
        return citaService.asignar(solicitudId, request, fundacion.getUsuario());
    }

    /** La consultan quienes siguen la solicitud: el Médico General y la Fundación. */
    @GetMapping
    @MedicoGeneralOFundacion
    public CitaResponse consultar(@PathVariable Long solicitudId) {
        return citaService.deSolicitud(solicitudId);
    }
}
