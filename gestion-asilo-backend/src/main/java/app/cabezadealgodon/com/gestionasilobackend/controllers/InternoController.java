package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.CuotaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.FichaMedicaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.FichaMedicaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistorialMedicoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.InternoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.InternoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.InternoResumenResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.OrdenNoPermitidoException;
import app.cabezadealgodon.com.gestionasilobackend.security.PersonalClinico;
import app.cabezadealgodon.com.gestionasilobackend.security.PersonalConAccesoAInternos;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorAsilo;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloMedicoGeneral;
import app.cabezadealgodon.com.gestionasilobackend.services.HistorialMedicoService;
import app.cabezadealgodon.com.gestionasilobackend.services.InternoService;
import jakarta.validation.Valid;

/**
 * HU-01 (YS-58). El expediente de un interno.
 *
 * <p>
 * <b>Aquí conviven dos niveles de acceso y conviene no confundirlos.</b> Los
 * endpoints del expediente administrativo los usan también el Administrador del
 * Asilo y enfermería; los de la ficha médica quedan fuera de su alcance por el
 * RNF-03. Por eso este controlador no lleva una anotación de rol en la clase:
 * una sola regla para todo serviría de más a unos endpoints o de menos a otros,
 * y ambos errores son graves aquí.
 *
 * <p>
 * Lo que sí sigue valiendo de YS-53 es que ningún método puede quedarse sin
 * anotación: un endpoint sin restricción queda abierto a los siete roles y eso
 * no produce ningún síntoma visible.
 */
@RestController
@RequestMapping("/api/internos")
public class InternoController {

    /*
     * YS-166. Los campos por los que se puede ordenar la lista. Desde YS-62 la
     * búsqueda es una consulta propia, y un orden por un campo inexistente
     * salía como 500 en vez de 400.
     */
    private static final Set<String> ORDENES = Set.of("nombreCompleto", "fechaIngreso", "dpi");

    private final InternoService internoService;
    private final HistorialMedicoService historialMedicoService;

    public InternoController(InternoService internoService, HistorialMedicoService historialMedicoService) {
        this.internoService = internoService;
        this.historialMedicoService = historialMedicoService;
    }

    /**
     * Paginado desde el primer momento: la lista crece con cada ingreso y
     * agregar paginación después obliga a cambiar el contrato y la pantalla.
     */
    @GetMapping
    @PersonalConAccesoAInternos
    public Page<InternoResumenResponse> listar(
            @RequestParam(required = false) String busqueda,
            @PageableDefault(size = 20, sort = "nombreCompleto") Pageable paginado) {
        return internoService.listar(busqueda, OrdenNoPermitidoException.validar(paginado, ORDENES));
    }

    @GetMapping("/{id}")
    @PersonalConAccesoAInternos
    public InternoResponse obtener(@PathVariable Long id) {
        return internoService.obtener(id);
    }

    @PostMapping
    @SoloMedicoGeneral
    @ResponseStatus(HttpStatus.CREATED)
    public InternoResponse registrar(@Valid @RequestBody InternoRequest request) {
        return internoService.registrar(request);
    }

    @PutMapping("/{id}")
    @SoloMedicoGeneral
    public InternoResponse actualizar(@PathVariable Long id, @Valid @RequestBody InternoRequest request) {
        return internoService.actualizar(id, request);
    }

    /**
     * HU-18 (YS-132). La cuota base mensual, que fija el Administrador del
     * Asilo: es un dato financiero (RNF-02), y la consolidación de la cuenta
     * del familiar la suma a los cargos del mes.
     */
    @PutMapping("/{id}/cuota")
    @SoloAdministradorAsilo
    public InternoResponse actualizarCuota(@PathVariable Long id, @Valid @RequestBody CuotaRequest request) {
        return internoService.actualizarCuota(id, request.cuotaMensual());
    }

    /**
     * RNF-03. {@code @PersonalClinico} deja fuera al Administrador del Asilo:
     * es el único endpoint del expediente al que no puede entrar, y es
     * exactamente el que devuelve psicopatologías y medicamentos.
     */
    @GetMapping("/{id}/ficha-medica")
    @PersonalClinico
    public FichaMedicaResponse obtenerFicha(@PathVariable Long id) {
        return internoService.obtenerFicha(id);
    }

    @PutMapping("/{id}/ficha-medica")
    @SoloMedicoGeneral
    public FichaMedicaResponse actualizarFicha(@PathVariable Long id,
            @Valid @RequestBody FichaMedicaRequest request) {
        return internoService.actualizarFicha(id, request);
    }

    /**
     * HU-02 (YS-64). El historial médico centralizado (RF-02): todas las
     * visitas del interno con su diagnóstico, y lo que se le recetó y ordenó
     * en cada una. Los padecimientos y los medicamentos habituales siguen en
     * la ficha médica, que la pantalla pide aparte.
     *
     * <p>
     * Solo el Médico General. El especialista ve los antecedentes dentro de
     * su visita activa y no fuera de ella (HU-08), enfermería trabaja con la
     * ficha, y el Administrador del Asilo no lee datos clínicos (RNF-03).
     */
    @GetMapping("/{id}/historial")
    @SoloMedicoGeneral
    public HistorialMedicoResponse obtenerHistorial(@PathVariable Long id) {
        return historialMedicoService.delInterno(id);
    }
}
