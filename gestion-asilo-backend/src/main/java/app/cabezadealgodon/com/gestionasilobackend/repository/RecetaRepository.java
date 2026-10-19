package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
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

    /**
     * YS-123. La bandeja de la farmacia (HU-15): las recetas en alguno de
     * {@code estados}, cuyo paciente coincide por nombre o DPI. La misma
     * búsqueda que la bandeja del laboratorio: el nombre sin mayúsculas ni
     * tildes, el DPI tal cual, y con la búsqueda vacía el patrón "%%" trae
     * todas.
     *
     * <p>
     * El {@code @EntityGraph} trae de una vez el paciente y el médico de cada
     * fila. Los medicamentos no van en él: con una colección, Hibernate no
     * puede paginar en la base y traería todas las recetas para cortar la
     * página en memoria. Llegan después, por lotes ({@code @BatchSize} en
     * {@link Receta}).
     */
    @EntityGraph(attributePaths = { "visita", "visita.interno", "visita.especialista", "visita.especialista.usuario" })
    @Query("""
            select r from Receta r
            where r.estado in :estados
              and (lower(function('translate', r.visita.interno.nombreCompleto,
                        'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) like :patron escape '\\'
                   or r.visita.interno.dpi like :patron escape '\\')
            """)
    Page<Receta> bandeja(@Param("estados") Collection<EstadoReceta> estados, @Param("patron") String patron,
            Pageable paginado);
}
