package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.time.LocalDateTime;
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
}
