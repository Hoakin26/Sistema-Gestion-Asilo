package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloMedicoGeneral;
import app.cabezadealgodon.com.gestionasilobackend.services.UsuarioService;

/**
 * YS-84. Los enfermeros que pueden acompañar a un interno referido.
 *
 * <p>
 * Es un endpoint aparte y no un filtro de {@code /api/usuarios} porque ese es
 * del Administrador del Asilo y devuelve correos, roles y estados. El Médico
 * General solo necesita elegir a una persona: aquí recibe el id y el nombre,
 * nada más.
 */
@RestController
@RequestMapping("/api/enfermeros")
public class EnfermeroController {

    private final UsuarioService usuarioService;

    public EnfermeroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @SoloMedicoGeneral
    public List<ResumenPersona> listarActivos() {
        return usuarioService.enfermerosActivos();
    }
}
