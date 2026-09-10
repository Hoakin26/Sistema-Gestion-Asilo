package app.cabezadealgodon.com.gestionasilobackend.dto;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede exceder 150 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String correo,

        @NotNull(message = "El rol es obligatorio")
        RolNombre rol) {
}
