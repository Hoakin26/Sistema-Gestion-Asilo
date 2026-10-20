package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.CargoServicio;

public interface CargoServicioRepository extends JpaRepository<CargoServicio, Long> {

    /** Los cargos que salieron de una visita. */
    List<CargoServicio> findByVisitaIdOrderByIdAsc(Long visitaId);

    /**
     * YS-132. Los cargos de todos los internos entre dos fechas, para
     * consolidar el mes (HU-18). Con el interno y su familiar, que es a quien
     * queda a cargo la cuenta.
     */
    @EntityGraph(attributePaths = { "interno", "interno.familiarResponsable" })
    @Query("select c from CargoServicio c where c.fecha between :desde and :hasta order by c.fecha, c.id")
    List<CargoServicio> entre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /**
     * YS-132. Los de un interno entre dos fechas, para su estado de cuenta.
     * Con la visita, la cita y la solicitud: el número de solicitud es lo único
     * de la consulta que muestra el desglose.
     */
    @EntityGraph(attributePaths = { "cuenta", "visita", "visita.cita", "visita.cita.solicitud" })
    @Query("""
            select c from CargoServicio c
            where c.interno.id = :internoId and c.fecha between :desde and :hasta
            order by c.fecha, c.id
            """)
    List<CargoServicio> delInternoEntre(@Param("internoId") Long internoId, @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
