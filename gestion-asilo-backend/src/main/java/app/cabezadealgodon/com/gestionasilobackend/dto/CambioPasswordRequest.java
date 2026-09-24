package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * YS-69. Se pide la contraseña actual aunque la petición ya traiga un token
 * válido: sin eso, quien encuentre una sesión abierta podría quedarse con la
 * cuenta.
 */
public record CambioPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        // Mismo rango que al crear el usuario (YS-42): el máximo evita el límite de 72 bytes de BCrypt.
        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
        String passwordNueva) {
}
