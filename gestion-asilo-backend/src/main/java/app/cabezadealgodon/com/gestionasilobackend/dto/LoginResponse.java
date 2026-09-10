package app.cabezadealgodon.com.gestionasilobackend.dto;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;

/** El rol viaja aquí para que el frontend arme su menú (YS-55). */
public record LoginResponse(
        String token,
        String tipo,
        String correo,
        String nombreCompleto,
        RolNombre rol) {
}
