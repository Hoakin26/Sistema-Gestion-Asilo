package app.cabezadealgodon.com.gestionasilobackend.dto;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;

public record UsuarioRequest(
        String nombreCompleto,
        String correo,
        String password,
        RolNombre rol) {
}
