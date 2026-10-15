package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    /** YS-84. Los usuarios activos de un rol, por nombre (rol.nombre). */
    List<Usuario> findByRolNombreAndActivoTrueOrderByNombreCompletoAsc(RolNombre rol);
}
