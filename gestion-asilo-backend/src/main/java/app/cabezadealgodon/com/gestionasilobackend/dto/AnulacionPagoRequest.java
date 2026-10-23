package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * YS-176. Por qué se anula un pago: obligatorio, para que quede escrito en el
 * historial. YS-145 y YS-148: también el de una donación (HU-22) y el de un
 * gasto operativo (HU-23), con la misma regla.
 */
public record AnulacionPagoRequest(
        @NotBlank(message = "Escriba el motivo de la anulación")
        @Size(max = 300, message = "El motivo no puede exceder 300 caracteres")
        String motivo) {
}
