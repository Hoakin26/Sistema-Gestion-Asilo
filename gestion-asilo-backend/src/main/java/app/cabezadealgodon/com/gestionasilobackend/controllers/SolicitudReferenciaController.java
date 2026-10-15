package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.SolicitudReferenciaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.SolicitudReferenciaResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.OrdenNoPermitidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.security.MedicoGeneralOFundacion;
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

    /** YS-166. Los campos por los que se puede ordenar el listado. */
    private static final Set<String> ORDENES = Set.of("fechaCreacion", "numero");

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

    /**
     * HU-05 (YS-87). Sin {@code estado} devuelve todas; la bandeja de la
     * Fundación pide {@code estado=PENDIENTE_ASIGNACION}. El orden por
     * defecto es de la más antigua a la más reciente; las pantallas piden
     * {@code sort=fechaCreacion,desc} para ver primero las más recientes.
     *
     * <p>
     * YS-166. {@code busqueda} filtra por número, interno o especialidad.
     *
     * <p>
     * La consultan quienes participan en la referencia: el Médico General
     * para darle seguimiento y la Fundación para asignar la cita.
     */
    @GetMapping
    @MedicoGeneralOFundacion
    public Page<SolicitudReferenciaResponse> listar(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) String busqueda,
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.ASC) Pageable paginado) {
        return solicitudService.listar(estado, busqueda, OrdenNoPermitidoException.validar(paginado, ORDENES));
    }
}
