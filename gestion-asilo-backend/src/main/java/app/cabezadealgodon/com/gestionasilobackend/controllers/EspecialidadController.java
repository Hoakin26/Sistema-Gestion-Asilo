package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.security.MedicoGeneralOFundacion;
import app.cabezadealgodon.com.gestionasilobackend.services.EspecialidadService;

/** YS-84. El catálogo de especialidades, para el selector de la solicitud. */
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
}
