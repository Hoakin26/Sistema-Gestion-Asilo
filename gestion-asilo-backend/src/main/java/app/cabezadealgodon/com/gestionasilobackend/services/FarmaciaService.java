package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.RecetaResponse;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
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
}
