package app.cabezadealgodon.com.gestionasilobackend.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.AsignacionCitaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.CitaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.eventos.CitaAsignadaEvent;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.ConflictoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Cita;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.MedicoEspecialista;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.CitaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.MedicoEspecialistaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.SolicitudReferenciaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/**
 * HU-06 (YS-91). La Fundación le asigna especialista, fecha y hora a una
 * solicitud de referencia (RF-05, CU-002).
 */
@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final SolicitudReferenciaRepository solicitudRepository;
    private final MedicoEspecialistaRepository especialistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventos;

    public CitaService(CitaRepository citaRepository, SolicitudReferenciaRepository solicitudRepository,
            MedicoEspecialistaRepository especialistaRepository, UsuarioRepository usuarioRepository,
            ApplicationEventPublisher eventos) {
        this.citaRepository = citaRepository;
        this.solicitudRepository = solicitudRepository;
        this.especialistaRepository = especialistaRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventos = eventos;
    }

    /**
     * Asigna la cita y lleva la solicitud a CITA_PROGRAMADA, en la misma
     * transacción: una cita sin solicitud actualizada, o al revés, dejaría al
     * sistema contando dos historias distintas.
     *
     * <p>
     * Las comprobaciones van de lo general a lo particular, y cada una con su
     * código: la solicitud tiene que estar esperando (409 si no), el
     * especialista tiene que servir para ella (400), la hora tiene que ser una
     * de su horario (400) y tiene que estar libre (409).
     */
    @Transactional
    public CitaResponse asignar(Long solicitudId, AsignacionCitaRequest request, Usuario fundacion) {
        SolicitudReferencia solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una solicitud de referencia con id " + solicitudId));

        // Antes que nada: si ya no está pendiente, el resto da igual. La
        // entidad aplica el diagrama DE-01 y responde 409.
        solicitud.cambiarEstado(EstadoSolicitud.CITA_PROGRAMADA);

        // Bloqueado hasta el final de la transacción: dos asignaciones para el
        // mismo especialista se hacen una tras otra, y la segunda ya ve la
        // cita de la primera.
        MedicoEspecialista especialista = especialistaRepository.bloquear(request.especialistaId())
                .filter(MedicoEspecialista::isActivo)
                .orElseThrow(() -> new CampoInvalidoException("especialistaId",
                        "El especialista seleccionado no existe o ya no está atendiendo"));
        if (!especialista.getEspecialidad().getId().equals(solicitud.getEspecialidad().getId())) {
            throw new CampoInvalidoException("especialistaId",
                    "Ese especialista no atiende " + solicitud.getEspecialidad().getNombre());
        }

        LocalDateTime inicio = request.fechaHora().truncatedTo(ChronoUnit.MINUTES);
        if (!inicio.isAfter(LocalDateTime.now())) {
            throw new CampoInvalidoException("fechaHora", "La cita tiene que ser en una fecha y hora futuras");
        }
        if (!Agenda.franjas(especialista, inicio.toLocalDate()).contains(inicio.toLocalTime())) {
            throw new CampoInvalidoException("fechaHora",
                    "Esa hora no es una de las franjas del horario de atención del especialista");
        }

        List<Cita> delDia = citaRepository.delEspecialistaEntre(especialista.getId(),
                inicio.toLocalDate().atStartOfDay(), inicio.toLocalDate().plusDays(1).atStartOfDay());
        if (Agenda.ocupada(inicio, especialista.getDuracionCitaMinutos(), delDia)) {
            throw new ConflictoException("El especialista ya tiene una cita a esa hora. Elija otra hora libre.");
        }

        Cita cita = new Cita();
        cita.setSolicitud(solicitud);
        cita.setEspecialista(especialista);
        cita.setFechaHora(inicio);
        cita.setDuracionMinutos(especialista.getDuracionCitaMinutos());
        cita.setAsignadaPor(usuarioRepository.getReferenceById(fundacion.getId()));
        citaRepository.save(cita);
        solicitudRepository.save(solicitud);

        // RF-04: el segundo aviso al familiar, ahora con el especialista.
        eventos.publishEvent(new CitaAsignadaEvent(solicitud.getId()));

        return mapear(cita);
    }

    /** La cita de una solicitud, para su detalle. 404 si todavía no tiene. */
    @Transactional(readOnly = true)
    public CitaResponse deSolicitud(Long solicitudId) {
        return citaRepository.findBySolicitudId(solicitudId).map(this::mapear)
                .orElseThrow(() -> new RecursoNoEncontradoException("La solicitud todavía no tiene cita asignada"));
    }

    private CitaResponse mapear(Cita cita) {
        MedicoEspecialista especialista = cita.getEspecialista();
        return new CitaResponse(
                cita.getId(),
                cita.getSolicitud().getId(),
                new ResumenPersona(especialista.getUsuario().getId(), especialista.getUsuario().getNombreCompleto()),
                new EspecialidadResponse(especialista.getEspecialidad().getId(),
                        especialista.getEspecialidad().getNombre()),
                cita.getFechaHora(),
                cita.getDuracionMinutos());
    }
}
