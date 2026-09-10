package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;

/**
 * Nunca incluye passwordHash: el hash de la contraseña no sale de la API
 * bajo ninguna circunstancia (ver docs/seguridad.md).
 */
public record UsuarioResponse(
        Long id,
        String nombreCompleto,
        String correo,
        RolNombre rol,
        String rolDescripcion,
        boolean activo,
        LocalDateTime fechaCreacion) {
}
