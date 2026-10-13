package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.CargoServicio;

public interface CargoServicioRepository extends JpaRepository<CargoServicio, Long> {

    /** Los cargos que salieron de una visita. */
    List<CargoServicio> findByVisitaIdOrderByIdAsc(Long visitaId);
}
