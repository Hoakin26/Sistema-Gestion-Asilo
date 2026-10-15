package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** YS-58. El familiar responsable, que viaja dentro de InternoRequest. */
public record FamiliarRequest(
        @NotBlank(message = "El nombre del familiar es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El parentesco es obligatorio")
        @Size(max = 50, message = "El parentesco no puede exceder 50 caracteres")
        String parentesco,

        @NotBlank(message = "El teléfono del familiar es obligatorio")
        @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
        String telefono,

        // Opcional: no todo familiar responsable tiene correo electrónico.
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String correo,

        @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
        String direccion) {
}
