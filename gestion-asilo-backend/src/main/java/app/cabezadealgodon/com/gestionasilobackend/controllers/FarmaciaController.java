package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.RecetaResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.OrdenNoPermitidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloFarmacia;
import app.cabezadealgodon.com.gestionasilobackend.services.FarmaciaService;

/**
 * HU-15 (YS-123). Las recetas, del lado de la farmacia (RF-11). Todo el
 * controlador es del rol Farmacia: los especialistas consultan las suyas por
 * la visita (HU-10), y ningún otro rol tiene por qué ver qué medicamentos se
 * le recetan a un paciente (RNF-03).
 */
@RestController
@RequestMapping("/api/recetas")
@SoloFarmacia
public class FarmaciaController {

    /** El único campo por el que se puede ordenar la bandeja. */
    private static final Set<String> ORDENES = Set.of("fechaEmision");

    private final FarmaciaService farmaciaService;

    public FarmaciaController(FarmaciaService farmaciaService) {
        this.farmaciaService = farmaciaService;
    }

    /**
     * La bandeja. {@code estado} se puede repetir
     * ({@code ?estado=PENDIENTE_DESPACHO&estado=PARCIALMENTE_ENTREGADA}); sin
     * él, todas. {@code paciente} busca por nombre o DPI. Por defecto, de la
     * más antigua a la más reciente: la que más tiempo lleva esperando va
     * primero.
     */
    @GetMapping
    public Page<RecetaResponse> listar(
            @RequestParam(required = false) Set<EstadoReceta> estado,
            @RequestParam(required = false) String paciente,
            @PageableDefault(size = 20, sort = "fechaEmision", direction = Sort.Direction.ASC) Pageable paginado) {
        return farmaciaService.recetas(estado, paciente, OrdenNoPermitidoException.validar(paginado, ORDENES));
    }
}
