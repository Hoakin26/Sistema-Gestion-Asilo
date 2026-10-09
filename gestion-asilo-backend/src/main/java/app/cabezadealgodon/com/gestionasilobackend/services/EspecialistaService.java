package app.cabezadealgodon.com.gestionasilobackend.services;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialistaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialistaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HorarioAtencionDto;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.ConflictoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Cita;
import app.cabezadealgodon.com.gestionasilobackend.models.Especialidad;
import app.cabezadealgodon.com.gestionasilobackend.models.HorarioAtencion;
import app.cabezadealgodon.com.gestionasilobackend.models.MedicoEspecialista;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.CitaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.MedicoEspecialistaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/**
 * HU-06 (YS-167). Los especialistas de la fundación, su horario y sus horas
 * libres. Lo administra la Fundación desde "Médicos y disponibilidad".
 */
@Service
public class EspecialistaService {

    private final MedicoEspecialistaRepository especialistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final CitaRepository citaRepository;

    public EspecialistaService(MedicoEspecialistaRepository especialistaRepository,
            UsuarioRepository usuarioRepository, EspecialidadRepository especialidadRepository,
            CitaRepository citaRepository) {
        this.especialistaRepository = especialistaRepository;
        this.usuarioRepository = usuarioRepository;
        this.especialidadRepository = especialidadRepository;
        this.citaRepository = citaRepository;
    }

    /**
     * Sin especialidad, todos (la pantalla de administración). Con
     * especialidad, solo los activos que la atienden: a quiénes se les puede
     * asignar una cita de esa especialidad.
     */
    @Transactional(readOnly = true)
    public List<EspecialistaResponse> listar(Long especialidadId) {
        List<MedicoEspecialista> especialistas = especialidadId == null
                ? especialistaRepository.todos()
                : especialistaRepository.activosDeEspecialidad(especialidadId);
        return especialistas.stream().map(this::mapear).toList();
    }

    /** Las cuentas de médico especialista a las que se les puede registrar perfil. */
    @Transactional(readOnly = true)
    public List<ResumenPersona> usuariosDisponibles() {
        return especialistaRepository.usuariosSinPerfil(RolNombre.MEDICO_ESPECIALISTA).stream()
                .map(usuario -> new ResumenPersona(usuario.getId(), usuario.getNombreCompleto()))
                .toList();
    }

    @Transactional
    public EspecialistaResponse registrar(EspecialistaRequest request) {
        if (request.usuarioId() == null) {
            throw new CampoInvalidoException("usuarioId", "Seleccione al médico especialista");
        }
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .filter(u -> u.isActivo() && u.getRol().getNombre() == RolNombre.MEDICO_ESPECIALISTA)
                .orElseThrow(() -> new CampoInvalidoException("usuarioId",
                        "Tiene que ser una cuenta activa con rol de Médico Especialista"));
        if (especialistaRepository.existsByUsuarioId(usuario.getId())) {
            throw new ConflictoException(usuario.getNombreCompleto() + " ya tiene un perfil de especialista");
        }

        MedicoEspecialista especialista = new MedicoEspecialista();
        especialista.setUsuario(usuario);
        aplicar(request, especialista);
        return mapear(especialistaRepository.save(especialista));
    }

    @Transactional
    public EspecialistaResponse actualizar(Long id, EspecialistaRequest request) {
        MedicoEspecialista especialista = especialistaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un especialista con id " + id));
        aplicar(request, especialista);
        if (request.activo() != null) {
            especialista.setActivo(request.activo());
        }
        return mapear(especialistaRepository.save(especialista));
    }

    /**
     * Las horas libres de un día: las del horario que todavía no pasaron y
     * que no se cruzan con una cita ya dada. Es lo que la Fundación ve como
     * botones al asignar la cita.
     */
    @Transactional(readOnly = true)
    public List<LocalTime> disponibilidad(Long id, LocalDate fecha) {
        MedicoEspecialista especialista = especialistaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un especialista con id " + id));
        if (!especialista.isActivo()) return List.of();

        List<Cita> citas = citaRepository.delEspecialistaEntre(id, fecha.atStartOfDay(),
                fecha.plusDays(1).atStartOfDay());
        LocalDateTime ahora = LocalDateTime.now();
        int duracion = especialista.getDuracionCitaMinutos();

        return Agenda.franjas(especialista, fecha).stream()
                .filter(hora -> fecha.atTime(hora).isAfter(ahora))
                .filter(hora -> !Agenda.ocupada(fecha.atTime(hora), duracion, citas))
                .toList();
    }

    // ---- apoyo ----

    /**
     * Reglas del horario que el DTO no puede ver solo: que el fin sea
     * posterior al inicio, que alcance para al menos una cita y que no haya
     * dos bloques para el mismo día. El error va al campo exacto
     * ("horarios[2].horaFin") para que la pantalla lo señale en su fila.
     */
    private void aplicar(EspecialistaRequest request, MedicoEspecialista especialista) {
        Especialidad especialidad = especialidadRepository.findById(request.especialidadId())
                .filter(Especialidad::isActiva)
                .orElseThrow(() -> new CampoInvalidoException("especialidadId",
                        "La especialidad seleccionada no existe o ya no se ofrece"));

        int duracion = request.duracionCitaMinutos();
        Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        for (int i = 0; i < request.horarios().size(); i++) {
            HorarioAtencionDto horario = request.horarios().get(i);
            if (!dias.add(horario.dia())) {
                throw new CampoInvalidoException("horarios[%d].dia".formatted(i), "Ese día ya está en el horario");
            }
            if (!horario.horaFin().isAfter(horario.horaInicio())) {
                throw new CampoInvalidoException("horarios[%d].horaFin".formatted(i),
                        "La hora de fin tiene que ser posterior a la de inicio");
            }
            if (Duration.between(horario.horaInicio(), horario.horaFin()).toMinutes() < duracion) {
                throw new CampoInvalidoException("horarios[%d].horaFin".formatted(i),
                        "El horario no alcanza para una cita de %d minutos".formatted(duracion));
            }
        }

        especialista.setEspecialidad(especialidad);
        especialista.setDuracionCitaMinutos(duracion);

        /*
         * El horario se manda como debe quedar, pero no se borra y se vuelve a
         * crear como las listas de la ficha médica: Hibernate inserta las
         * filas nuevas antes de borrar las viejas, y el mismo día dos veces
         * choca contra la restricción única (especialista, día). Lo descubrió
         * la prueba de edición: cambiar cualquier dato respondía 409. Por eso
         * cada día se actualiza en su lugar, y solo se quitan los que ya no
         * vienen y se agregan los nuevos.
         */
        Set<DayOfWeek> pedidos = EnumSet.noneOf(DayOfWeek.class);
        request.horarios().forEach(dto -> pedidos.add(dto.dia()));
        especialista.getHorarios().removeIf(horario -> !pedidos.contains(horario.getDia()));
        for (HorarioAtencionDto dto : request.horarios()) {
            HorarioAtencion horario = especialista.horarioDe(dto.dia()).orElseGet(() -> {
                HorarioAtencion nuevo = new HorarioAtencion();
                nuevo.setDia(dto.dia());
                especialista.agregarHorario(nuevo);
                return nuevo;
            });
            horario.setHoraInicio(dto.horaInicio());
            horario.setHoraFin(dto.horaFin());
        }
    }

    private EspecialistaResponse mapear(MedicoEspecialista especialista) {
        Usuario usuario = especialista.getUsuario();
        Especialidad especialidad = especialista.getEspecialidad();
        List<HorarioAtencionDto> horarios = especialista.getHorarios().stream()
                .sorted(Comparator.comparing(HorarioAtencion::getDia))
                .map(h -> new HorarioAtencionDto(h.getDia(), h.getHoraInicio(), h.getHoraFin()))
                .toList();
        return new EspecialistaResponse(
                especialista.getId(),
                new ResumenPersona(usuario.getId(), usuario.getNombreCompleto()),
                new EspecialidadResponse(especialidad.getId(), especialidad.getNombre()),
                especialista.getDuracionCitaMinutos(),
                especialista.isActivo(),
                horarios);
    }
}
