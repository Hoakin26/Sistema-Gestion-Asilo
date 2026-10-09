package app.cabezadealgodon.com.gestionasilobackend.services;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import app.cabezadealgodon.com.gestionasilobackend.dto.NotificacionResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Cita;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.models.Familiar;
import app.cabezadealgodon.com.gestionasilobackend.models.NotificacionFamiliar;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.repository.CitaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.NotificacionFamiliarRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.SolicitudReferenciaRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * HU-04 (YS-94, YS-95). El aviso por correo al familiar responsable (RF-04).
 *
 * <p>
 * Ningún método de aquí lanza una excepción por un fallo del correo: lo
 * registran como {@code FALLIDA} y siguen. Un servidor de correo caído nunca
 * debe impedir ni revertir una solicitud (criterio de HU-04).
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    /**
     * Un aviso pendiente se deja en paz este tiempo antes de que el reintento
     * automático lo tome: es el margen del primer envío, que corre en otro
     * hilo apenas se confirma la solicitud.
     */
    private static final long MINUTOS_DE_GRACIA = 2;

    private static final Locale ES_GT = Locale.forLanguageTag("es-GT");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_GT);
    /** YS-91. "jueves 15 de octubre de 2026": en una cita importa el día de la semana. */
    private static final DateTimeFormatter FECHA_CON_DIA = DateTimeFormatter
            .ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES_GT);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm a", ES_GT);

    private final NotificacionFamiliarRepository notificacionRepository;
    private final SolicitudReferenciaRepository solicitudRepository;
    private final CitaRepository citaRepository;
    private final JavaMailSender correo;
    private final String remitente;
    private final String remitenteNombre;
    private final int maxIntentos;

    public NotificacionService(NotificacionFamiliarRepository notificacionRepository,
            SolicitudReferenciaRepository solicitudRepository, CitaRepository citaRepository,
            JavaMailSender correo,
            @Value("${app.correo.remitente:no-responder@cabezadealgodon.test}") String remitente,
            @Value("${app.correo.remitente-nombre:Asilo de Ancianos Cabeza de Algodón}") String remitenteNombre,
            @Value("${app.notificaciones.max-intentos:5}") int maxIntentos) {
        this.notificacionRepository = notificacionRepository;
        this.solicitudRepository = solicitudRepository;
        this.citaRepository = citaRepository;
        this.correo = correo;
        this.remitente = remitente;
        this.remitenteNombre = remitenteNombre;
        this.maxIntentos = maxIntentos;
    }

    /**
     * Registra el aviso como pendiente. Se llama dentro de la transacción que
     * crea la solicitud: o quedan las dos, o ninguna.
     */
    @Transactional
    public void registrar(Long solicitudId, TipoNotificacion tipo) {
        NotificacionFamiliar notificacion = new NotificacionFamiliar();
        notificacion.setSolicitud(solicitudRepository.getReferenceById(solicitudId));
        notificacion.setTipo(tipo);
        notificacionRepository.save(notificacion);
    }

    /** El primer envío, que dispara la solicitud recién confirmada. */
    @Transactional
    public void enviarPendientes(Long solicitudId) {
        procesar(solicitudId, EnumSet.of(EstadoNotificacion.PENDIENTE), false);
    }

    /**
     * El reintento automático (YS-95): pendientes atrasados y fallidos que no
     * agotaron sus intentos. Los "sin correo" no: no hay a dónde mandarlos.
     */
    @Transactional
    public void reintentarAutomaticamente(Long solicitudId) {
        procesar(solicitudId, EnumSet.of(EstadoNotificacion.PENDIENTE, EstadoNotificacion.FALLIDA), true);
    }

    /**
     * El reintento que pide una persona. No respeta el máximo de intentos, que
     * solo frena a la máquina, y vuelve a mirar los "sin correo": quien
     * reintenta puede haber agregado el correo del familiar justo antes.
     */
    @Transactional
    public List<NotificacionResponse> reintentar(Long solicitudId) {
        verificarSolicitud(solicitudId);
        procesar(solicitudId, EnumSet.of(EstadoNotificacion.PENDIENTE, EstadoNotificacion.FALLIDA,
                EstadoNotificacion.SIN_CORREO), false);
        return listarSinVerificar(solicitudId);
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(Long solicitudId) {
        verificarSolicitud(solicitudId);
        return listarSinVerificar(solicitudId);
    }

    /** Las solicitudes con algún aviso que el reintento automático debe tomar. */
    @Transactional(readOnly = true)
    public List<Long> solicitudesParaReintentar() {
        return notificacionRepository.solicitudesParaReintentar(EstadoNotificacion.FALLIDA,
                EstadoNotificacion.PENDIENTE, maxIntentos, LocalDateTime.now().minusMinutes(MINUTOS_DE_GRACIA));
    }

    // ---- envío ----

    private void procesar(Long solicitudId, Set<EstadoNotificacion> estados, boolean respetarMaximo) {
        for (NotificacionFamiliar notificacion : notificacionRepository.bloquearDeSolicitud(solicitudId, estados)) {
            if (respetarMaximo && notificacion.getIntentos() >= maxIntentos) {
                continue;
            }
            intentar(notificacion);
        }
    }

    private void intentar(NotificacionFamiliar notificacion) {
        SolicitudReferencia solicitud = notificacion.getSolicitud();
        Familiar familiar = solicitud.getInterno().getFamiliarResponsable();

        // Se lee en cada intento, no se guarda de antemano: si el correo
        // estaba mal escrito y lo corrigieron, este intento ya va al bueno.
        String direccion = familiar.getCorreo();
        if (direccion == null || direccion.isBlank()) {
            notificacion.setEstado(EstadoNotificacion.SIN_CORREO);
            notificacion.setDestinatario(null);
            log.info("Aviso {} de la solicitud {}: el familiar no tiene correo registrado",
                    notificacion.getTipo(), solicitud.getNumero());
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        notificacion.setIntentos(notificacion.getIntentos() + 1);
        notificacion.setFechaUltimoIntento(ahora);
        notificacion.setDestinatario(direccion.trim());

        try {
            correo.send(armarMensaje(notificacion, familiar, direccion.trim()));
            notificacion.setEstado(EstadoNotificacion.ENVIADA);
            notificacion.setFechaEnvio(ahora);
            notificacion.setUltimoError(null);
            log.info("Aviso {} de la solicitud {} enviado al familiar", notificacion.getTipo(),
                    solicitud.getNumero());
        } catch (MailException | MessagingException | UnsupportedEncodingException fallo) {
            notificacion.setEstado(EstadoNotificacion.FALLIDA);
            notificacion.setUltimoError(recortar(fallo.getMessage()));
            // WARN y no ERROR: es un caso previsto, que el reintento resuelve solo.
            log.warn("No se pudo enviar el aviso {} de la solicitud {} (intento {}): {}", notificacion.getTipo(),
                    solicitud.getNumero(), notificacion.getIntentos(), fallo.getMessage());
        }
    }

    private MimeMessage armarMensaje(NotificacionFamiliar notificacion, Familiar familiar, String direccion)
            throws MessagingException, UnsupportedEncodingException {
        Contenido contenido = switch (notificacion.getTipo()) {
            case SOLICITUD_CREADA -> solicitudCreada(notificacion.getSolicitud());
            case CITA_ASIGNADA -> citaAsignada(notificacion.getSolicitud());
        };

        MimeMessage mensaje = correo.createMimeMessage();
        // multipart: texto plano y HTML juntos, para que cada programa de correo
        // muestre el que sepa mostrar.
        MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, "UTF-8");
        ayudante.setFrom(remitente, remitenteNombre);
        ayudante.setTo(direccion);
        ayudante.setSubject(contenido.asunto());
        ayudante.setText(texto(contenido, familiar), html(contenido, familiar));
        return mensaje;
    }

    /*
     * Qué dice cada correo y, sobre todo, qué no. Llevan lo que piden HU-04 y
     * el RF-04 —la referencia, la especialidad, el estado y, al asignar la
     * cita, el especialista con la fecha y la hora— y dejan fuera el motivo de
     * la referencia: es un dato clínico (RNF-03), y el correo es un canal que
     * el asilo no controla una vez enviado. Quien quiera el detalle lo pide en
     * el asilo.
     */

    /** Lo que cambia entre un aviso y otro; el saludo, el formato y la firma son comunes. */
    private record Contenido(String asunto, String introduccion, List<String[]> filas, String cierre) {
    }

    private Contenido solicitudCreada(SolicitudReferencia solicitud) {
        return new Contenido(
                "Solicitud de referencia " + solicitud.getNumero() + ": " + solicitud.getInterno().getNombreCompleto(),
                "Le informamos que el Médico General del asilo refirió a %s a la especialidad de %s, para ser atendido(a) por un especialista de la fundación."
                        .formatted(solicitud.getInterno().getNombreCompleto(), solicitud.getEspecialidad().getNombre()),
                List.of(
                        new String[] { "Número de solicitud", solicitud.getNumero() },
                        new String[] { "Especialidad", solicitud.getEspecialidad().getNombre() },
                        new String[] { "Estado", solicitud.getEstado().etiqueta() },
                        new String[] { "Fecha de la solicitud", solicitud.getFechaCreacion().format(FECHA) }),
                "La fundación asignará el especialista, la fecha y la hora de la cita. Le avisaremos por este medio cuando la cita esté programada.");
    }

    /** YS-91. El segundo aviso del RF-04: con quién y cuándo. */
    private Contenido citaAsignada(SolicitudReferencia solicitud) {
        Cita cita = citaRepository.findBySolicitudId(solicitud.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "La solicitud " + solicitud.getNumero() + " no tiene cita que avisar"));
        String especialista = cita.getEspecialista().getUsuario().getNombreCompleto();
        return new Contenido(
                "Cita programada " + solicitud.getNumero() + ": " + solicitud.getInterno().getNombreCompleto(),
                "Le informamos que la fundación programó la cita de %s con un especialista de %s."
                        .formatted(solicitud.getInterno().getNombreCompleto(), solicitud.getEspecialidad().getNombre()),
                List.of(
                        new String[] { "Número de solicitud", solicitud.getNumero() },
                        new String[] { "Especialista", especialista },
                        new String[] { "Especialidad", solicitud.getEspecialidad().getNombre() },
                        new String[] { "Fecha", cita.getFechaHora().format(FECHA_CON_DIA) },
                        new String[] { "Hora", cita.getFechaHora().format(HORA) },
                        new String[] { "Estado", solicitud.getEstado().etiqueta() }),
                "El interno irá a la cita acompañado por personal de enfermería del asilo.");
    }

    private String texto(Contenido contenido, Familiar familiar) {
        StringBuilder filas = new StringBuilder();
        for (String[] fila : contenido.filas()) {
            filas.append(fila[0]).append(": ").append(fila[1]).append('\n');
        }
        return """
                Estimado(a) %s:

                %s

                %s
                %s

                Este es un mensaje automático; por favor no lo responda. Si tiene preguntas, comuníquese directamente con el asilo.

                %s
                """.formatted(familiar.getNombreCompleto(), contenido.introduccion(), filas, contenido.cierre(),
                remitenteNombre);
    }

    /**
     * La misma información en HTML, con los colores del manual de diseño.
     * Todo lo que viene de la base pasa por {@code htmlEscape}: un nombre con
     * "&lt;" no puede romper ni inyectar nada en el correo.
     */
    private String html(Contenido contenido, Familiar familiar) {
        StringBuilder filas = new StringBuilder();
        for (String[] fila : contenido.filas()) {
            filas.append("""
                    <tr><td style="padding:4px 16px 4px 0;color:#64748B">%s</td>\
                    <td style="padding:4px 0;color:#1E293B;font-weight:600">%s</td></tr>
                    """.formatted(e(fila[0]), e(fila[1])));
        }

        return """
                <!DOCTYPE html>
                <html lang="es">
                <body style="margin:0;padding:24px;background:#F8FAFC;font-family:Inter,Arial,sans-serif;font-size:16px;line-height:1.5;color:#1E293B">
                  <div style="max-width:560px;margin:0 auto;background:#FFFFFF;border:1px solid #E2E8F0;border-radius:8px;padding:24px">
                    <p style="margin:0 0 16px;color:#2563EB;font-weight:700">%s</p>
                    <p style="margin:0 0 16px">Estimado(a) %s:</p>
                    <p style="margin:0 0 16px">%s</p>
                    <table style="border-collapse:collapse;margin:0 0 16px">%s</table>
                    <p style="margin:0 0 16px">%s</p>
                    <p style="margin:0;color:#64748B;font-size:14px">Este es un mensaje automático; por favor no lo responda. Si tiene preguntas, comuníquese directamente con el asilo.</p>
                  </div>
                </body>
                </html>
                """.formatted(
                e(remitenteNombre),
                e(familiar.getNombreCompleto()),
                e(contenido.introduccion()),
                filas,
                e(contenido.cierre()));
    }

    private static String e(String texto) {
        return HtmlUtils.htmlEscape(texto, "UTF-8");
    }

    // ---- apoyo ----

    private List<NotificacionResponse> listarSinVerificar(Long solicitudId) {
        return notificacionRepository.findBySolicitudIdOrderByIdAsc(solicitudId).stream()
                .map(n -> new NotificacionResponse(n.getId(), n.getTipo(), n.getEstado(), n.getIntentos(),
                        n.getFechaUltimoIntento(), n.getFechaEnvio()))
                .toList();
    }

    private void verificarSolicitud(Long solicitudId) {
        if (!solicitudRepository.existsById(solicitudId)) {
            throw new RecursoNoEncontradoException("No existe una solicitud de referencia con id " + solicitudId);
        }
    }

    private static String recortar(String mensaje) {
        if (mensaje == null) return "Error sin detalle";
        return mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje;
    }
}
