package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;

/**
 * YS-87. Los listados traen de una vez todo lo que la fila muestra. Sin el
 * {@code @EntityGraph}, cada solicitud de la página dispararía sus propias
 * consultas para el interno, la especialidad y las dos personas: veinte
 * filas serían más de ochenta consultas.
 */
public interface SolicitudReferenciaRepository extends JpaRepository<SolicitudReferencia, Long> {

    /*
     * YS-166. La búsqueda: por número, nombre del interno o especialidad, sin
     * mayúsculas ni tildes (el patrón llega normalizado de PatronBusqueda, y
     * la columna pasa por translate antes que por lower, como en internos).
     * Con la búsqueda vacía el patrón es "%%" y trae todas.
     */
    String COINCIDE = """
            (lower(s.numero) like :patron escape '\\'
             or lower(function('translate', s.interno.nombreCompleto,
                    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) like :patron escape '\\'
             or lower(function('translate', s.especialidad.nombre,
                    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) like :patron escape '\\')
            """;

    @EntityGraph(attributePaths = { "interno", "interno.familiarResponsable", "especialidad", "medicoGeneral",
            "enfermeroAcompanante" })
    @Query("select s from SolicitudReferencia s where " + COINCIDE)
    Page<SolicitudReferencia> buscar(@Param("patron") String patron, Pageable paginado);

    @EntityGraph(attributePaths = { "interno", "interno.familiarResponsable", "especialidad", "medicoGeneral",
            "enfermeroAcompanante" })
    @Query("select s from SolicitudReferencia s where s.estado = :estado and " + COINCIDE)
    Page<SolicitudReferencia> buscarPorEstado(@Param("estado") EstadoSolicitud estado,
            @Param("patron") String patron, Pageable paginado);

    /**
     * YS-188. Las solicitudes en que un enfermero(a) acompaña al interno, en
     * los estados pedidos, la más reciente primero, con lo que muestra cada una.
     * OBS-01: sin paginar, para los próximos, que nunca son muchos.
     */
    @EntityGraph(attributePaths = { "interno", "especialidad", "medicoGeneral" })
    @Query("""
            select s from SolicitudReferencia s
            where s.enfermeroAcompanante.id = :enfermeroId and s.estado in :estados
            order by s.fechaCreacion desc, s.id desc
            """)
    List<SolicitudReferencia> acompanadasPor(@Param("enfermeroId") Long enfermeroId,
            @Param("estados") Collection<EstadoSolicitud> estados);

    /**
     * OBS-01. Lo mismo, por páginas, para los anteriores, que crecen con cada
     * mes. La más reciente primero por la fecha de la cita o, sin ella, por la
     * de la solicitud: el orden que antes hacía la pantalla con la lista entera.
     */
    @EntityGraph(attributePaths = { "interno", "especialidad", "medicoGeneral" })
    @Query(value = """
            select s from SolicitudReferencia s left join Cita c on c.solicitud = s
            where s.enfermeroAcompanante.id = :enfermeroId and s.estado in :estados
            order by coalesce(c.fechaHora, s.fechaCreacion) desc, s.id desc
            """, countQuery = """
            select count(s) from SolicitudReferencia s
            where s.enfermeroAcompanante.id = :enfermeroId and s.estado in :estados
            """)
    Page<SolicitudReferencia> acompanadasPor(@Param("enfermeroId") Long enfermeroId,
            @Param("estados") Collection<EstadoSolicitud> estados, Pageable paginado);
}
