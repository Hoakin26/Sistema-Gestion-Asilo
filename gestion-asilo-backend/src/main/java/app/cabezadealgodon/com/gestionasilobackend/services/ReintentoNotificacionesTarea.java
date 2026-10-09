package app.cabezadealgodon.com.gestionasilobackend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * YS-95. Reintenta solos los avisos al familiar que no se pudieron enviar.
 *
 * <p>
 * Cada diez minutos por defecto, hasta cinco intentos por aviso
 * ({@code app.notificaciones.*}). Así un servidor de correo caído un rato no
 * deja a ninguna familia sin aviso, y nadie tiene que acordarse de reintentar.
 *
 * <p>
 * Vive aparte de {@link NotificacionService} para que cada solicitud se
 * reintente en su propia transacción: si el bucle llamara a métodos del mismo
 * servicio, Spring no aplicaría {@code @Transactional} y un fallo a mitad
 * arrastraría a todos.
 *
 * <p>
 * Las pruebas la apagan ({@code app.notificaciones.reintento-automatico=false})
 * para que no corra en segundo plano mientras otra prueba mira el estado de un
 * aviso.
 */
@Component
@ConditionalOnProperty(name = "app.notificaciones.reintento-automatico", havingValue = "true",
        matchIfMissing = true)
public class ReintentoNotificacionesTarea {

    private static final Logger log = LoggerFactory.getLogger(ReintentoNotificacionesTarea.class);

    private final NotificacionService notificacionService;

    public ReintentoNotificacionesTarea(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Scheduled(fixedDelayString = "${app.notificaciones.reintento-ms:600000}",
            initialDelayString = "${app.notificaciones.reintento-ms:600000}")
    public void reintentar() {
        for (Long solicitudId : notificacionService.solicitudesParaReintentar()) {
            try {
                notificacionService.reintentarAutomaticamente(solicitudId);
            } catch (RuntimeException fallo) {
                // Un problema con una solicitud no detiene las demás.
                log.warn("No se pudieron reintentar los avisos de la solicitud {}: {}", solicitudId,
                        fallo.getMessage());
            }
        }
    }
}
