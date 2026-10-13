package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import jakarta.persistence.LockModeType;

/**
 * YS-98. Las consultas de detalle traen de una vez todo lo que muestra la
 * pantalla de la visita: la cita, la solicitud con sus personas y el
 * especialista. Sin el grafo, cada visita de la lista dispararía media docena
 * de consultas más.
 */
public interface VisitaMedicaRepository extends JpaRepository<VisitaMedica, Long> {

    @EntityGraph(attributePaths = {
            "cita", "cita.solicitud", "cita.solicitud.especialidad", "cita.solicitud.medicoGeneral",
            "cita.solicitud.enfermeroAcompanante", "interno", "especialista", "especialista.usuario" })
    @Query("select v from VisitaMedica v where v.id = :id")
    Optional<VisitaMedica> conDetalle(@Param("id") Long id);

    boolean existsByCitaId(Long citaId);

    /**
     * YS-105. Lee la visita bloqueándola hasta el final de la transacción: dos
     * "Finalizar" a la vez se hacen uno tras otro, y el segundo ya la ve
     * finalizada. Sin esto, cada uno generaría su propio cargo de consulta.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VisitaMedica v where v.id = :id")
    Optional<VisitaMedica> bloquear(@Param("id") Long id);

    /** Solo los dos ids: es todo lo que la agenda necesita saber de la visita. */
    interface VisitaDeCita {
        Long getCitaId();

        Long getVisitaId();
    }

    /** Las visitas de unas citas: la agenda las usa para ofrecer "Continuar visita". */
    @Query("select v.cita.id as citaId, v.id as visitaId from VisitaMedica v where v.cita.id in :citaIds")
    List<VisitaDeCita> deCitas(@Param("citaIds") Collection<Long> citaIds);

    /** HU-07. Las visitas de un especialista, la más reciente primero. */
    @EntityGraph(attributePaths = {
            "cita", "cita.solicitud", "cita.solicitud.especialidad", "cita.solicitud.medicoGeneral",
            "cita.solicitud.enfermeroAcompanante", "interno", "especialista", "especialista.usuario" })
    @Query("""
            select v from VisitaMedica v
            where v.especialista.usuario.id = :usuarioId
            order by v.fechaInicio desc
            """)
    List<VisitaMedica> delEspecialista(@Param("usuarioId") Long usuarioId);

    /** YS-102. Las visitas de un interno, con cualquier especialista, la más reciente primero. */
    @EntityGraph(attributePaths = {
            "cita", "cita.solicitud", "cita.solicitud.especialidad", "especialista", "especialista.usuario" })
    @Query("""
            select v from VisitaMedica v
            where v.interno.id = :internoId
            order by v.fechaInicio desc
            """)
    List<VisitaMedica> delInterno(@Param("internoId") Long internoId);

    /** Lo mismo, solo las que están en un estado. */
    @EntityGraph(attributePaths = {
            "cita", "cita.solicitud", "cita.solicitud.especialidad", "cita.solicitud.medicoGeneral",
            "cita.solicitud.enfermeroAcompanante", "interno", "especialista", "especialista.usuario" })
    @Query("""
            select v from VisitaMedica v
            where v.especialista.usuario.id = :usuarioId and v.estado = :estado
            order by v.fechaInicio desc
            """)
    List<VisitaMedica> delEspecialistaEnEstado(@Param("usuarioId") Long usuarioId,
            @Param("estado") EstadoVisita estado);
}
