package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.CostosRecetaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.DespachoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.MedicamentoEntregadoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.RecetaResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.ConflictoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.DetalleReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.Receta;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
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
    private final CargoService cargoService;

    public FarmaciaService(RecetaRepository recetaRepository, CargoService cargoService) {
        this.recetaRepository = recetaRepository;
        this.cargoService = cargoService;
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
     *
     * <p>
     * HU-17 (YS-129). Cada medicamento entregado llega con su costo, mayor que
     * cero, que se guarda en él y queda como un cargo de farmacia de la visita,
     * el que suma la cuenta del familiar (HU-18): uno por medicamento, como el
     * laboratorio tiene uno por examen. Lo que queda pendiente no se cobra
     * (CU-005, E.2), y como nada se entrega dos veces, nada se cobra dos veces.
     * Si algo falla, la transacción deshace los cargos junto con el despacho.
     */
    @Transactional
    public RecetaResponse despachar(Long recetaId, DespachoRequest request, Usuario farmaceutico) {
        Receta receta = recetaRepository.paraDespachar(recetaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una receta con id " + recetaId));
        if (receta.getEstado() == EstadoReceta.ENTREGADA) {
            throw new ConflictoException("Esta receta ya se entregó completa y no se vuelve a despachar");
        }

        List<Entrega> entregas = validarEntregas(receta, request.entregados());
        if (entregas.isEmpty() && receta.getEstado() != EstadoReceta.PENDIENTE_DESPACHO) {
            throw new CampoInvalidoException("entregados", "Marque al menos un medicamento entregado");
        }

        LocalDateTime ahora = LocalDateTime.now();
        entregas.forEach(entrega -> entrega.detalle().entregar(ahora, entrega.costo()));
        long entregados = receta.getMedicamentos().stream().filter(DetalleReceta::isEntregado).count();
        receta.cambiarEstado(entregados == receta.getMedicamentos().size() ? EstadoReceta.ENTREGADA
                : entregados > 0 ? EstadoReceta.PARCIALMENTE_ENTREGADA
                        : EstadoReceta.MEDICAMENTO_AGOTADO);
        if (!entregas.isEmpty()) {
            receta.setFechaDespacho(ahora);
        }
        receta.setFarmaceutico(farmaceutico);
        Receta guardada = recetaRepository.saveAndFlush(receta);
        entregas.forEach(entrega -> cargoService.registrar(guardada.getVisita(), TipoServicio.FARMACIA, entrega.costo()));
        return MapeoReceta.receta(guardada);
    }

    /** HU-17 (YS-129). Lo que se cobró de la receta: cada medicamento entregado y el total. */
    @Transactional(readOnly = true)
    public CostosRecetaResponse costos(Long recetaId) {
        Receta receta = recetaRepository.findById(recetaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una receta con id " + recetaId));
        List<CostosRecetaResponse.Costo> entregados = receta.getMedicamentos().stream()
                .filter(DetalleReceta::isEntregado)
                .map(detalle -> new CostosRecetaResponse.Costo(detalle.getId(), detalle.getCosto()))
                .toList();
        // Lo entregado antes de HU-17 no tiene costo: no suma.
        BigDecimal total = entregados.stream().map(CostosRecetaResponse.Costo::costo).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        return new CostosRecetaResponse(entregados, total);
    }

    /** Un medicamento que se entrega ahora, con su costo ya validado. */
    private record Entrega(DetalleReceta detalle, BigDecimal costo) {
    }

    /**
     * Los medicamentos que se entregan ahora, en el orden en que llegaron. Un
     * error de fila sale en su campo ("entregados[1].id", "entregados[1].costo"),
     * como en la receta.
     */
    private static List<Entrega> validarEntregas(Receta receta, List<MedicamentoEntregadoRequest> pedidas) {
        Map<Long, DetalleReceta> porId = receta.getMedicamentos().stream()
                .collect(Collectors.toMap(DetalleReceta::getId, Function.identity()));
        Set<Long> vistos = new HashSet<>();
        List<Entrega> entregas = new ArrayList<>();
        for (int i = 0; i < pedidas.size(); i++) {
            String fila = "entregados[" + i + "]";
            DetalleReceta detalle = porId.get(pedidas.get(i).id());
            if (detalle == null) {
                // Lo más probable: el especialista la corrigió después de que la pantalla la cargó.
                throw new CampoInvalidoException(fila + ".id", "Ese medicamento no está en la receta; vuelva a abrirla");
            }
            if (!vistos.add(detalle.getId())) {
                throw new CampoInvalidoException(fila + ".id", "«" + detalle.getMedicamento() + "» está repetido");
            }
            if (detalle.isEntregado()) {
                throw new ConflictoException("«" + detalle.getMedicamento() + "» ya se entregó");
            }
            BigDecimal costo = Montos.costo(pedidas.get(i).costo(), fila + ".costo", "Indique el costo del medicamento");
            entregas.add(new Entrega(detalle, costo));
        }
        return entregas;
    }
}
