package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.cabezadealgodon.com.gestionasilobackend.dto.ArchivoDescargable;
import app.cabezadealgodon.com.gestionasilobackend.dto.OrdenLaboratorioResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.OrdenNoPermitidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.security.LaboratorioOMedico;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.LaboratorioService;

/**
 * HU-12 (YS-114). Las órdenes de laboratorio, del lado del laboratorio (RF-09).
 * Todo el controlador es del rol Laboratorio: los especialistas consultan las
 * suyas por la visita (HU-11), y ningún otro rol tiene por qué ver qué
 * exámenes se le piden a un paciente. La única excepción es la descarga del
 * archivo de un resultado (HU-13), que también es de los médicos.
 */
@RestController
@RequestMapping("/api/ordenes-laboratorio")
@SoloLaboratorio
public class LaboratorioController {

    /**
     * Los campos por los que se puede ordenar la bandeja. YS-118: el registro
     * de resultados va del más reciente al más antiguo.
     */
    private static final Set<String> ORDENES = Set.of("fechaOrden", "fechaResultado");

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

    /**
     * HU-13 (YS-117). El resultado del examen: {@code resultado} en texto,
     * obligatorio, y {@code archivo} opcional (PDF, JPG o PNG, hasta 10 MB).
     * La orden pasa a Resultados Cargados.
     */
    @PostMapping(path = "/{id}/resultado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OrdenLaboratorioResponse registrarResultado(@PathVariable Long id,
            @RequestParam(required = false) String resultado,
            @RequestPart(name = "archivo", required = false) MultipartFile archivo,
            @AuthenticationPrincipal UsuarioAutenticado laboratorista) {
        return laboratorioService.registrarResultado(id, resultado, archivo, laboratorista.getUsuario());
    }

    /**
     * HU-13 (YS-117). El archivo del resultado, con el nombre con que se
     * subió. Como descarga y no para abrirse dentro de la página: así el
     * navegador nunca lo interpreta como parte del sistema.
     */
    @GetMapping("/{id}/archivo")
    @LaboratorioOMedico
    public ResponseEntity<Resource> archivo(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ArchivoDescargable archivo = laboratorioService.archivo(id, usuario.getUsuario());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipo()))
                .contentLength(archivo.tamano())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(archivo.contenido());
    }
}
