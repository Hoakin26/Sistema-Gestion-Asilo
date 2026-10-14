package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.List;

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
}
