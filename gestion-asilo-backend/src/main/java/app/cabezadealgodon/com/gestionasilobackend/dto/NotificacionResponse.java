package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoNotificacion;

/**
 * YS-95. Un aviso al familiar tal como lo ve quien da seguimiento a la
 * solicitud.
 *
 * <p>
 * No trae el correo del familiar ni el detalle técnico del error: la
 * Fundación también lo consulta y no necesita ninguno de los dos para decidir
 * si reintentar. El detalle queda en la base y en el registro del servidor.
 */
public record NotificacionResponse(
        Long id,
        TipoNotificacion tipo,
        EstadoNotificacion estado,
        int intentos,
        LocalDateTime fechaUltimoIntento,
        LocalDateTime fechaEnvio) {
}
