package app.cabezadealgodon.com.gestionasilobackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
