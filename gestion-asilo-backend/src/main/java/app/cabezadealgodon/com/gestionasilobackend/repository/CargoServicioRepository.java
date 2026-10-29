package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
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
     * DEF-02. Los gastos médicos de unas cuentas mensuales, ya sumados por cuenta
     * y tipo de servicio: lo único que usa el reporte de cobros (HU-26). Una sola
     * consulta y sin cargar entidades: cada cargo arrastraba su visita, su cita y
     * su solicitud, y con miles de cargos eran miles de consultas.
     */
    @Query("""
            select c.cuenta.id as cuentaId, c.tipo as tipo, count(c) as cantidad,
                   sum(c.montoOriginal) as original, sum(c.descuento) as descuento,
                   sum(c.montoNeto) as neto
            from CargoServicio c
            where c.cuenta.id in :cuentaIds
            group by c.cuenta.id, c.tipo
            """)
    List<TotalDeCuenta> totalesPorCuenta(@Param("cuentaIds") Collection<Long> cuentaIds);

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
     * OBS-01. Lo mismo que {@link #deCitasEntre}, ya sumado por cita y por
     * cuenta, con los montos de la cuenta: lo justo para saber qué citas siguen
     * pendientes y cuánto suman todas, sin cargar entidades. Una fila por cita
     * y cuenta, y una con la cuenta vacía para lo que no se ha consolidado.
     */
    @Query("""
            select v.id as visitaId, cu.id as cuentaId, cu.saldo as saldoCuenta,
                   cu.totalPagado as pagadoCuenta, cu.totalAPagar as totalCuenta, sum(c.montoNeto) as neto
            from CargoServicio c join c.visita v join v.cita ci join c.interno i left join c.cuenta cu
            where ci.fechaHora >= :desde and ci.fechaHora < :hasta
              and (lower(function('translate', i.nombreCompleto,
                        'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) like :patron escape '\\'
                   or i.dpi like :patron escape '\\')
            group by ci.fechaHora, v.id, cu.id, cu.saldo, cu.totalPagado, cu.totalAPagar
            order by ci.fechaHora, v.id
            """)
    List<NetoDeCita> netosDeCitasEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
            @Param("patron") String patron);

    /** OBS-01. Los cargos de unas citas, con lo mismo que {@link #deCitasEntre}: los de una página del listado. */
    @EntityGraph(attributePaths = { "cuenta", "interno", "visita", "visita.cita", "visita.cita.solicitud" })
    @Query("""
            select c from CargoServicio c
            where c.visita.id in :visitaIds
            order by c.visita.cita.fechaHora, c.visita.id, c.id
            """)
    List<CargoServicio> deVisitas(@Param("visitaIds") Collection<Long> visitaIds);

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

    /** DEF-02. Una fila de {@link #totalesPorCuenta}: los cargos de un tipo en una cuenta. */
    interface TotalDeCuenta {
        Long getCuentaId();

        TipoServicio getTipo();

        Long getCantidad();

        BigDecimal getOriginal();

        BigDecimal getDescuento();

        BigDecimal getNeto();
    }

    /**
     * OBS-01. Una fila de {@link #netosDeCitasEntre}: lo que suman los cargos
     * de una cita en una cuenta, y lo que esa cuenta lleva pagado. Los de la
     * cuenta van nulos para lo que no se ha consolidado.
     */
    interface NetoDeCita {
        Long getVisitaId();

        Long getCuentaId();

        BigDecimal getSaldoCuenta();

        BigDecimal getPagadoCuenta();

        BigDecimal getTotalCuenta();

        BigDecimal getNeto();
    }
}
