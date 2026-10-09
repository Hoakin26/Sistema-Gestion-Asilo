package app.cabezadealgodon.com.gestionasilobackend.services;

import java.time.Year;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.dto.SolicitudReferenciaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.SolicitudReferenciaResponse;
import app.cabezadealgodon.com.gestionasilobackend.eventos.SolicitudCreadaEvent;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.ContadorSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.Especialidad;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.Interno;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.ContadorSolicitudRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.InternoRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.SolicitudReferenciaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/** HU-03 (YS-84): el Médico General refiere a un interno a una especialidad (CU-001). */
@Service
public class SolicitudReferenciaService {

    private final SolicitudReferenciaRepository solicitudRepository;
    private final ContadorSolicitudRepository contadorRepository;
    private final InternoRepository internoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventos;

    public SolicitudReferenciaService(SolicitudReferenciaRepository solicitudRepository,
            ContadorSolicitudRepository contadorRepository, InternoRepository internoRepository,
            EspecialidadRepository especialidadRepository, UsuarioRepository usuarioRepository,
            ApplicationEventPublisher eventos) {
        this.solicitudRepository = solicitudRepository;
        this.contadorRepository = contadorRepository;
        this.internoRepository = internoRepository;
        this.especialidadRepository = especialidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventos = eventos;
    }

    /**
     * Crea la solicitud en PENDIENTE_ASIGNACION y con su número correlativo.
     *
     * <p>
     * Cada dato elegido se comprueba contra la base y, si no sirve, se rechaza
     * con el error en su campo: el formato ya lo validó el DTO, pero solo aquí
     * se sabe si el interno sigue activo o si el acompañante es de enfermería.
     */
    @Transactional
    public SolicitudReferenciaResponse crear(SolicitudReferenciaRequest request, Usuario medicoGeneral) {
        Interno interno = internoRepository.findById(request.internoId())
                .filter(Interno::isActivo)
                .orElseThrow(() -> new CampoInvalidoException("internoId",
                        "El interno seleccionado no existe o ya no está activo"));

        Especialidad especialidad = especialidadRepository.findById(request.especialidadId())
                .filter(Especialidad::isActiva)
                .orElseThrow(() -> new CampoInvalidoException("especialidadId",
                        "La especialidad seleccionada no existe o la fundación ya no la ofrece"));

        // RF-03: el acompañante es un enfermero(a). Asignar a alguien de otro
        // rol dejaría al interno sin quien lo acompañe de verdad.
        Usuario enfermero = usuarioRepository.findById(request.enfermeroAcompananteId())
                .filter(usuario -> usuario.isActivo()
                        && usuario.getRol().getNombre() == RolNombre.ENFERMERIA)
                .orElseThrow(() -> new CampoInvalidoException("enfermeroAcompananteId",
                        "El acompañante tiene que ser un enfermero(a) con cuenta activa"));

        SolicitudReferencia solicitud = new SolicitudReferencia();
        solicitud.setNumero(siguienteNumero());
        solicitud.setInterno(interno);
        solicitud.setEspecialidad(especialidad);
        // El usuario del token viene de otra sesión de Hibernate (la del
        // filtro JWT); se enlaza por referencia para no arrastrarlo desconectado.
        solicitud.setMedicoGeneral(usuarioRepository.getReferenceById(medicoGeneral.getId()));
        solicitud.setEnfermeroAcompanante(enfermero);
        solicitud.setMotivo(request.motivo().trim());
        solicitudRepository.save(solicitud);

        eventos.publishEvent(new SolicitudCreadaEvent(solicitud.getId()));

        return mapear(solicitud);
    }

    /**
     * YS-87. Las solicitudes, opcionalmente de un solo estado. Es la bandeja de
     * la Fundación, que pide las PENDIENTE_ASIGNACION (RF-05), y el
     * seguimiento del Médico General, que pide todas.
     *
     * <p>
     * El orden lo pone quien consulta: la Fundación las quiere de la más
     * antigua a la más reciente, porque la que más espera es la más urgente;
     * el médico, al revés, para ver primero las que acaba de crear.
     */
    @Transactional(readOnly = true)
    public Page<SolicitudReferenciaResponse> listar(EstadoSolicitud estado, Pageable paginado) {
        Page<SolicitudReferencia> pagina = estado == null
                ? solicitudRepository.findAll(paginado)
                : solicitudRepository.findByEstado(estado, paginado);
        return pagina.map(this::mapear);
    }

    /**
     * "REF-2026-0001", reiniciado cada año. El contador se bloquea antes de
     * leerse (ver {@code ContadorSolicitudRepository}), así que dos solicitudes
     * simultáneas esperan una a la otra en vez de obtener el mismo número. El
     * bloqueo dura hasta que termina la transacción, que es corta.
     */
    private String siguienteNumero() {
        int anio = Year.now().getValue();
        contadorRepository.asegurarAnio(anio);
        ContadorSolicitud contador = contadorRepository.bloquear(anio).orElseThrow();
        contador.setUltimo(contador.getUltimo() + 1);
        return "REF-%d-%04d".formatted(anio, contador.getUltimo());
    }

    private SolicitudReferenciaResponse mapear(SolicitudReferencia solicitud) {
        Interno interno = solicitud.getInterno();
        Usuario medicoGeneral = solicitud.getMedicoGeneral();
        Usuario enfermero = solicitud.getEnfermeroAcompanante();
        return new SolicitudReferenciaResponse(
                solicitud.getId(),
                solicitud.getNumero(),
                solicitud.getEstado(),
                solicitud.getMotivo(),
                new ResumenPersona(interno.getId(), interno.getNombreCompleto()),
                new EspecialidadResponse(solicitud.getEspecialidad().getId(),
                        solicitud.getEspecialidad().getNombre()),
                new ResumenPersona(medicoGeneral.getId(), medicoGeneral.getNombreCompleto()),
                new ResumenPersona(enfermero.getId(), enfermero.getNombreCompleto()),
                solicitud.getFechaCreacion(),
                // YS-94. Solo dice si habrá aviso; el aviso lo maneja el listener.
                tieneCorreo(interno.getFamiliarResponsable().getCorreo()));
    }

    private static boolean tieneCorreo(String correo) {
        return correo != null && !correo.isBlank();
    }
}
