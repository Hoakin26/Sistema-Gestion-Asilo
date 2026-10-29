package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCobrosResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCobrosResponse.CuentaCobrada;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCobrosResponse.GastoMedico;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCobrosResponse.PacienteCobros;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCobrosResponse.PagoCobrado;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCobrosResponse.TotalPorForma;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.CuentaMensual;
import app.cabezadealgodon.com.gestionasilobackend.models.FormaPago;
import app.cabezadealgodon.com.gestionasilobackend.models.Interno;
import app.cabezadealgodon.com.gestionasilobackend.models.Pago;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository.TotalDeCuenta;
import app.cabezadealgodon.com.gestionasilobackend.repository.InternoRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.PagoRepository;

/**
 * YS-157. El reporte de cobros a familiares (HU-26, RF-19). Va aparte de
 * {@link ReporteService} porque sale de otro lado: de los pagos del familiar
 * (HU-34) y de las cuentas mensuales a las que se aplicaron (HU-18), no de los
 * cargos sueltos.
 */
@Service
public class ReporteCobrosService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final PagoRepository pagoRepository;
    private final CargoServicioRepository cargoRepository;
    private final InternoRepository internoRepository;

    public ReporteCobrosService(PagoRepository pagoRepository, CargoServicioRepository cargoRepository,
            InternoRepository internoRepository) {
        this.pagoRepository = pagoRepository;
        this.cargoRepository = cargoRepository;
        this.internoRepository = internoRepository;
    }

    /**
     * HU-26. Los pagos recibidos entre {@code desde} y {@code hasta}, las dos
     * incluidas (decidido el 22/10: el rango es de los pagos), agrupados por
     * paciente y por nombre, con las cuentas que cubren y sus gastos médicos.
     * Con {@code internoId}, solo los de ese paciente. Los anulados no se
     * cobraron: no aparecen ni suman.
     */
    @Transactional(readOnly = true)
    public ReporteCobrosResponse cobros(Long internoId, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new CampoInvalidoException("hasta", "La fecha final no puede ser anterior a la inicial");
        }
        ResumenPersona filtro = null;
        if (internoId != null) {
            Interno interno = internoRepository.findById(internoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe un interno con id " + internoId));
            filtro = new ResumenPersona(interno.getId(), interno.getNombreCompleto());
        }

        List<Pago> pagos = pagoRepository.cobradosEntre(desde, hasta).stream()
                .filter(pago -> internoId == null || internoId.equals(pago.getCuenta().getInterno().getId()))
                .toList();

        // Las cuentas que cubren esos pagos, cada una con sus cargos por tipo.
        Map<Long, CuentaMensual> cuentas = new LinkedHashMap<>();
        pagos.forEach(pago -> cuentas.putIfAbsent(pago.getCuenta().getId(), pago.getCuenta()));
        // DEF-02. Los gastos médicos de esas cuentas, ya sumados por tipo en una sola
        // consulta. En el orden de TipoServicio, como antes.
        Map<Long, List<GastoMedico>> gastosPorCuenta = new LinkedHashMap<>();
        if (!cuentas.isEmpty()) {
            cargoRepository.totalesPorCuenta(cuentas.keySet()).stream()
                    .sorted(Comparator.comparing(TotalDeCuenta::getTipo))
                    .forEach(total -> gastosPorCuenta
                            .computeIfAbsent(total.getCuentaId(), id -> new ArrayList<>())
                            .add(new GastoMedico(total.getTipo(), Math.toIntExact(total.getCantidad()),
                                    total.getOriginal().setScale(2), total.getDescuento().setScale(2),
                                    total.getNeto().setScale(2))));
        }

        Map<Long, List<Pago>> pagosPorPaciente = new LinkedHashMap<>();
        Map<Long, ResumenPersona> pacientes = new LinkedHashMap<>();
        for (Pago pago : pagos) {
            Interno interno = pago.getCuenta().getInterno();
            pacientes.putIfAbsent(interno.getId(), new ResumenPersona(interno.getId(), interno.getNombreCompleto()));
            pagosPorPaciente.computeIfAbsent(interno.getId(), id -> new ArrayList<>()).add(pago);
        }

        LocalDate hoy = LocalDate.now();
        List<PacienteCobros> porPaciente = pacientes.values().stream()
                .sorted(Comparator.comparing(ResumenPersona::nombreCompleto).thenComparing(ResumenPersona::id))
                .map(paciente -> {
                    List<Pago> suyos = pagosPorPaciente.get(paciente.id());
                    Map<Long, List<Pago>> porCuenta = new LinkedHashMap<>();
                    suyos.forEach(pago -> porCuenta.computeIfAbsent(pago.getCuenta().getId(), id -> new ArrayList<>())
                            .add(pago));
                    List<CuentaCobrada> cobradas = porCuenta.entrySet().stream()
                            .map(grupo -> cuenta(cuentas.get(grupo.getKey()),
                                    gastosPorCuenta.getOrDefault(grupo.getKey(), List.of()), suma(grupo.getValue()), hoy))
                            .sorted(Comparator.comparing(CuentaCobrada::periodo))
                            .toList();
                    return new PacienteCobros(paciente, suyos.stream().map(ReporteCobrosService::pago).toList(),
                            cobradas, suma(suyos));
                })
                .toList();

        return new ReporteCobrosResponse(desde, hasta, filtro, porPaciente, pagos.size(), suma(pagos),
                porForma(pagos));
    }

    private static PagoCobrado pago(Pago pago) {
        return new PagoCobrado(pago.getId(), pago.getFecha(), pago.getCuenta().getPeriodo(), pago.getFormaPago(),
                pago.getReferencia(), pago.getMonto());
    }

    /** Una cuenta con sus gastos médicos por tipo, lo cobrado de ella en el período y su estado de hoy. */
    private static CuentaCobrada cuenta(CuentaMensual cuenta, List<GastoMedico> gastos, BigDecimal cobrado,
            LocalDate hoy) {
        return new CuentaCobrada(cuenta.getId(), cuenta.getPeriodo(), cuenta.getFamiliar().getNombreCompleto(),
                cuenta.getCuotaBase(), gastos, cuenta.getTotalDescuentos(), cuenta.getTotalAPagar(), cobrado,
                cuenta.getTotalPagado(), cuenta.getSaldo(), cuenta.estadoAl(hoy));
    }

    /** Lo cobrado por forma de pago, en el orden de {@link FormaPago}; las que no se usaron, en cero. */
    private static List<TotalPorForma> porForma(List<Pago> pagos) {
        return Arrays.stream(FormaPago.values())
                .map(forma -> {
                    List<Pago> conEsta = pagos.stream().filter(pago -> pago.getFormaPago() == forma).toList();
                    return new TotalPorForma(forma, conEsta.size(), suma(conEsta));
                })
                .toList();
    }

    private static BigDecimal suma(List<Pago> pagos) {
        return sumar(pagos, Pago::getMonto);
    }

    private static <T> BigDecimal sumar(List<T> elementos, Function<T, BigDecimal> monto) {
        return elementos.stream().map(monto).reduce(CERO, BigDecimal::add).setScale(2);
    }
}
