package app.cabezadealgodon.com.gestionasilobackend.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.DespachoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.MedicamentoEntregadoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.RecetaResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.ConflictoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.DetalleReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.Receta;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.RecetaRepository;

/**
 * YS-123. El trabajo de la farmacia sobre las recetas que emiten los
 * especialistas (RF-11). HU-15 es la bandeja; HU-16 y HU-17 le agregan el
 * despacho y su costo.
 */
@Service
public class FarmaciaService {

    private final RecetaRepository recetaRepository;

    public FarmaciaService(RecetaRepository recetaRepository) {
        this.recetaRepository = recetaRepository;
    }

    /**
     * HU-15. Las recetas en alguno de {@code estados} (sin ninguno, todas),
     * filtradas por el nombre o el DPI del paciente, cada una con sus
     * medicamentos. Nada del diagnóstico: la farmacia despacha la receta, no
     * la consulta (mínimo privilegio, sección 5 del diseño).
     *
     * <p>
     * Se desempata por id, como en la bandeja del laboratorio: sin eso, dos
     * recetas con la misma fecha podían repetirse o perderse al pasar de
     * página.
     */
    @Transactional(readOnly = true)
    public Page<RecetaResponse> recetas(Set<EstadoReceta> estados, String paciente, Pageable paginado) {
        Set<EstadoReceta> filtro = estados == null || estados.isEmpty() ? EnumSet.allOf(EstadoReceta.class) : estados;
        String patron = PatronBusqueda.contiene(PatronBusqueda.normalizado(paciente));
        Pageable estable = PageRequest.of(paginado.getPageNumber(), paginado.getPageSize(),
                paginado.getSort().and(Sort.by("id")));
        return recetaRepository.bandeja(filtro, patron, estable).map(MapeoReceta::receta);
    }

    /**
     * HU-16 (YS-126). Un despacho: la farmacia entrega los medicamentos de
     * {@code request} y los demás quedan pendientes, como pide CU-005 cuando
     * falta alguno (E.1). El estado sale de lo entregado en total (DE-03):
     * todos, Entregada; algunos, Parcialmente Entregada; ninguno, Medicamento
     * Agotado. Quedan anotados la fecha y quién despachó.
     *
     * <p>
     * Nada se entrega dos veces. Una receta entregada ya no se despacha, y
     * un medicamento ya entregado no se vuelve a entregar: los dos son
     * conflictos con el estado de la receta (409). Un despacho sin entregas
     * solo tiene sentido la primera vez, para dejar constancia de que no había
     * existencias; después no cambiaría nada y se rechaza (400).
     *
     * <p>
     * Si otra persona despacha la misma receta al mismo tiempo, o el
     * especialista la corrige, el bloqueo de {@link RecetaRepository#paraDespachar}
     * hace que la segunda escritura falle (409) en vez de pisar a la primera.
     */
    @Transactional
    public RecetaResponse despachar(Long recetaId, DespachoRequest request, Usuario farmaceutico) {
        Receta receta = recetaRepository.paraDespachar(recetaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una receta con id " + recetaId));
        if (receta.getEstado() == EstadoReceta.ENTREGADA) {
            throw new ConflictoException("Esta receta ya se entregó completa y no se vuelve a despachar");
        }

        List<DetalleReceta> aEntregar = validarEntregas(receta, request.entregados());
        if (aEntregar.isEmpty() && receta.getEstado() != EstadoReceta.PENDIENTE_DESPACHO) {
            throw new CampoInvalidoException("entregados", "Marque al menos un medicamento entregado");
        }

        LocalDateTime ahora = LocalDateTime.now();
        aEntregar.forEach(detalle -> detalle.entregar(ahora));
        long entregados = receta.getMedicamentos().stream().filter(DetalleReceta::isEntregado).count();
        receta.cambiarEstado(entregados == receta.getMedicamentos().size() ? EstadoReceta.ENTREGADA
                : entregados > 0 ? EstadoReceta.PARCIALMENTE_ENTREGADA
                        : EstadoReceta.MEDICAMENTO_AGOTADO);
        if (!aEntregar.isEmpty()) {
            receta.setFechaDespacho(ahora);
        }
        receta.setFarmaceutico(farmaceutico);
        return MapeoReceta.receta(recetaRepository.saveAndFlush(receta));
    }

    /**
     * Los medicamentos que se entregan ahora, en el orden en que llegaron. Un
     * error de fila sale en su campo ("entregados[1].id"), como en la receta.
     */
    private static List<DetalleReceta> validarEntregas(Receta receta, List<MedicamentoEntregadoRequest> entregas) {
        Map<Long, DetalleReceta> porId = receta.getMedicamentos().stream()
                .collect(Collectors.toMap(DetalleReceta::getId, Function.identity()));
        Set<Long> vistos = new HashSet<>();
        List<DetalleReceta> aEntregar = new ArrayList<>();
        for (int i = 0; i < entregas.size(); i++) {
            String campo = "entregados[" + i + "].id";
            DetalleReceta detalle = porId.get(entregas.get(i).id());
            if (detalle == null) {
                // Lo más probable: el especialista la corrigió después de que la pantalla la cargó.
                throw new CampoInvalidoException(campo, "Ese medicamento no está en la receta; vuelva a abrirla");
            }
            if (!vistos.add(detalle.getId())) {
                throw new CampoInvalidoException(campo, "«" + detalle.getMedicamento() + "» está repetido");
            }
            if (detalle.isEntregado()) {
                throw new ConflictoException("«" + detalle.getMedicamento() + "» ya se entregó");
            }
            aEntregar.add(detalle);
        }
        return aEntregar;
    }
}
