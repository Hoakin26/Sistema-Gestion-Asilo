package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.PagoFundacion;

public interface PagoFundacionRepository extends JpaRepository<PagoFundacion, Long> {

    /** YS-139. Los pagos de una deuda con la fundación, del más reciente al más antiguo. */
    @EntityGraph(attributePaths = { "registradoPor", "anuladoPor" })
    List<PagoFundacion> findByCuentaIdOrderByFechaDescIdDesc(Long cuentaId);

    /**
     * YS-160. Los pagos a la fundación entre dos fechas, las dos incluidas,
     * para su reporte (HU-27, RF-20a): sin los anulados, que no se pagaron, y
     * con la deuda de una vez, que trae el concepto y el mes.
     */
    @EntityGraph(attributePaths = { "cuenta" })
    @Query("""
            select p from PagoFundacion p
            where p.anulado = false and p.fecha between :desde and :hasta
            order by p.fecha, p.id
            """)
    List<PagoFundacion> realizadosEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
