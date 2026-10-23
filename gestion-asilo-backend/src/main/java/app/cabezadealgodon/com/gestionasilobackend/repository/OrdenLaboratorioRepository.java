package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.models.OrdenLaboratorio;

public interface OrdenLaboratorioRepository extends JpaRepository<OrdenLaboratorio, Long> {

    /** Las órdenes de una visita, en el orden en que se dieron. */
    @EntityGraph(attributePaths = { "examen" })
    List<OrdenLaboratorio> findByVisitaIdOrderByIdAsc(Long visitaId);

    /** Si ese examen ya está ordenado en la visita, sin contar las órdenes anuladas. */
    boolean existsByVisitaIdAndExamenIdAndEstadoNot(Long visitaId, Long examenId, EstadoOrden estado);

    /**
     * YS-111. Las órdenes de un interno, la más reciente primero, con lo que
     * muestran los antecedentes (HU-08): de qué visita y especialista salieron.
     */
    @EntityGraph(attributePaths = { "examen", "visita", "visita.cita", "visita.cita.solicitud",
            "visita.cita.solicitud.especialidad", "visita.especialista", "visita.especialista.usuario" })
    @Query("""
            select o from OrdenLaboratorio o
            where o.visita.interno.id = :internoId
            order by o.fechaOrden desc, o.id desc
            """)
    List<OrdenLaboratorio> delInterno(@Param("internoId") Long internoId);

    /**
     * YS-114. La bandeja del laboratorio (HU-12): las órdenes en alguno de
     * {@code estados}, cuyo paciente coincide por nombre o DPI. El nombre se
     * compara sin mayúsculas ni tildes, como en la bandeja de solicitudes; el
     * DPI, tal cual. Con la búsqueda vacía el patrón es "%%" y trae todas.
     *
     * <p>
     * Trae de una vez lo que muestra cada fila; sin el {@code @EntityGraph},
     * veinte órdenes serían más de ochenta consultas.
     */
    @EntityGraph(attributePaths = { "examen", "visita", "visita.interno", "visita.especialista",
            "visita.especialista.usuario" })
    @Query("""
            select o from OrdenLaboratorio o
            where o.estado in :estados
              and (lower(function('translate', o.visita.interno.nombreCompleto,
                        'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) like :patron escape '\\'
                   or o.visita.interno.dpi like :patron escape '\\')
            """)
    Page<OrdenLaboratorio> bandeja(@Param("estados") Collection<EstadoOrden> estados,
            @Param("patron") String patron, Pageable paginado);

    /**
     * YS-163. Los exámenes realizados a un interno entre dos momentos, por la
     * fecha del resultado, para su reporte (HU-28, RF-21a): solo las órdenes
     * en {@code estado}, que es el de resultados cargados. Con quién lo pidió
     * de una vez.
     */
    @EntityGraph(attributePaths = { "examen", "visita", "visita.cita", "visita.cita.solicitud",
            "visita.cita.solicitud.especialidad", "visita.especialista", "visita.especialista.usuario" })
    @Query("""
            select o from OrdenLaboratorio o
            where o.visita.interno.id = :internoId and o.estado = :estado
              and o.fechaResultado >= :desde and o.fechaResultado < :hasta
            order by o.fechaResultado, o.id
            """)
    List<OrdenLaboratorio> delInternoConResultadoEntre(@Param("internoId") Long internoId,
            @Param("estado") EstadoOrden estado, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
