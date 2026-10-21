package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.CargoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;

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

    /**
     * YS-142. Los cargos de las citas que caen entre dos momentos, de los
     * pacientes que coinciden por nombre o DPI: la base del listado de citas
     * pendientes de pago (HU-21). La misma búsqueda que las bandejas del
     * laboratorio y la farmacia: el nombre sin mayúsculas ni tildes, el DPI
     * tal cual, y con la búsqueda vacía el patrón "%%" trae todos. Con la
     * cuenta en que se consolidó cada uno, para repartir lo pagado, y la
     * solicitud, por su número.
     */
    @EntityGraph(attributePaths = { "cuenta", "interno", "visita", "visita.cita", "visita.cita.solicitud" })
    @Query("""
            select c from CargoServicio c
            where c.visita.cita.fechaHora >= :desde and c.visita.cita.fechaHora < :hasta
              and (lower(function('translate', c.interno.nombreCompleto,
                        'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) like :patron escape '\\'
                   or c.interno.dpi like :patron escape '\\')
            order by c.visita.cita.fechaHora, c.visita.id, c.id
            """)
    List<CargoServicio> deCitasEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
            @Param("patron") String patron);

    /**
     * YS-139. Lo que suman los cargos de cada mes y tipo de servicio: lo que el
     * asilo le debe a la fundación (HU-20). El neto, con el descuento que da la
     * fundación (decidido el 21/10). Sin nada del interno ni del servicio en
     * sí: solo el tipo, cuántos y cuánto.
     */
    @Query("""
            select year(c.fecha) as anio, month(c.fecha) as mes, c.tipo as tipo,
                   count(c) as cantidad, sum(c.montoNeto) as total
            from CargoServicio c
            group by year(c.fecha), month(c.fecha), c.tipo
            """)
    List<TotalDelMes> totalesPorMes();

    /** YS-139. Lo mismo para un solo tipo de servicio entre dos fechas: la deuda de un mes. */
    @Query("""
            select year(c.fecha) as anio, month(c.fecha) as mes, c.tipo as tipo,
                   count(c) as cantidad, sum(c.montoNeto) as total
            from CargoServicio c
            where c.tipo = :tipo and c.fecha between :desde and :hasta
            group by year(c.fecha), month(c.fecha), c.tipo
            """)
    Optional<TotalDelMes> totalDelMes(@Param("tipo") TipoServicio tipo, @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /** YS-139. Una fila de {@link #totalesPorMes}: cuántos cargos de un tipo hubo en un mes y cuánto suman. */
    interface TotalDelMes {
        Integer getAnio();

        Integer getMes();

        TipoServicio getTipo();

        Long getCantidad();

        BigDecimal getTotal();
    }
}
