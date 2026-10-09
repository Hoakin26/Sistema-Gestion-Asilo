package app.cabezadealgodon.com.gestionasilobackend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.SolicitudReferenciaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.SolicitudReferenciaResponse;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloMedicoGeneral;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.SolicitudReferenciaService;
import jakarta.validation.Valid;

/**
 * HU-03 (YS-84). Las solicitudes de referencia médica (RF-03).
 *
 * <p>
 * Como el de internos, no lleva anotación de rol en la clase: con HU-05 se le
 * suma el listado, que también consulta la fundación, y con HU-06 la
 * asignación de cita, que es solo de ella.
 */
@RestController
@RequestMapping("/api/solicitudes-referencia")
public class SolicitudReferenciaController {

    private final SolicitudReferenciaService solicitudService;

    public SolicitudReferenciaController(SolicitudReferenciaService solicitudService) {
        this.solicitudService = solicitudService;
    }

    /**
     * RF-03: la crea el Médico General. Enfermería no: es la acompañante
     * asignada, no quien refiere (corrección del 08/10 en YS-84).
     */
    @PostMapping
    @SoloMedicoGeneral
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudReferenciaResponse crear(@AuthenticationPrincipal UsuarioAutenticado medico,
            @Valid @RequestBody SolicitudReferenciaRequest request) {
        return solicitudService.crear(request, medico.getUsuario());
    }
}
