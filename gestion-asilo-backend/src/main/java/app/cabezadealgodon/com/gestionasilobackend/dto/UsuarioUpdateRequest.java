package app.cabezadealgodon.com.gestionasilobackend.dto;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;

public record UsuarioUpdateRequest(
        String nombreCompleto,
        String correo,
        RolNombre rol) {
}
