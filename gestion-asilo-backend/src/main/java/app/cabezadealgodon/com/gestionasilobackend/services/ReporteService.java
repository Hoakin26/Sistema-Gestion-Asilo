package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCostosResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCostosResponse.CitaCostos;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCostosResponse.Costos;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCostosResponse.PacienteCostos;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.CargoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.Interno;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.InternoRepository;

/**
 * YS-151. Los reportes (HU-24 a HU-28, RF-17 a RF-21b), el {@code ReporteService}
 * del diseño. Arranca con el de costos por paciente y cita (HU-24); los demás
 * se le irán sumando.
 */
@Service
public class ReporteService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final CargoServicioRepository cargoRepository;
    private final InternoRepository internoRepository;

    public ReporteService(CargoServicioRepository cargoRepository, InternoRepository internoRepository) {
        this.cargoRepository = cargoRepository;
        this.internoRepository = internoRepository;
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
        if (hasta.isBefore(desde)) {
            throw new CampoInvalidoException("hasta", "La fecha final no puede ser anterior a la inicial");
        }
        ResumenPersona filtro = null;
        if (internoId != null) {
            Interno interno = internoRepository.findById(internoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe un interno con id " + internoId));
            filtro = new ResumenPersona(interno.getId(), interno.getNombreCompleto());
        }

        // Todos los pacientes: el patrón vacío trae a todos, y el filtro por id va aquí.
        List<CargoServicio> cargos = cargoRepository.deCitasEntre(desde.atStartOfDay(),
                hasta.plusDays(1).atStartOfDay(), PatronBusqueda.contiene(""));
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
