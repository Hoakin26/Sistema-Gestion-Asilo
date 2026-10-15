package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadTarifaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.TarifaRequest;
import app.cabezadealgodon.com.gestionasilobackend.security.MedicoGeneralOFundacion;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorFundacion;
import app.cabezadealgodon.com.gestionasilobackend.services.EspecialidadService;
import jakarta.validation.Valid;

/**
 * YS-84. El catálogo de especialidades, para el selector de la solicitud.
 * YS-169. Y la tarifa de consulta de cada una, que define la Fundación.
 */
@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    public EspecialidadController(EspecialidadService especialidadService) {
        this.especialidadService = especialidadService;
    }

    @GetMapping
    @MedicoGeneralOFundacion
    public List<EspecialidadResponse> listar() {
        return especialidadService.listarActivas();
    }

    @GetMapping("/tarifas")
    @SoloAdministradorFundacion
    public List<EspecialidadTarifaResponse> listarTarifas() {
        return especialidadService.listarTarifas();
    }

    @PutMapping("/{id}/tarifa")
    @SoloAdministradorFundacion
    public EspecialidadTarifaResponse actualizarTarifa(@PathVariable Long id,
            @Valid @RequestBody TarifaRequest request) {
        return especialidadService.actualizarTarifa(id, request);
    }
}
