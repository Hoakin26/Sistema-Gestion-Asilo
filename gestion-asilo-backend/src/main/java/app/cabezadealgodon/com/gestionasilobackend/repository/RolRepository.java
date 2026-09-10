package app.cabezadealgodon.com.gestionasilobackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {
}
