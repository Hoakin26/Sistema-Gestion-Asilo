package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.ExamenLaboratorio;

public interface ExamenLaboratorioRepository extends JpaRepository<ExamenLaboratorio, Long> {

    boolean existsByNombre(String nombre);

    /** Los que se pueden ordenar. El servicio los ordena por nombre. */
    List<ExamenLaboratorio> findByActivoTrue();
}
