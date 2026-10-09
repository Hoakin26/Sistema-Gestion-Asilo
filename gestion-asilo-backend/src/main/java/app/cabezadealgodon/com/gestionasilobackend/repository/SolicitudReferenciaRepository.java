package app.cabezadealgodon.com.gestionasilobackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;

/**
 * YS-87. Los listados traen de una vez todo lo que la fila muestra. Sin el
 * {@code @EntityGraph}, cada solicitud de la página dispararía sus propias
 * consultas para el interno, la especialidad y las dos personas: veinte
 * filas serían más de ochenta consultas.
 */
public interface SolicitudReferenciaRepository extends JpaRepository<SolicitudReferencia, Long> {

    @Override
    @EntityGraph(attributePaths = { "interno", "interno.familiarResponsable", "especialidad", "medicoGeneral",
            "enfermeroAcompanante" })
    Page<SolicitudReferencia> findAll(Pageable paginado);

    @EntityGraph(attributePaths = { "interno", "interno.familiarResponsable", "especialidad", "medicoGeneral",
            "enfermeroAcompanante" })
    Page<SolicitudReferencia> findByEstado(EstadoSolicitud estado, Pageable paginado);
}
