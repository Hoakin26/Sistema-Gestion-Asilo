package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.AcompanamientoResponse;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloEnfermeria;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.AcompanamientoService;

/**
 * YS-188. "Mis acompañamientos" de Enfermería: las solicitudes de referencia en
 * que quien pregunta acompaña al interno. Sin parámetros a propósito: el
 * enfermero sale del token, así que no hay cómo pedir las de otro.
 */
@RestController
@RequestMapping("/api/acompanamientos")
public class AcompanamientoController {

    private final AcompanamientoService acompanamientoService;

    public AcompanamientoController(AcompanamientoService acompanamientoService) {
        this.acompanamientoService = acompanamientoService;
    }

    @GetMapping
    @SoloEnfermeria
    public List<AcompanamientoResponse> mios(@AuthenticationPrincipal UsuarioAutenticado enfermero) {
        return acompanamientoService.de(enfermero.getUsuario());
    }
}
