package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** YS-133. El historial de una cuenta, del pago más reciente al más antiguo. */
    @EntityGraph(attributePaths = { "registradoPor", "anuladoPor" })
    List<Pago> findByCuentaIdOrderByFechaDescIdDesc(Long cuentaId);

    /**
     * YS-157. Los pagos recibidos entre dos fechas, las dos incluidas, para el
     * reporte de cobros (HU-26): sin los anulados, que no se cobraron, y con
     * la cuenta, su interno y su familiar de una vez.
     */
    @EntityGraph(attributePaths = { "cuenta", "cuenta.interno", "cuenta.familiar" })
    @Query("""
            select p from Pago p
            where p.anulado = false and p.fecha between :desde and :hasta
            order by p.fecha, p.id
            """)
    List<Pago> cobradosEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
