package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.Cita;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    @EntityGraph(attributePaths = { "especialista", "especialista.usuario", "especialista.especialidad" })
    Optional<Cita> findBySolicitudId(Long solicitudId);

    boolean existsBySolicitudId(Long solicitudId);

    /**
     * Las citas de un especialista que empiezan en un rango, para saber qué
     * horas tiene ocupadas un día. Cuando exista la cancelación de citas, las
     * canceladas tendrán que quedar fuera de esta consulta para liberar su hora.
     */
    @Query("""
            select c from Cita c
            where c.especialista.id = :especialistaId
              and c.fechaHora >= :desde and c.fechaHora < :hasta
            """)
    List<Cita> delEspecialistaEntre(@Param("especialistaId") Long especialistaId,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /**
     * YS-99. La agenda de un especialista, buscado por su cuenta: las citas
     * que empiezan en el rango, en orden, con lo que muestra cada fila.
     */
    @EntityGraph(attributePaths = { "solicitud", "solicitud.interno", "solicitud.especialidad",
            "solicitud.enfermeroAcompanante" })
    @Query("""
            select c from Cita c
            where c.especialista.usuario.id = :usuarioId
              and c.fechaHora >= :desde and c.fechaHora < :hasta
            order by c.fechaHora
            """)
    List<Cita> agendaDe(@Param("usuarioId") Long usuarioId, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** YS-99. Una cita con su solicitud y su especialista, para iniciar la visita. */
    @EntityGraph(attributePaths = { "solicitud", "solicitud.interno", "solicitud.especialidad",
            "especialista", "especialista.usuario" })
    @Query("select c from Cita c where c.id = :id")
    Optional<Cita> conSolicitud(@Param("id") Long id);

    /** YS-188. Las citas de unas solicitudes, con su especialista: las de los acompañamientos de Enfermería. */
    @EntityGraph(attributePaths = { "solicitud", "especialista", "especialista.usuario" })
    List<Cita> findBySolicitudIdIn(Collection<Long> solicitudIds);
}
