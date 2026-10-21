package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.CitaPendienteResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.dto.ServiciosPendientesResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.CargoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.CuentaMensual;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoCuenta;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoPagoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository;

/**
 * YS-142. Las citas que el familiar todavía no termina de pagar (HU-21,
 * RF-14): cada visita médica con lo que generó (la consulta, sus exámenes y sus
 * medicamentos), lo pagado, el saldo y si va sin pago o con pago parcial.
 *
 * <p>
 * <b>Cómo se reparte lo pagado.</b> El familiar paga la cuenta del mes entera,
 * no cita por cita (HU-34). Decidido el 21/10: lo pagado se reparte en
 * proporción. Si la cuenta lleva pagado el 40 %, cada cargo suyo lleva pagado
 * el 40 % de su neto, y todas sus citas quedan en Pago parcial hasta cubrir el
 * total, como dice RF-22: "los servicios quedan pendientes hasta cubrir el
 * total". Una cita sale del listado cuando sus cuentas quedan pagadas
 * completas.
 *
 * <p>
 * También decidido el 21/10: entran las citas de un mes que no se ha
 * consolidado, sin pago, porque tampoco se han pagado. Se marcan, para que se
 * sepa que todavía no están en la cuenta del familiar.
 */
@Service
public class ServicioPendienteService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);
    /** Sin fechas, todas: desde una fecha anterior a cualquier cita hasta una posterior. */
    private static final LocalDateTime SIN_INICIO = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime SIN_FIN = LocalDateTime.of(3000, 1, 1, 0, 0);

    private final CargoServicioRepository cargoRepository;

    public ServicioPendienteService(CargoServicioRepository cargoRepository) {
        this.cargoRepository = cargoRepository;
    }

    /**
     * Las citas pendientes del paciente que coincide por nombre o DPI, cuya
     * fecha cae entre {@code desde} y {@code hasta}, las dos incluidas. Sin
     * paciente, de todos; sin fechas, de cualquier fecha. De la más antigua a
     * la más reciente: las deudas más viejas primero.
     */
    @Transactional(readOnly = true)
    public ServiciosPendientesResponse pendientes(String paciente, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new CampoInvalidoException("hasta", "La fecha final no puede ser anterior a la inicial");
        }
        String patron = PatronBusqueda.contiene(PatronBusqueda.normalizado(paciente));
        List<CargoServicio> cargos = cargoRepository.deCitasEntre(
                desde == null ? SIN_INICIO : desde.atStartOfDay(),
                hasta == null ? SIN_FIN : hasta.plusDays(1).atStartOfDay(),
                patron);

        Map<Long, List<CargoServicio>> porVisita = new LinkedHashMap<>();
        cargos.forEach(cargo -> porVisita.computeIfAbsent(cargo.getVisita().getId(), id -> new ArrayList<>()).add(cargo));

        LocalDate hoy = LocalDate.now();
        List<CitaPendienteResponse> citas = new ArrayList<>();
        for (List<CargoServicio> deLaVisita : porVisita.values()) {
            CitaPendienteResponse cita = pendiente(deLaVisita, hoy);
            if (cita != null) {
                citas.add(cita);
            }
        }
        return new ServiciosPendientesResponse(citas,
                suma(citas.stream().map(CitaPendienteResponse::montoTotal).toList()),
                suma(citas.stream().map(CitaPendienteResponse::montoPagado).toList()),
                suma(citas.stream().map(CitaPendienteResponse::saldo).toList()));
    }

    /** Una visita como fila del listado; {@code null} si ya está pagada completa. */
    private static CitaPendienteResponse pendiente(List<CargoServicio> cargos, LocalDate hoy) {
        VisitaMedica visita = cargos.get(0).getVisita();

        // Lo pagado, por cuenta: un examen cargado al mes siguiente cae en otra cuenta.
        Map<CuentaMensual, List<CargoServicio>> porCuenta = new LinkedHashMap<>();
        List<CargoServicio> sinCuenta = new ArrayList<>();
        for (CargoServicio cargo : cargos) {
            if (cargo.getCuenta() == null) {
                sinCuenta.add(cargo);
            } else {
                porCuenta.computeIfAbsent(cargo.getCuenta(), cuenta -> new ArrayList<>()).add(cargo);
            }
        }
        BigDecimal pagado = CERO;
        boolean vencida = false;
        for (Map.Entry<CuentaMensual, List<CargoServicio>> grupo : porCuenta.entrySet()) {
            pagado = pagado.add(pagadoDe(grupo.getKey(), neto(grupo.getValue())));
            vencida |= grupo.getKey().estadoAl(hoy) == EstadoCuenta.EN_MORA;
        }

        BigDecimal total = neto(cargos);
        BigDecimal saldo = total.subtract(pagado).max(BigDecimal.ZERO).setScale(2);
        if (saldo.signum() == 0) {
            return null;
        }
        return new CitaPendienteResponse(
                visita.getId(),
                visita.getCita().getSolicitud().getNumero(),
                visita.getCita().getFechaHora().toLocalDate(),
                new ResumenPersona(visita.getInterno().getId(), visita.getInterno().getNombreCompleto()),
                servicios(cargos),
                total,
                pagado,
                saldo,
                pagado.signum() == 0 ? EstadoPagoServicio.SIN_PAGO : EstadoPagoServicio.PAGO_PARCIAL,
                !sinCuenta.isEmpty(),
                vencida);
    }

    /**
     * Lo que le toca a {@code neto} de lo pagado en su cuenta: la misma
     * proporción que la cuenta lleva pagada. Una cuenta pagada completa cubre
     * todo; una sin pagos, nada.
     */
    static BigDecimal pagadoDe(CuentaMensual cuenta, BigDecimal neto) {
        if (cuenta.getSaldo().signum() == 0) {
            return neto;
        }
        if (cuenta.getTotalPagado().signum() == 0 || cuenta.getTotalAPagar().signum() == 0) {
            return CERO;
        }
        return neto.multiply(cuenta.getTotalPagado())
                .divide(cuenta.getTotalAPagar(), 2, RoundingMode.HALF_UP)
                .min(neto);
    }

    /** Cuántos de cada tipo de servicio y cuánto suman, en el orden de {@link TipoServicio}. */
    private static List<CitaPendienteResponse.Servicio> servicios(List<CargoServicio> cargos) {
        Map<TipoServicio, List<CargoServicio>> porTipo = new EnumMap<>(TipoServicio.class);
        cargos.forEach(cargo -> porTipo.computeIfAbsent(cargo.getTipo(), tipo -> new ArrayList<>()).add(cargo));
        return porTipo.entrySet().stream()
                .map(grupo -> new CitaPendienteResponse.Servicio(grupo.getKey(), grupo.getValue().size(),
                        neto(grupo.getValue())))
                .toList();
    }

    private static BigDecimal neto(List<CargoServicio> cargos) {
        return suma(cargos.stream().map(CargoServicio::getMontoNeto).toList());
    }

    private static BigDecimal suma(List<BigDecimal> montos) {
        return montos.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }
}
