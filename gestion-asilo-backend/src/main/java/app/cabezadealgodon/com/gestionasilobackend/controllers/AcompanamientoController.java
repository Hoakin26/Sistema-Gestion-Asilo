package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.AcompanamientoResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.OrdenNoPermitidoException;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloEnfermeria;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.AcompanamientoService;

/**
 * YS-188. "Mis acompañamientos" de Enfermería: las solicitudes de referencia en
 * que quien pregunta acompaña al interno. Sin filtros a propósito: el
 * enfermero sale del token, así que no hay cómo pedir las de otro.
 *
 * <p>
 * OBS-01. Una dirección por pestaña: los próximos llegan enteros y los
 * anteriores, que crecen con cada mes, por páginas.
 */
@RestController
@RequestMapping("/api/acompanamientos")
@SoloEnfermeria
public class AcompanamientoController {

    private final AcompanamientoService acompanamientoService;

    public AcompanamientoController(AcompanamientoService acompanamientoService) {
        this.acompanamientoService = acompanamientoService;
    }

    @GetMapping("/proximos")
    public List<AcompanamientoResponse> proximos(@AuthenticationPrincipal UsuarioAutenticado enfermero) {
        return acompanamientoService.proximos(enfermero.getUsuario());
    }

    /** El orden es fijo, el más reciente primero: no se acepta otro. */
    @GetMapping("/anteriores")
    public Page<AcompanamientoResponse> anteriores(@AuthenticationPrincipal UsuarioAutenticado enfermero,
            @PageableDefault(size = 20) Pageable paginado) {
        return acompanamientoService.anteriores(enfermero.getUsuario(),
                OrdenNoPermitidoException.validar(paginado, Set.of()));
    }
}
