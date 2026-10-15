package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.Especialidad;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    boolean existsByNombre(String nombre);

    /** Las que se ofrecen al referir, en orden alfabético para encontrarlas rápido. */
    List<Especialidad> findByActivaTrueOrderByNombreAsc();
}
