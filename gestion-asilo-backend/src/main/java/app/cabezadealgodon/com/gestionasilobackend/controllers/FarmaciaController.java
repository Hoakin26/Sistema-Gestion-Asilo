package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.DespachoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.RecetaResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.OrdenNoPermitidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloFarmacia;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.FarmaciaService;
import jakarta.validation.Valid;

/**
 * HU-15 (YS-123). Las recetas, del lado de la farmacia (RF-11). Todo el
 * controlador es del rol Farmacia: los especialistas consultan las suyas por
 * la visita (HU-10), y ningún otro rol tiene por qué ver qué medicamentos se
 * le recetan a un paciente (RNF-03). HU-16 (YS-126) le agrega el despacho.
 */
@RestController
@RequestMapping("/api/recetas")
@SoloFarmacia
public class FarmaciaController {

    /**
     * Los campos por los que se puede ordenar la bandeja. YS-126: el historial
     * de despachos va del más reciente al más antiguo (decidido el 19/10).
     */
    private static final Set<String> ORDENES = Set.of("fechaEmision", "fechaDespacho");

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

    /**
     * HU-16 (YS-126). Despacha la receta: entrega los medicamentos de
     * {@code entregados} y deja pendientes los demás. La receta pasa a
     * Entregada, Parcialmente Entregada o Medicamento Agotado (DE-03).
     */
    @PostMapping("/{id}/despacho")
    public RecetaResponse despachar(@PathVariable Long id, @Valid @RequestBody DespachoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado farmaceutico) {
        return farmaciaService.despachar(id, request, farmaceutico.getUsuario());
    }
}
