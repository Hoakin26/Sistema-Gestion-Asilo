package app.cabezadealgodon.com.gestionasilobackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.Familiar;

public interface FamiliarRepository extends JpaRepository<Familiar, Long> {
}
