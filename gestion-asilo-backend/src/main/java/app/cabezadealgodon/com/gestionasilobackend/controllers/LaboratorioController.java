package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.OrdenLaboratorioResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.OrdenNoPermitidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.LaboratorioService;

/**
 * HU-12 (YS-114). Las órdenes de laboratorio, del lado del laboratorio (RF-09).
 * Todo el controlador es del rol Laboratorio: los especialistas consultan las
 * suyas por la visita (HU-11), y ningún otro rol tiene por qué ver qué
 * exámenes se le piden a un paciente.
 */
@RestController
@RequestMapping("/api/ordenes-laboratorio")
@SoloLaboratorio
public class LaboratorioController {

    /** Los campos por los que se puede ordenar la bandeja. */
    private static final Set<String> ORDENES = Set.of("fechaOrden");

    private final LaboratorioService laboratorioService;

    public LaboratorioController(LaboratorioService laboratorioService) {
        this.laboratorioService = laboratorioService;
    }

    /**
     * La bandeja. {@code estado} se puede repetir
     * ({@code ?estado=PENDIENTE&estado=EN_PROCESO}) para ver juntas las que
     * faltan por atender; sin él, todas. {@code paciente} busca por nombre o
     * DPI. Por defecto, de la más antigua a la más reciente: la que más
     * tiempo lleva esperando va primero.
     */
    @GetMapping
    public Page<OrdenLaboratorioResponse> listar(
            @RequestParam(required = false) Set<EstadoOrden> estado,
            @RequestParam(required = false) String paciente,
            @PageableDefault(size = 20, sort = "fechaOrden", direction = Sort.Direction.ASC) Pageable paginado) {
        return laboratorioService.ordenes(estado, paciente, OrdenNoPermitidoException.validar(paginado, ORDENES));
    }

    /** DE-02. El laboratorio toma la orden: de Pendiente a En Proceso. */
    @PatchMapping("/{id}/iniciar")
    public OrdenLaboratorioResponse iniciar(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado laboratorista) {
        return laboratorioService.iniciar(id, laboratorista.getUsuario());
    }
}
