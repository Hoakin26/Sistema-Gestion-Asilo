package app.cabezadealgodon.com.gestionasilobackend.eventos;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.services.NotificacionService;

/**
 * HU-04 (YS-94). Escucha la creación de solicitudes y le avisa al familiar.
 *
 * <p>
 * Es la otra mitad de {@link SolicitudCreadaEvent}: el correo se agregó aquí
 * sin tocar {@code SolicitudReferenciaService}, que sigue sin saber que los
 * correos existen (principio abierto/cerrado, sección 4.7 del diseño).
 *
 * <p>
 * Reacciona en dos momentos de la misma transacción, y el orden importa:
 * <ol>
 * <li><b>Antes de confirmarla</b>, registra el aviso como pendiente. Queda
 * guardado junto con la solicitud: si algo falla después, el reintento
 * automático lo encuentra.</li>
 * <li><b>Después de confirmarla</b>, y en otro hilo, lo envía. Después, porque
 * mandar el correo de una solicitud que al final no se guardó sería avisar de
 * algo que no existe. En otro hilo, porque el médico no tiene por qué esperar
 * al servidor de correo, y si ese servidor está caído la solicitud igual
 * queda creada (criterio de HU-04).</li>
 * </ol>
 */
@Component
public class AvisoFamiliarListener {

    private final NotificacionService notificacionService;

    public AvisoFamiliarListener(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void registrar(SolicitudCreadaEvent evento) {
        notificacionService.registrar(evento.solicitudId(), TipoNotificacion.SOLICITUD_CREADA);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(SolicitudCreadaEvent evento) {
        notificacionService.enviarPendientes(evento.solicitudId());
    }

    /*
     * HU-06 (YS-91). El segundo aviso, al asignar la cita, con el mismo
     * esquema de dos momentos: registrado junto con la cita, enviado después.
     */

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void registrar(CitaAsignadaEvent evento) {
        notificacionService.registrar(evento.solicitudId(), TipoNotificacion.CITA_ASIGNADA);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(CitaAsignadaEvent evento) {
        notificacionService.enviarPendientes(evento.solicitudId());
    }
}
