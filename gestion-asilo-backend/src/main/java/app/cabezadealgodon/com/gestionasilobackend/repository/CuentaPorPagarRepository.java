package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.CuentaPorPagar;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;

public interface CuentaPorPagarRepository extends JpaRepository<CuentaPorPagar, Long> {

    /** YS-139. La deuda de un servicio en un mes ("2026-10"), si ya tiene pagos. */
    Optional<CuentaPorPagar> findByPeriodoAndTipoServicio(String periodo, TipoServicio tipoServicio);
}
