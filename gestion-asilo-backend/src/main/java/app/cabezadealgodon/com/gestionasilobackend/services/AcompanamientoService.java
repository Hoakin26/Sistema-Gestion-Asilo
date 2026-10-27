package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.AcompanamientoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.AcompanamientoResponse.CitaAsignada;
import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.models.Cita;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.CitaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.SolicitudReferenciaRepository;

/**
 * YS-188. Los acompañamientos de un enfermero(a): las solicitudes de
 * referencia en que el médico general lo eligió como acompañante (HU-03,
 * RF-03), con su cita si ya la tienen. Reemplaza a "Solicitudes asignadas" y
 * "Citas" del menú del diseño, que nunca se construyeron (decidido el 22/10).
 */
@Service
public class AcompanamientoService {

    private final SolicitudReferenciaRepository solicitudRepository;
    private final CitaRepository citaRepository;

    public AcompanamientoService(SolicitudReferenciaRepository solicitudRepository, CitaRepository citaRepository) {
        this.solicitudRepository = solicitudRepository;
        this.citaRepository = citaRepository;
    }

    /**
     * OBS-01. Lo que todavía va a pasar: por asignar, con cita o en atención.
     * Sin paginar, porque nunca son muchos.
     */
    static final Set<EstadoSolicitud> PROXIMOS = EnumSet.of(EstadoSolicitud.PENDIENTE_ASIGNACION,
            EstadoSolicitud.CITA_PROGRAMADA, EstadoSolicitud.EN_ATENCION);

    /** OBS-01. Lo que ya pasó, atendido o cancelado: crece con cada mes, así que va por páginas. */
    static final Set<EstadoSolicitud> ANTERIORES = EnumSet.complementOf(EnumSet.copyOf(PROXIMOS));

    /**
     * Los próximos de {@code enfermero}, y nunca los de otro: el id sale de su
     * sesión, no de la petición.
     */
    @Transactional(readOnly = true)
    public List<AcompanamientoResponse> proximos(Usuario enfermero) {
        return conCita(solicitudRepository.acompanadasPor(enfermero.getId(), PROXIMOS));
    }

    /** OBS-01. Una página de los anteriores de {@code enfermero}, el más reciente primero. */
    @Transactional(readOnly = true)
    public Page<AcompanamientoResponse> anteriores(Usuario enfermero, Pageable paginado) {
        Page<SolicitudReferencia> pagina = solicitudRepository.acompanadasPor(enfermero.getId(), ANTERIORES,
                paginado);
        return new PageImpl<>(conCita(pagina.getContent()), paginado, pagina.getTotalElements());
    }

    /** Las solicitudes, en el mismo orden, cada una con su cita si ya la tiene. */
    private List<AcompanamientoResponse> conCita(List<SolicitudReferencia> solicitudes) {
        Map<Long, Cita> citas = solicitudes.isEmpty() ? Map.of()
                : citaRepository.findBySolicitudIdIn(solicitudes.stream().map(SolicitudReferencia::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(cita -> cita.getSolicitud().getId(), Function.identity()));
        return solicitudes.stream().map(solicitud -> acompanamiento(solicitud, citas.get(solicitud.getId()))).toList();
    }

    private static AcompanamientoResponse acompanamiento(SolicitudReferencia solicitud, Cita cita) {
        return new AcompanamientoResponse(
                solicitud.getId(),
                solicitud.getNumero(),
                solicitud.getEstado(),
                new ResumenPersona(solicitud.getInterno().getId(), solicitud.getInterno().getNombreCompleto()),
                new EspecialidadResponse(solicitud.getEspecialidad().getId(), solicitud.getEspecialidad().getNombre()),
                solicitud.getMotivo(),
                persona(solicitud.getMedicoGeneral()),
                solicitud.getFechaCreacion(),
                cita == null ? null
                        : new CitaAsignada(cita.getId(), cita.getFechaHora(), cita.getDuracionMinutos(),
                                persona(cita.getEspecialista().getUsuario())));
    }

    private static ResumenPersona persona(Usuario usuario) {
        return new ResumenPersona(usuario.getId(), usuario.getNombreCompleto());
    }
}
