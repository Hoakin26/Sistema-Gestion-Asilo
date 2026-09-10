package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.Rol;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(RolNombre nombre);
}
