package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.NotificacionResponse;
import app.cabezadealgodon.com.gestionasilobackend.security.MedicoGeneralOFundacion;
import app.cabezadealgodon.com.gestionasilobackend.services.NotificacionService;

/**
 * HU-04 (YS-95). Los avisos al familiar de una solicitud: ver en qué quedaron
 * y reintentarlos.
 *
 * <p>
 * Los usan quienes dan seguimiento a la solicitud: el Médico General que la
 * creó y la Fundación que la atiende. El botón para reintentar aparece en la
 * lista de solicitudes, que llega con HU-05.
 */
@RestController
@RequestMapping("/api/solicitudes-referencia/{solicitudId}/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    @MedicoGeneralOFundacion
    public List<NotificacionResponse> listar(@PathVariable Long solicitudId) {
        return notificacionService.listar(solicitudId);
    }

    /** Reintenta en el momento, sin esperar al reintento automático. */
    @PostMapping("/reintentar")
    @MedicoGeneralOFundacion
    public List<NotificacionResponse> reintentar(@PathVariable Long solicitudId) {
        return notificacionService.reintentar(solicitudId);
    }
}
