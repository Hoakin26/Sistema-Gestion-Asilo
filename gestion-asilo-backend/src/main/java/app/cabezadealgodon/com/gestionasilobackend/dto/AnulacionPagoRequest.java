package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** YS-176. Por qué se anula un pago: obligatorio, para que quede escrito en el historial. */
public record AnulacionPagoRequest(
        @NotBlank(message = "Escriba el motivo de la anulación")
        @Size(max = 300, message = "El motivo no puede exceder 300 caracteres")
        String motivo) {
}
