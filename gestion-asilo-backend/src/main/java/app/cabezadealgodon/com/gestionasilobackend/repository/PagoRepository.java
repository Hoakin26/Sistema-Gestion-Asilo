package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** YS-133. El historial de una cuenta, del pago más reciente al más antiguo. */
    @EntityGraph(attributePaths = { "registradoPor", "anuladoPor" })
    List<Pago> findByCuentaIdOrderByFechaDescIdDesc(Long cuentaId);
}
