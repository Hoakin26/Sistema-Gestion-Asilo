package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.OrdenLaboratorioResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.models.OrdenLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.OrdenLaboratorioRepository;

/**
 * YS-114. El trabajo del laboratorio sobre las órdenes que dan los
 * especialistas (RF-09). HU-12 es la bandeja y la toma de cada orden; HU-13 y
 * HU-14 le agregan el registro del resultado y su costo.
 */
@Service
public class LaboratorioService {

    private final OrdenLaboratorioRepository ordenRepository;

    public LaboratorioService(OrdenLaboratorioRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    /**
     * HU-12. Las órdenes en alguno de {@code estados} (sin ninguno, todas),
     * filtradas por el nombre o el DPI del paciente.
     *
     * <p>
     * Las órdenes de una misma visita comparten la fecha, así que se desempata
     * por id. Sin eso, el orden entre ellas lo elige la base en cada consulta,
     * y al pasar de página una orden podía repetirse y otra no aparecer nunca.
     */
    @Transactional(readOnly = true)
    public Page<OrdenLaboratorioResponse> ordenes(Set<EstadoOrden> estados, String paciente, Pageable paginado) {
        Set<EstadoOrden> filtro = estados == null || estados.isEmpty() ? EnumSet.allOf(EstadoOrden.class) : estados;
        String patron = PatronBusqueda.contiene(PatronBusqueda.normalizado(paciente));
        Pageable estable = PageRequest.of(paginado.getPageNumber(), paginado.getPageSize(),
                paginado.getSort().and(Sort.by("id")));
        return ordenRepository.bandeja(filtro, patron, estable).map(VisitaMedicaService::orden);
    }

    /**
     * DE-02. El laboratorio toma la orden: pasa de Pendiente a En Proceso y
     * queda anotado quién la tomó. Una orden que ya no está pendiente es un
     * conflicto con su estado (409), no un error del cliente.
     *
     * <p>
     * Si dos personas la toman a la vez, la columna {@code version} hace que
     * la segunda falle al guardar en vez de pisar a la primera.
     */
    @Transactional
    public OrdenLaboratorioResponse iniciar(Long ordenId, Usuario laboratorista) {
        OrdenLaboratorio orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una orden de laboratorio con id " + ordenId));
        orden.cambiarEstado(EstadoOrden.EN_PROCESO);
        orden.setLaboratorista(laboratorista);
        return VisitaMedicaService.orden(ordenRepository.saveAndFlush(orden));
    }
}
