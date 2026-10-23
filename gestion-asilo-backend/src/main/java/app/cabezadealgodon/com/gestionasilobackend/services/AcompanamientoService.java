package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.AcompanamientoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.AcompanamientoResponse.CitaAsignada;
import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.models.Cita;
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

    /** Las de {@code enfermero}, y nunca las de otro: el id sale de su sesión, no de la petición. */
    @Transactional(readOnly = true)
    public List<AcompanamientoResponse> de(Usuario enfermero) {
        List<SolicitudReferencia> solicitudes = solicitudRepository.acompanadasPor(enfermero.getId());
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
