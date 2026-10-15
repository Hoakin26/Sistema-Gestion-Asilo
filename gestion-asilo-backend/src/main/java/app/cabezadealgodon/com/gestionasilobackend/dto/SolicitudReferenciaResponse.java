package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;

/**
 * YS-84. Una solicitud de referencia tal como la devuelve la API.
 *
 * <p>
 * YS-94. {@code familiarConCorreo} le dice a la pantalla si el familiar
 * recibirá aviso: sin correo registrado no hay a dónde mandarlo, y el médico
 * debe saberlo en el momento para avisar por otro medio o agregar el correo.
 */
public record SolicitudReferenciaResponse(
        Long id,
        String numero,
        EstadoSolicitud estado,
        String motivo,
        ResumenPersona interno,
        EspecialidadResponse especialidad,
        ResumenPersona medicoGeneral,
        ResumenPersona enfermeroAcompanante,
        LocalDateTime fechaCreacion,
        boolean familiarConCorreo) {
}
