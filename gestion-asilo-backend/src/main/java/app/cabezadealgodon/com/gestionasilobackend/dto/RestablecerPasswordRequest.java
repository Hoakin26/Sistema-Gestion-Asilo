package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * YS-71. Contraseña temporal que un administrador asigna a otro usuario que
 * olvidó la suya. No pide la anterior: justamente se usa cuando nadie la sabe.
 */
public record RestablecerPasswordRequest(
        // Mismo rango que al crear el usuario (YS-42): el máximo evita el límite de 72 bytes de BCrypt.
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
        String password) {
}
