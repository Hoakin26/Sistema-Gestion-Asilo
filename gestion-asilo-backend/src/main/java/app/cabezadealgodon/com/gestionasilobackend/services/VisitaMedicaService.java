package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.ArchivoResultadoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.CitaAgendaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.DiagnosticoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ExamenLaboratorioResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistorialMedicoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.MedicamentoRecetaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.OrdenLaboratorioResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.OrdenesLaboratorioRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.RecetaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.RecetaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.dto.VisitaMedicaResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.ConflictoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.OperacionNoPermitidaException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Cita;
import app.cabezadealgodon.com.gestionasilobackend.models.DetalleReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.Especialidad;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.ExamenLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;
import app.cabezadealgodon.com.gestionasilobackend.models.Interno;
import app.cabezadealgodon.com.gestionasilobackend.models.OrdenLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.models.Receta;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.CitaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.ExamenLaboratorioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.MedicoEspecialistaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.OrdenLaboratorioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.RecetaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.VisitaMedicaRepository;

/**
 * HU-07 (YS-99). La agenda del especialista y el inicio de la visita médica
 * (RF-06, CU-003). HU-08 agrega el historial, HU-09 el diagnóstico y el
 * cierre, HU-10 la receta y HU-11 los exámenes de laboratorio.
 *
 * <p>
 * Toda operación recibe la cuenta del especialista que la pide: una cita y una
 * visita son de un especialista concreto, y otro especialista no las puede
 * tocar aunque tenga el mismo rol.
 */
@Service
public class VisitaMedicaService {

    private static final String SIN_PERFIL = "Su cuenta todavía no tiene perfil de especialista. "
            + "Pídale a la Fundación que lo registre en «Médicos y disponibilidad».";

    /** "lunes 12 de octubre", para decirle desde qué día puede iniciar. */
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",
            Locale.forLanguageTag("es-GT"));

    private final CitaRepository citaRepository;
    private final VisitaMedicaRepository visitaRepository;
    private final MedicoEspecialistaRepository especialistaRepository;
    private final HistorialMedicoService historialService;
    private final CargoService cargoService;
    private final RecetaRepository recetaRepository;
    private final ExamenLaboratorioRepository examenRepository;
    private final OrdenLaboratorioRepository ordenRepository;

    public VisitaMedicaService(CitaRepository citaRepository, VisitaMedicaRepository visitaRepository,
            MedicoEspecialistaRepository especialistaRepository, HistorialMedicoService historialService,
            CargoService cargoService, RecetaRepository recetaRepository,
            ExamenLaboratorioRepository examenRepository, OrdenLaboratorioRepository ordenRepository) {
        this.citaRepository = citaRepository;
        this.visitaRepository = visitaRepository;
        this.especialistaRepository = especialistaRepository;
        this.historialService = historialService;
        this.cargoService = cargoService;
        this.recetaRepository = recetaRepository;
        this.examenRepository = examenRepository;
        this.ordenRepository = ordenRepository;
    }

    /**
     * Las citas del especialista en un día, en orden de hora. Con cada una va
     * el id de su visita, si ya se inició.
     */
    @Transactional(readOnly = true)
    public List<CitaAgendaResponse> agenda(Usuario medico, LocalDate fecha) {
        if (!especialistaRepository.existsByUsuarioId(medico.getId())) {
            throw new RecursoNoEncontradoException(SIN_PERFIL);
        }
        List<Cita> citas = citaRepository.agendaDe(medico.getId(), fecha.atStartOfDay(),
                fecha.plusDays(1).atStartOfDay());
        Map<Long, Long> visitas = citas.isEmpty()
                ? Map.of()
                : visitaRepository.deCitas(citas.stream().map(Cita::getId).toList()).stream()
                        .collect(Collectors.toMap(VisitaMedicaRepository.VisitaDeCita::getCitaId,
                                VisitaMedicaRepository.VisitaDeCita::getVisitaId));

        return citas.stream().map(cita -> {
            SolicitudReferencia solicitud = cita.getSolicitud();
            return new CitaAgendaResponse(
                    cita.getId(),
                    cita.getFechaHora(),
                    cita.getDuracionMinutos(),
                    solicitud.getId(),
                    solicitud.getNumero(),
                    solicitud.getEstado(),
                    solicitud.getMotivo(),
                    new ResumenPersona(solicitud.getInterno().getId(), solicitud.getInterno().getNombreCompleto()),
                    especialidad(solicitud.getEspecialidad()),
                    persona(solicitud.getEnfermeroAcompanante()),
                    visitas.get(cita.getId()));
        }).toList();
    }

    /**
     * Convierte la cita en una visita activa (criterios de HU-07). En este
     * orden, de lo que no depende del estado a lo que sí:
     * <ol>
     * <li>la cita existe (404);</li>
     * <li>es de este especialista (403);</li>
     * <li>su visita no se inició ya (409);</li>
     * <li>ya llegó el día de la cita (409): ese día o después, nunca antes,
     * decidido el 09/10 para que no se inicie por error una cita de la semana
     * siguiente, sin bloquear una atrasada;</li>
     * <li>la solicitud está en Cita Programada (409, lo aplica la entidad).</li>
     * </ol>
     * Al iniciar, la solicitud pasa a En Atención y la visita copia la tarifa
     * de la especialidad (YS-169).
     */
    @Transactional
    public VisitaMedicaResponse iniciar(Long citaId, Usuario medico) {
        Cita cita = citaRepository.conSolicitud(citaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una cita con id " + citaId));
        if (!cita.getEspecialista().getUsuario().getId().equals(medico.getId())) {
            throw new OperacionNoPermitidaException(
                    "Esta cita está asignada a otro especialista: solo quien la tiene asignada puede iniciar la visita");
        }
        if (visitaRepository.existsByCitaId(citaId)) {
            throw new ConflictoException("La visita de esta cita ya se inició");
        }
        LocalDate dia = cita.getFechaHora().toLocalDate();
        if (LocalDate.now().isBefore(dia)) {
            throw new ConflictoException("La cita es el " + DIA.format(dia)
                    + ": la visita se puede iniciar desde ese día");
        }

        SolicitudReferencia solicitud = cita.getSolicitud();
        solicitud.cambiarEstado(EstadoSolicitud.EN_ATENCION);

        VisitaMedica visita = new VisitaMedica();
        visita.setCita(cita);
        visita.setInterno(solicitud.getInterno());
        visita.setEspecialista(cita.getEspecialista());
        visita.setFechaInicio(LocalDateTime.now());
        visita.setCostoConsulta(solicitud.getEspecialidad().getTarifaConsulta());
        visita.setEstado(EstadoVisita.ACTIVA);
        // Con flush aquí: si otra petición inició la misma visita a la vez, la
        // restricción única salta dentro de esta llamada y no al confirmar.
        return mapear(visitaRepository.saveAndFlush(visita));
    }

    /** El módulo "Visitas médicas": las del especialista, la más reciente primero. */
    @Transactional(readOnly = true)
    public List<VisitaMedicaResponse> listar(Usuario medico, EstadoVisita estado) {
        List<VisitaMedica> visitas = estado == null
                ? visitaRepository.delEspecialista(medico.getId())
                : visitaRepository.delEspecialistaEnEstado(medico.getId(), estado);
        return visitas.stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public VisitaMedicaResponse detalle(Long id, Usuario medico) {
        VisitaMedica visita = visitaRepository.conDetalle(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una visita con id " + id));
        if (!visita.getEspecialista().getUsuario().getId().equals(medico.getId())) {
            throw new OperacionNoPermitidaException("Esta visita es de otro especialista");
        }
        return mapear(visita);
    }

    /**
     * YS-102. Los antecedentes del paciente durante la consulta (HU-08,
     * RF-07): solo para el especialista de la visita y solo mientras está
     * activa. Al finalizarla el acceso se cierra, a diferencia de la ficha
     * médica, que el especialista puede leer siempre (YS-58).
     */
    @Transactional(readOnly = true)
    public HistorialMedicoResponse historial(Long visitaId, Usuario medico) {
        VisitaMedica visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una visita con id " + visitaId));
        if (!visita.getEspecialista().getUsuario().getId().equals(medico.getId())) {
            throw new OperacionNoPermitidaException("Esta visita es de otro especialista");
        }
        if (visita.getEstado() != EstadoVisita.ACTIVA) {
            throw new OperacionNoPermitidaException(
                    "Los antecedentes del paciente se consultan solo durante la visita activa");
        }
        return historialService.de(visita.getInterno().getId(), visita.getId());
    }

    /**
     * YS-105. Guarda el diagnóstico y las observaciones de la consulta (HU-09).
     * Se puede guardar las veces que haga falta mientras la visita está
     * activa; finalizada, ya no admite cambios.
     */
    @Transactional
    public VisitaMedicaResponse registrarDiagnostico(Long visitaId, DiagnosticoRequest request, Usuario medico) {
        VisitaMedica visita = activaDe(visitaRepository.bloquear(visitaId), visitaId, medico);
        visita.setDiagnostico(request.diagnostico().trim());
        visita.setObservaciones(opcional(request.observaciones()));
        return mapear(visita);
    }

    /**
     * YS-105. Cierra la visita (HU-09, CU-003 E.1). En este orden:
     * <ol>
     * <li>es de este especialista (403) y sigue activa (409);</li>
     * <li>tiene diagnóstico (400, en el campo): no se finaliza sin él;</li>
     * <li>hay tarifa de consulta (409): la que se copió al iniciar o, si la
     * especialidad no tenía, la que la Fundación haya definido después.
     * Decidido el 10/10: sin tarifa no se finaliza, para que ningún cargo
     * nazca vacío. El diagnóstico ya guardado no se pierde.</li>
     * </ol>
     * Al finalizar, la solicitud pasa a Atendida (DE-01) y se registra el
     * cargo de la consulta para la cuenta del familiar (HU-18).
     *
     * <p>
     * La visita se lee bloqueada: dos "Finalizar" a la vez no generan dos
     * cargos, porque el segundo ya la encuentra finalizada.
     */
    @Transactional
    public VisitaMedicaResponse finalizar(Long visitaId, Usuario medico) {
        VisitaMedica visita = activaDe(visitaRepository.bloquear(visitaId), visitaId, medico);
        if (visita.getDiagnostico() == null || visita.getDiagnostico().isBlank()) {
            throw new CampoInvalidoException("diagnostico", "Registre el diagnóstico antes de finalizar la visita");
        }

        SolicitudReferencia solicitud = visita.getCita().getSolicitud();
        Especialidad especialidad = solicitud.getEspecialidad();
        BigDecimal costo = visita.getCostoConsulta() != null
                ? visita.getCostoConsulta()
                : especialidad.getTarifaConsulta();
        if (costo == null) {
            throw new ConflictoException("La consulta de " + especialidad.getNombre()
                    + " todavía no tiene tarifa, así que la visita no se puede finalizar. "
                    + "Pídale a la Fundación que la defina en «Médicos y disponibilidad». "
                    + "El diagnóstico ya quedó guardado.");
        }

        visita.setCostoConsulta(costo);
        visita.setEstado(EstadoVisita.FINALIZADA);
        visita.setFechaCierre(LocalDateTime.now());
        solicitud.cambiarEstado(EstadoSolicitud.ATENDIDA);
        cargoService.registrar(visita, TipoServicio.CONSULTA, costo);
        return mapear(visita);
    }

    /**
     * YS-108. Registra la receta de la visita (HU-10, RF-10). Una por visita:
     * si ya tiene, se corrige con {@link #corregirReceta}. Nace pendiente de
     * despacho, y desde ese momento la farmacia la ve en su bandeja (HU-15).
     */
    @Transactional
    public RecetaResponse registrarReceta(Long visitaId, RecetaRequest request, Usuario medico) {
        VisitaMedica visita = activaDe(visitaRepository.bloquear(visitaId), visitaId, medico);
        if (recetaRepository.existsByVisitaId(visitaId)) {
            throw new ConflictoException("Esta visita ya tiene receta. Corríjala en lugar de registrar otra");
        }
        Receta receta = new Receta();
        receta.setVisita(visita);
        receta.setFechaEmision(LocalDateTime.now());
        llenar(receta, request);
        return MapeoReceta.receta(recetaRepository.save(receta));
    }

    /**
     * YS-108. Reemplaza los medicamentos de la receta por los que llegan.
     * Solo mientras la visita sigue activa y la farmacia no ha entregado nada:
     * después, cambiarla dejaría a la farmacia despachando algo distinto de lo
     * que se recetó.
     */
    @Transactional
    public RecetaResponse corregirReceta(Long visitaId, RecetaRequest request, Usuario medico) {
        activaDe(visitaRepository.bloquear(visitaId), visitaId, medico);
        Receta receta = recetaRepository.findByVisitaId(visitaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Esta visita todavía no tiene receta"));
        if (receta.getEstado() != EstadoReceta.PENDIENTE_DESPACHO) {
            throw new ConflictoException("La farmacia ya empezó a despachar esta receta y no se puede cambiar");
        }
        receta.getMedicamentos().clear();
        llenar(receta, request);
        return MapeoReceta.receta(recetaRepository.save(receta));
    }

    /** YS-108. La receta de una visita del especialista, activa o finalizada. */
    @Transactional(readOnly = true)
    public RecetaResponse receta(Long visitaId, Usuario medico) {
        VisitaMedica visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una visita con id " + visitaId));
        if (!visita.getEspecialista().getUsuario().getId().equals(medico.getId())) {
            throw new OperacionNoPermitidaException("Esta visita es de otro especialista");
        }
        return recetaRepository.findByVisitaId(visitaId)
                .map(MapeoReceta::receta)
                .orElseThrow(() -> new RecursoNoEncontradoException("Esta visita todavía no tiene receta"));
    }

    /**
     * YS-111. El catálogo de exámenes que el especialista puede ordenar, en
     * orden alfabético para encontrarlos rápido. Se ordena aquí y no en la
     * consulta: PostgreSQL ordena según la intercalación de la base, y con la
     * de desarrollo «Ácido úrico» quedaba después de «Vitamina B12».
     */
    @Transactional(readOnly = true)
    public List<ExamenLaboratorioResponse> catalogoExamenes() {
        Collator espanol = Collator.getInstance(Locale.forLanguageTag("es"));
        return examenRepository.findByActivoTrue().stream()
                .sorted(Comparator.comparing(ExamenLaboratorio::getNombre, espanol))
                .map(examen -> new ExamenLaboratorioResponse(examen.getId(), examen.getNombre()))
                .toList();
    }

    /**
     * YS-111. Ordena uno o más exámenes en la visita activa (HU-11, RF-08):
     * una orden pendiente por examen, que el laboratorio ve en su bandeja
     * (HU-12). Cada orden copia el costo de referencia del catálogo: es el
     * costo para el asilo que pide RF-08.
     *
     * <p>
     * Un examen que no está en el catálogo, o que se repite en la misma
     * petición, es un error del campo (400, en su fila). Uno que ya se ordenó
     * en esta visita es un conflicto con lo existente (409): ordenarlo otra
     * vez sacaría la muestra dos veces.
     */
    @Transactional
    public List<OrdenLaboratorioResponse> ordenarExamenes(Long visitaId, OrdenesLaboratorioRequest request,
            Usuario medico) {
        VisitaMedica visita = activaDe(visitaRepository.bloquear(visitaId), visitaId, medico);

        Set<Long> vistos = new HashSet<>();
        List<ExamenLaboratorio> examenes = new ArrayList<>();
        for (int i = 0; i < request.examenes().size(); i++) {
            Long examenId = request.examenes().get(i).examenId();
            String campo = "examenes[%d].examenId".formatted(i);
            ExamenLaboratorio examen = examenRepository.findById(examenId)
                    .filter(ExamenLaboratorio::isActivo)
                    .orElseThrow(() -> new CampoInvalidoException(campo, "El examen seleccionado no está en el catálogo"));
            if (!vistos.add(examenId)) {
                throw new CampoInvalidoException(campo, "Ese examen ya está en la lista");
            }
            if (ordenRepository.existsByVisitaIdAndExamenIdAndEstadoNot(visitaId, examenId, EstadoOrden.ANULADA)) {
                throw new ConflictoException("El examen «" + examen.getNombre() + "» ya se ordenó en esta visita");
            }
            examenes.add(examen);
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<OrdenLaboratorio> ordenes = new ArrayList<>();
        for (int i = 0; i < examenes.size(); i++) {
            OrdenLaboratorio orden = new OrdenLaboratorio();
            orden.setVisita(visita);
            orden.setExamen(examenes.get(i));
            orden.setIndicaciones(opcional(request.examenes().get(i).indicaciones()));
            orden.setCosto(examenes.get(i).getCostoReferencia());
            orden.setFechaOrden(ahora);
            ordenes.add(orden);
        }
        return ordenRepository.saveAll(ordenes).stream().map(VisitaMedicaService::orden).toList();
    }

    /** YS-111. Las órdenes de una visita del especialista, activa o finalizada (criterio de HU-11). */
    @Transactional(readOnly = true)
    public List<OrdenLaboratorioResponse> ordenes(Long visitaId, Usuario medico) {
        VisitaMedica visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una visita con id " + visitaId));
        if (!visita.getEspecialista().getUsuario().getId().equals(medico.getId())) {
            throw new OperacionNoPermitidaException("Esta visita es de otro especialista");
        }
        return ordenRepository.findByVisitaIdOrderByIdAsc(visitaId).stream().map(VisitaMedicaService::orden).toList();
    }

    // ---- apoyo ----

    /** YS-114. La comparten el especialista (HU-11) y la bandeja del laboratorio (HU-12). */
    static OrdenLaboratorioResponse orden(OrdenLaboratorio orden) {
        VisitaMedica visita = orden.getVisita();
        Usuario medico = visita.getEspecialista().getUsuario();
        return new OrdenLaboratorioResponse(
                orden.getId(),
                visita.getId(),
                new ResumenPersona(visita.getInterno().getId(), visita.getInterno().getNombreCompleto()),
                new ResumenPersona(medico.getId(), medico.getNombreCompleto()),
                new ExamenLaboratorioResponse(orden.getExamen().getId(), orden.getExamen().getNombre()),
                orden.getIndicaciones(),
                orden.getEstado(),
                orden.getFechaOrden(),
                orden.getResultado(),
                orden.getFechaResultado(),
                archivoDe(orden));
    }

    /** YS-117. El archivo del resultado, sin su contenido; {@code null} si no tiene. */
    static ArchivoResultadoResponse archivoDe(OrdenLaboratorio orden) {
        return orden.getRutaArchivo() == null ? null
                : new ArchivoResultadoResponse(orden.getNombreArchivo(), orden.getTipoArchivo().tipoMime(),
                        orden.getTamanoArchivo());
    }

    private static void llenar(Receta receta, RecetaRequest request) {
        receta.setIndicacionesGenerales(opcional(request.indicacionesGenerales()));
        for (MedicamentoRecetaRequest dto : request.medicamentos()) {
            DetalleReceta detalle = new DetalleReceta();
            detalle.setMedicamento(dto.medicamento().trim());
            detalle.setDosis(dto.dosis().trim());
            detalle.setCantidad(dto.cantidad());
            detalle.setFormaAplicacion(dto.formaAplicacion().trim());
            receta.agregarMedicamento(detalle);
        }
    }

    /** La visita, si es de este especialista y sigue activa: para todo lo que la modifica. */
    private VisitaMedica activaDe(Optional<VisitaMedica> encontrada, Long visitaId, Usuario medico) {
        VisitaMedica visita = encontrada
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una visita con id " + visitaId));
        if (!visita.getEspecialista().getUsuario().getId().equals(medico.getId())) {
            throw new OperacionNoPermitidaException("Esta visita es de otro especialista");
        }
        if (visita.getEstado() != EstadoVisita.ACTIVA) {
            throw new ConflictoException("La visita ya está finalizada y no admite cambios");
        }
        return visita;
    }

    /** Un texto opcional vacío se guarda como null: "" no es "sin observaciones". */
    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private VisitaMedicaResponse mapear(VisitaMedica visita) {
        Cita cita = visita.getCita();
        SolicitudReferencia solicitud = cita.getSolicitud();
        Interno interno = visita.getInterno();
        return new VisitaMedicaResponse(
                visita.getId(),
                visita.getEstado(),
                visita.getFechaInicio(),
                visita.getFechaCierre(),
                visita.getDiagnostico(),
                visita.getObservaciones(),
                cita.getId(),
                cita.getFechaHora(),
                solicitud.getId(),
                solicitud.getNumero(),
                solicitud.getEstado(),
                solicitud.getMotivo(),
                new VisitaMedicaResponse.Paciente(interno.getId(), interno.getNombreCompleto(),
                        interno.getFechaNacimiento()),
                especialidad(solicitud.getEspecialidad()),
                persona(visita.getEspecialista().getUsuario()),
                persona(solicitud.getMedicoGeneral()),
                persona(solicitud.getEnfermeroAcompanante()));
    }

    private static ResumenPersona persona(Usuario usuario) {
        return new ResumenPersona(usuario.getId(), usuario.getNombreCompleto());
    }

    private static EspecialidadResponse especialidad(Especialidad especialidad) {
        return new EspecialidadResponse(especialidad.getId(), especialidad.getNombre());
    }
}
