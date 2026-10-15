package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * YS-84. Lo que el Médico General elige al referir a un interno (RF-03).
 *
 * <p>
 * No trae al médico: el servicio lo toma del token. Si viniera en el cuerpo,
 * cualquiera podría crear una solicitud a nombre de otro.
 */
public record SolicitudReferenciaRequest(
        @NotNull(message = "Seleccione al interno que va a referir")
        Long internoId,

        @NotNull(message = "Seleccione la especialidad")
        Long especialidadId,

        @NotNull(message = "Asigne el enfermero(a) acompañante")
        Long enfermeroAcompananteId,

        @NotBlank(message = "El motivo de referencia es obligatorio")
        @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
        String motivo) {
}
