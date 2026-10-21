package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.PagoFundacion;

public interface PagoFundacionRepository extends JpaRepository<PagoFundacion, Long> {

    /** YS-139. Los pagos de una deuda con la fundación, del más reciente al más antiguo. */
    @EntityGraph(attributePaths = { "registradoPor", "anuladoPor" })
    List<PagoFundacion> findByCuentaIdOrderByFechaDescIdDesc(Long cuentaId);
}
