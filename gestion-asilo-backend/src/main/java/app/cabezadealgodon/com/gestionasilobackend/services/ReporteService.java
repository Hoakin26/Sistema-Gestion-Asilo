package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteAnalisisMedicoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteAnalisisMedicoResponse.Paciente;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCostosResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCostosResponse.CitaCostos;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCostosResponse.Costos;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCostosResponse.PacienteCostos;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteExamenesResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteExamenesResponse.ExamenRealizado;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteMedicamentosResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteMedicamentosResponse.MedicamentoEntregado;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.OperacionNoPermitidaException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.CargoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.DetalleReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;
import app.cabezadealgodon.com.gestionasilobackend.models.Interno;
import app.cabezadealgodon.com.gestionasilobackend.models.OrdenLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.models.Receta;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.DetalleRecetaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.InternoRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.OrdenLaboratorioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.VisitaMedicaRepository;

/**
 * YS-151. Los reportes (HU-24 a HU-28, RF-17 a RF-21b), el {@code ReporteService}
 * del diseño. Arranca con el de costos por paciente y cita (HU-24); HU-25 le
 * suma el de análisis médico, y HU-28 los de exámenes y medicamentos por
 * paciente. Los de cobros (HU-26) y los financieros (HU-27) van en sus propios
 * servicios.
 */
@Service
public class ReporteService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final CargoServicioRepository cargoRepository;
    private final InternoRepository internoRepository;
    private final VisitaMedicaRepository visitaRepository;
    private final InternoService internoService;
    private final HistorialMedicoService historialService;
    private final OrdenLaboratorioRepository ordenRepository;
    private final DetalleRecetaRepository detalleRepository;

    public ReporteService(CargoServicioRepository cargoRepository, InternoRepository internoRepository,
            VisitaMedicaRepository visitaRepository, InternoService internoService,
            HistorialMedicoService historialService, OrdenLaboratorioRepository ordenRepository,
            DetalleRecetaRepository detalleRepository) {
        this.cargoRepository = cargoRepository;
        this.internoRepository = internoRepository;
        this.visitaRepository = visitaRepository;
        this.internoService = internoService;
        this.historialService = historialService;
        this.ordenRepository = ordenRepository;
        this.detalleRepository = detalleRepository;
    }

    /**
     * HU-25 (YS-154). El análisis médico de un paciente: sus datos, su ficha y
     * todo su historial. El Médico General lo genera de cualquier paciente,
     * como su historial centralizado (HU-02). Decidido el 21/10: el
     * especialista, solo del paciente de una visita suya que siga activa, como
     * los antecedentes de HU-08; si no, 403. Un interno que no existe, 404.
     */
    @Transactional(readOnly = true)
    public ReporteAnalisisMedicoResponse analisisMedico(Long internoId, Usuario medico) {
        Interno interno = pacienteClinico(internoId, medico);
        return new ReporteAnalisisMedicoResponse(paciente(interno), internoService.obtenerFicha(internoId),
                historialService.de(internoId, null));
    }

    /**
     * HU-28 (YS-163). Los exámenes que el laboratorio le realizó a un paciente
     * (RF-21a) entre {@code desde} y {@code hasta}, las dos incluidas, por la
     * fecha del resultado, del más antiguo al más reciente: el examen, la
     * fecha y el resultado, y quién lo pidió. Los permisos del análisis
     * médico (HU-25).
     */
    @Transactional(readOnly = true)
    public ReporteExamenesResponse examenes(Long internoId, LocalDate desde, LocalDate hasta, Usuario medico) {
        Interno interno = pacienteClinico(internoId, medico);
        validarRango(desde, hasta);
        List<ExamenRealizado> examenes = ordenRepository.delInternoConResultadoEntre(internoId,
                EstadoOrden.RESULTADOS_CARGADOS, inicio(desde), finExclusivo(hasta)).stream()
                .map(ReporteService::examenRealizado)
                .toList();
        return new ReporteExamenesResponse(paciente(interno), desde, hasta, examenes);
    }

    /**
     * HU-28 (YS-163). Los medicamentos que la farmacia le entregó a un
     * paciente (RF-21b) entre {@code desde} y {@code hasta}, las dos
     * incluidas, por la fecha de entrega: el medicamento, la dosis, la
     * cantidad y la fecha, y de qué receta salió. Los permisos del análisis
     * médico (HU-25).
     */
    @Transactional(readOnly = true)
    public ReporteMedicamentosResponse medicamentos(Long internoId, LocalDate desde, LocalDate hasta,
            Usuario medico) {
        Interno interno = pacienteClinico(internoId, medico);
        validarRango(desde, hasta);
        List<MedicamentoEntregado> medicamentos = detalleRepository
                .entregadosAlInternoEntre(internoId, inicio(desde), finExclusivo(hasta)).stream()
                .map(ReporteService::medicamentoEntregado)
                .toList();
        return new ReporteMedicamentosResponse(paciente(interno), desde, hasta, medicamentos);
    }

    /**
     * El paciente de un reporte clínico (HU-25, HU-28). El Médico General, de
     * cualquiera, como su historial centralizado (HU-02). Decidido el 21/10:
     * el especialista, solo del paciente de una visita suya que siga activa,
     * como los antecedentes de HU-08; si no, 403. Un interno que no existe, 404.
     */
    private Interno pacienteClinico(Long internoId, Usuario medico) {
        if (medico.getRol().getNombre() == RolNombre.MEDICO_ESPECIALISTA
                && visitaRepository.delEspecialistaEnEstado(medico.getId(), EstadoVisita.ACTIVA).stream()
                        .noneMatch(visita -> visita.getInterno().getId().equals(internoId))) {
            throw new OperacionNoPermitidaException(
                    "Solo puede generar el reporte del paciente que está atendiendo, mientras su visita siga activa");
        }
        return internoRepository.findById(internoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un interno con id " + internoId));
    }

    private static Paciente paciente(Interno interno) {
        return new Paciente(interno.getId(), interno.getNombreCompleto(), interno.getDpi(),
                interno.getFechaNacimiento(), interno.getFechaIngreso(), interno.isActivo());
    }

    private static ExamenRealizado examenRealizado(OrdenLaboratorio orden) {
        VisitaMedica visita = orden.getVisita();
        return new ExamenRealizado(orden.getId(), orden.getExamen().getNombre(), orden.getFechaOrden(),
                orden.getFechaResultado(), orden.getResultado(), VisitaMedicaService.archivoDe(orden),
                HistorialMedicoService.especialidad(visita.getCita().getSolicitud().getEspecialidad()),
                HistorialMedicoService.persona(visita.getEspecialista().getUsuario()));
    }

    private static MedicamentoEntregado medicamentoEntregado(DetalleReceta detalle) {
        Receta receta = detalle.getReceta();
        VisitaMedica visita = receta.getVisita();
        return new MedicamentoEntregado(detalle.getId(), detalle.getMedicamento(), detalle.getDosis(),
                detalle.getCantidad(), detalle.getFormaAplicacion(), detalle.getFechaEntrega(),
                receta.getFechaEmision(),
                HistorialMedicoService.especialidad(visita.getCita().getSolicitud().getEspecialidad()),
                HistorialMedicoService.persona(visita.getEspecialista().getUsuario()));
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new CampoInvalidoException("hasta", "La fecha final no puede ser anterior a la inicial");
        }
    }

    /** El primer instante de {@code dia}. */
    private static LocalDateTime inicio(LocalDate dia) {
        return dia.atStartOfDay();
    }

    /** El primer instante después de {@code dia}: el límite exclusivo de un rango que lo incluye. */
    private static LocalDateTime finExclusivo(LocalDate dia) {
        return dia.plusDays(1).atStartOfDay();
    }

    /**
     * HU-24. Los costos de cada cita entre {@code desde} y {@code hasta}, las
     * dos incluidas, por la fecha de la cita, como el listado de pendientes
     * (HU-21). Con {@code internoId}, solo de ese paciente; sin él, de todos,
     * por nombre. Lee los mismos cargos que la cuenta del familiar, así que el
     * costo de cada servicio es el que se registró al darlo.
     */
    @Transactional(readOnly = true)
    public ReporteCostosResponse costosPorCita(Long internoId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        ResumenPersona filtro = null;
        if (internoId != null) {
            Interno interno = internoRepository.findById(internoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe un interno con id " + internoId));
            filtro = new ResumenPersona(interno.getId(), interno.getNombreCompleto());
        }

        // Todos los pacientes: el patrón vacío trae a todos, y el filtro por id va aquí.
        List<CargoServicio> cargos = cargoRepository.deCitasEntre(inicio(desde), finExclusivo(hasta),
                PatronBusqueda.contiene(""));
        Map<Long, List<CargoServicio>> porVisita = new LinkedHashMap<>();
        for (CargoServicio cargo : cargos) {
            if (internoId == null || internoId.equals(cargo.getInterno().getId())) {
                porVisita.computeIfAbsent(cargo.getVisita().getId(), id -> new ArrayList<>()).add(cargo);
            }
        }

        Map<Long, List<CitaCostos>> citasPorPaciente = new LinkedHashMap<>();
        Map<Long, ResumenPersona> pacientes = new LinkedHashMap<>();
        for (List<CargoServicio> deLaVisita : porVisita.values()) {
            VisitaMedica visita = deLaVisita.get(0).getVisita();
            Interno interno = deLaVisita.get(0).getInterno();
            pacientes.putIfAbsent(interno.getId(), new ResumenPersona(interno.getId(), interno.getNombreCompleto()));
            citasPorPaciente.computeIfAbsent(interno.getId(), id -> new ArrayList<>()).add(new CitaCostos(
                    visita.getId(),
                    visita.getCita().getSolicitud().getNumero(),
                    visita.getCita().getFechaHora().toLocalDate(),
                    costos(deLaVisita)));
        }

        List<PacienteCostos> porPaciente = pacientes.values().stream()
                .sorted(Comparator.comparing(ResumenPersona::nombreCompleto).thenComparing(ResumenPersona::id))
                .map(paciente -> {
                    List<CitaCostos> citas = citasPorPaciente.get(paciente.id());
                    Costos totales = sumar(citas.stream().map(CitaCostos::costos).toList());
                    return new PacienteCostos(paciente, citas, totales, promedio(totales.total(), citas.size()));
                })
                .toList();
        int cantidad = porVisita.size();
        Costos totales = sumar(porPaciente.stream().map(PacienteCostos::totales).toList());
        return new ReporteCostosResponse(desde, hasta, filtro, porPaciente, cantidad, totales,
                promedio(totales.total(), cantidad));
    }

    /** Lo que costaron los cargos de una cita, por tipo de servicio. */
    private static Costos costos(List<CargoServicio> cargos) {
        BigDecimal consulta = CERO;
        BigDecimal examenes = CERO;
        BigDecimal medicamentos = CERO;
        int cantidadExamenes = 0;
        int cantidadMedicamentos = 0;
        BigDecimal descuento = CERO;
        BigDecimal neto = CERO;
        for (CargoServicio cargo : cargos) {
            if (cargo.getTipo() == TipoServicio.CONSULTA) {
                consulta = consulta.add(cargo.getMontoOriginal());
            } else if (cargo.getTipo() == TipoServicio.LABORATORIO) {
                examenes = examenes.add(cargo.getMontoOriginal());
                cantidadExamenes++;
            } else {
                medicamentos = medicamentos.add(cargo.getMontoOriginal());
                cantidadMedicamentos++;
            }
            descuento = descuento.add(cargo.getDescuento());
            neto = neto.add(cargo.getMontoNeto());
        }
        return new Costos(consulta.setScale(2), cantidadExamenes, examenes.setScale(2), cantidadMedicamentos,
                medicamentos.setScale(2), consulta.add(examenes).add(medicamentos).setScale(2), descuento.setScale(2),
                neto.setScale(2));
    }

    private static Costos sumar(List<Costos> lista) {
        Costos total = new Costos(CERO, 0, CERO, 0, CERO, CERO, CERO, CERO);
        for (Costos costos : lista) {
            total = new Costos(
                    total.consulta().add(costos.consulta()),
                    total.examenes() + costos.examenes(),
                    total.costoExamenes().add(costos.costoExamenes()),
                    total.medicamentos() + costos.medicamentos(),
                    total.costoMedicamentos().add(costos.costoMedicamentos()),
                    total.total().add(costos.total()),
                    total.descuento().add(costos.descuento()),
                    total.neto().add(costos.neto()));
        }
        return total;
    }

    /** El costo promedio por cita, al centavo; cero sin citas. */
    private static BigDecimal promedio(BigDecimal total, int citas) {
        return citas == 0 ? CERO : total.divide(BigDecimal.valueOf(citas), 2, RoundingMode.HALF_UP);
    }
}
