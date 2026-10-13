package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.Receta;

public interface RecetaRepository extends JpaRepository<Receta, Long> {

    /** La receta de una visita, con sus medicamentos. */
    @EntityGraph(attributePaths = { "medicamentos" })
    Optional<Receta> findByVisitaId(Long visitaId);

    boolean existsByVisitaId(Long visitaId);

    /**
     * YS-108. Las recetas de un interno, la más reciente primero, con lo que
     * muestran los antecedentes (HU-08): de qué visita y especialista salieron.
     */
    @EntityGraph(attributePaths = { "medicamentos", "visita", "visita.cita", "visita.cita.solicitud",
            "visita.cita.solicitud.especialidad", "visita.especialista", "visita.especialista.usuario" })
    @Query("""
            select distinct r from Receta r
            where r.visita.interno.id = :internoId
            order by r.fechaEmision desc
            """)
    List<Receta> delInterno(@Param("internoId") Long internoId);
}
